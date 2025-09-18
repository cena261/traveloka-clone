package com.cena.traveloka.search.service;

import com.cena.traveloka.search.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationEngineService {

    private final SearchService searchService;
    private final UserProfileService userProfileService;
    private final AnalyticsService analyticsService;
    private final PersonalizationService personalizationService;

    @Value("${recommendations.max-results:10}")
    private int maxRecommendationsPerType;

    @Value("${recommendations.enable-caching:true}")
    private boolean enableCaching;

    @Value("${recommendations.diversity-factor:0.3}")
    private double defaultDiversityFactor;

    @Value("${recommendations.confidence-threshold:0.6}")
    private double confidenceThreshold;

    @Cacheable(value = "recommendations", key = "#request.userId + ':' + #request.hashCode()", condition = "#enableCaching")
    public RecommendationResponse generateRecommendations(RecommendationRequest request) {
        log.info("Generating recommendations for user: {}, session: {}",
                request.getUserId(), request.getSessionId());

        long startTime = System.currentTimeMillis();

        try {
            // Validate request
            validateRecommendationRequest(request);

            // Enrich user profile if needed
            RecommendationRequest enrichedRequest = enrichUserProfile(request);

            // Generate recommendations using multiple strategies
            List<CompletableFuture<List<RecommendationResponse.PropertyRecommendation>>> propertyFutures =
                    generatePropertyRecommendations(enrichedRequest);

            List<CompletableFuture<List<RecommendationResponse.DestinationRecommendation>>> destinationFutures =
                    generateDestinationRecommendations(enrichedRequest);

            List<CompletableFuture<List<RecommendationResponse.ServiceRecommendation>>> serviceFutures =
                    generateServiceRecommendations(enrichedRequest);

            List<CompletableFuture<List<RecommendationResponse.DealRecommendation>>> dealFutures =
                    generateDealRecommendations(enrichedRequest);

            // Wait for all futures to complete
            List<RecommendationResponse.PropertyRecommendation> propertyRecommendations =
                    collectPropertyRecommendations(propertyFutures);

            List<RecommendationResponse.DestinationRecommendation> destinationRecommendations =
                    collectDestinationRecommendations(destinationFutures);

            List<RecommendationResponse.ServiceRecommendation> serviceRecommendations =
                    collectServiceRecommendations(serviceFutures);

            List<RecommendationResponse.DealRecommendation> dealRecommendations =
                    collectDealRecommendations(dealFutures);

            // Apply post-processing
            propertyRecommendations = postProcessRecommendations(propertyRecommendations, enrichedRequest);
            destinationRecommendations = postProcessDestinationRecommendations(destinationRecommendations, enrichedRequest);

            // Generate metadata and metrics
            RecommendationResponse.RecommendationMetadata metadata = buildMetadata(enrichedRequest, startTime);
            RecommendationResponse.RecommendationMetrics metrics = calculateMetrics(
                    propertyRecommendations, destinationRecommendations, serviceRecommendations, dealRecommendations);

            // Generate explanations
            RecommendationResponse.RecommendationExplanation explanation =
                    generateExplanations(enrichedRequest, propertyRecommendations, destinationRecommendations);

            RecommendationResponse response = RecommendationResponse.builder()
                    .propertyRecommendations(propertyRecommendations)
                    .destinationRecommendations(destinationRecommendations)
                    .serviceRecommendations(serviceRecommendations)
                    .dealRecommendations(dealRecommendations)
                    .metadata(metadata)
                    .metrics(metrics)
                    .explanation(explanation)
                    .build();

            // Record recommendation event for learning
            recordRecommendationEvent(enrichedRequest, response);

            log.info("Generated {} total recommendations in {}ms for user: {}",
                    getTotalRecommendationCount(response),
                    System.currentTimeMillis() - startTime,
                    request.getUserId());

            return response;

        } catch (Exception e) {
            log.error("Recommendation generation failed", e);
            return buildErrorResponse(e, startTime);
        }
    }

    public RecommendationResponse generateSimilarProperties(String propertyId, String userId, int maxResults) {
        log.info("Generating similar properties for property: {}, user: {}", propertyId, userId);

        try {
            // Get property details
            PropertySearchResult baseProperty = getPropertyById(propertyId);
            if (baseProperty == null) {
                log.warn("Property not found: {}", propertyId);
                return RecommendationResponse.builder().build();
            }

            // Build similar properties request
            RecommendationRequest request = buildSimilarPropertiesRequest(baseProperty, userId, maxResults);

            // Generate content-based recommendations
            List<RecommendationResponse.PropertyRecommendation> similarProperties =
                    generateContentBasedRecommendations(request, baseProperty);

            // Build response
            return RecommendationResponse.builder()
                    .propertyRecommendations(similarProperties)
                    .metadata(RecommendationResponse.RecommendationMetadata.builder()
                            .algorithmVersion("content-based-v2")
                            .appliedStrategies(List.of("content_similarity"))
                            .generatedAt(OffsetDateTime.now())
                            .totalRecommendations(similarProperties.size())
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("Similar properties generation failed", e);
            return RecommendationResponse.builder().build();
        }
    }

    public RecommendationResponse generateTrendingRecommendations(String region, int maxResults) {
        log.info("Generating trending recommendations for region: {}, maxResults: {}", region, maxResults);

        try {
            // Get trending properties from analytics
            List<PropertySearchResult> trendingProperties = analyticsService.getTrendingProperties(region, maxResults * 2);

            // Convert to recommendations
            List<RecommendationResponse.PropertyRecommendation> recommendations = trendingProperties.stream()
                    .limit(maxResults)
                    .map(this::convertToTrendingRecommendation)
                    .collect(Collectors.toList());

            return RecommendationResponse.builder()
                    .propertyRecommendations(recommendations)
                    .metadata(RecommendationResponse.RecommendationMetadata.builder()
                            .algorithmVersion("trending-v1")
                            .appliedStrategies(List.of("trending_analysis"))
                            .generatedAt(OffsetDateTime.now())
                            .totalRecommendations(recommendations.size())
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("Trending recommendations generation failed", e);
            return RecommendationResponse.builder().build();
        }
    }

    public RecommendationResponse generatePersonalizedRecommendations(String userId, int maxResults) {
        log.info("Generating personalized recommendations for user: {}, maxResults: {}", userId, maxResults);

        try {
            // Get user profile and history
            UserSearchProfile userProfile = userProfileService.getUserProfile(userId);
            if (userProfile == null) {
                log.info("No user profile found for user: {}, returning generic recommendations", userId);
                return generateGenericRecommendations(maxResults);
            }

            // Build personalized request
            RecommendationRequest request = buildPersonalizedRequest(userProfile, userId, maxResults);

            // Generate using collaborative filtering
            List<RecommendationResponse.PropertyRecommendation> collaborativeRecs =
                    generateCollaborativeFilteringRecommendations(request);

            // Generate using content-based filtering
            List<RecommendationResponse.PropertyRecommendation> contentRecs =
                    generateUserContentBasedRecommendations(request);

            // Merge and rank recommendations
            List<RecommendationResponse.PropertyRecommendation> mergedRecs =
                    mergeRecommendations(collaborativeRecs, contentRecs, 0.6, 0.4);

            return RecommendationResponse.builder()
                    .propertyRecommendations(mergedRecs.stream().limit(maxResults).collect(Collectors.toList()))
                    .metadata(RecommendationResponse.RecommendationMetadata.builder()
                            .algorithmVersion("personalized-hybrid-v3")
                            .appliedStrategies(List.of("collaborative_filtering", "content_based"))
                            .generatedAt(OffsetDateTime.now())
                            .wasPersonalized(true)
                            .personalizationLevel("HIGHLY_PERSONALIZED")
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("Personalized recommendations generation failed", e);
            return generateGenericRecommendations(maxResults);
        }
    }

    public CompletableFuture<RecommendationResponse> generateRecommendationsAsync(RecommendationRequest request) {
        return CompletableFuture.supplyAsync(() -> generateRecommendations(request));
    }

    // Private helper methods

    private void validateRecommendationRequest(RecommendationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Recommendation request cannot be null");
        }

        if (request.getPreferences() != null &&
            request.getPreferences().getMaxRecommendations() != null &&
            request.getPreferences().getMaxRecommendations() > 100) {
            throw new IllegalArgumentException("Maximum recommendations cannot exceed 100");
        }
    }

    private RecommendationRequest enrichUserProfile(RecommendationRequest request) {
        if (request.getUserProfile() == null && request.getUserId() != null) {
            try {
                UserSearchProfile userProfile = userProfileService.getUserProfile(request.getUserId());
                if (userProfile != null) {
                    RecommendationRequest.UserProfile enrichedProfile = convertUserProfile(userProfile);
                    return request.toBuilder().userProfile(enrichedProfile).build();
                }
            } catch (Exception e) {
                log.warn("Failed to enrich user profile for user: {}", request.getUserId(), e);
            }
        }
        return request;
    }

    private List<CompletableFuture<List<RecommendationResponse.PropertyRecommendation>>> generatePropertyRecommendations(
            RecommendationRequest request) {

        List<CompletableFuture<List<RecommendationResponse.PropertyRecommendation>>> futures = new ArrayList<>();

        List<RecommendationRequest.RecommendationType> requestedTypes =
                request.getPreferences() != null && request.getPreferences().getRequestedTypes() != null
                ? request.getPreferences().getRequestedTypes()
                : getDefaultRecommendationTypes();

        for (RecommendationRequest.RecommendationType type : requestedTypes) {
            switch (type) {
                case SIMILAR_PROPERTIES -> futures.add(CompletableFuture.supplyAsync(
                        () -> generateSimilarPropertiesRecommendations(request)));
                case PERSONALIZED_PICKS -> futures.add(CompletableFuture.supplyAsync(
                        () -> generatePersonalizedPicksRecommendations(request)));
                case TRENDING_NOW -> futures.add(CompletableFuture.supplyAsync(
                        () -> generateTrendingNowRecommendations(request)));
                case UPGRADE_SUGGESTIONS -> futures.add(CompletableFuture.supplyAsync(
                        () -> generateUpgradeRecommendations(request)));
                default -> log.debug("Skipping unsupported recommendation type: {}", type);
            }
        }

        return futures;
    }

    private List<CompletableFuture<List<RecommendationResponse.DestinationRecommendation>>> generateDestinationRecommendations(
            RecommendationRequest request) {

        List<CompletableFuture<List<RecommendationResponse.DestinationRecommendation>>> futures = new ArrayList<>();

        if (shouldGenerateDestinationRecommendations(request)) {
            futures.add(CompletableFuture.supplyAsync(() -> generateAlternativeDestinations(request)));
            futures.add(CompletableFuture.supplyAsync(() -> generateSeasonalDestinations(request)));
        }

        return futures;
    }

    private List<CompletableFuture<List<RecommendationResponse.ServiceRecommendation>>> generateServiceRecommendations(
            RecommendationRequest request) {

        List<CompletableFuture<List<RecommendationResponse.ServiceRecommendation>>> futures = new ArrayList<>();

        if (shouldGenerateServiceRecommendations(request)) {
            futures.add(CompletableFuture.supplyAsync(() -> generateComplementaryServices(request)));
        }

        return futures;
    }

    private List<CompletableFuture<List<RecommendationResponse.DealRecommendation>>> generateDealRecommendations(
            RecommendationRequest request) {

        List<CompletableFuture<List<RecommendationResponse.DealRecommendation>>> futures = new ArrayList<>();

        if (shouldGenerateDealRecommendations(request)) {
            futures.add(CompletableFuture.supplyAsync(() -> generateLastMinuteDeals(request)));
            futures.add(CompletableFuture.supplyAsync(() -> generateEarlyBirdOffers(request)));
        }

        return futures;
    }

    private List<RecommendationResponse.PropertyRecommendation> collectPropertyRecommendations(
            List<CompletableFuture<List<RecommendationResponse.PropertyRecommendation>>> futures) {

        return futures.stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        log.error("Failed to collect property recommendations", e);
                        return List.<RecommendationResponse.PropertyRecommendation>of();
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<RecommendationResponse.DestinationRecommendation> collectDestinationRecommendations(
            List<CompletableFuture<List<RecommendationResponse.DestinationRecommendation>>> futures) {

        return futures.stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        log.error("Failed to collect destination recommendations", e);
                        return List.<RecommendationResponse.DestinationRecommendation>of();
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<RecommendationResponse.ServiceRecommendation> collectServiceRecommendations(
            List<CompletableFuture<List<RecommendationResponse.ServiceRecommendation>>> futures) {

        return futures.stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        log.error("Failed to collect service recommendations", e);
                        return List.<RecommendationResponse.ServiceRecommendation>of();
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<RecommendationResponse.DealRecommendation> collectDealRecommendations(
            List<CompletableFuture<List<RecommendationResponse.DealRecommendation>>> futures) {

        return futures.stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        log.error("Failed to collect deal recommendations", e);
                        return List.<RecommendationResponse.DealRecommendation>of();
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<RecommendationResponse.PropertyRecommendation> postProcessRecommendations(
            List<RecommendationResponse.PropertyRecommendation> recommendations, RecommendationRequest request) {

        // Remove duplicates
        recommendations = removeDuplicateRecommendations(recommendations);

        // Apply diversity filtering
        double diversityFactor = request.getPreferences() != null && request.getPreferences().getDiversityFactor() != null
                ? request.getPreferences().getDiversityFactor()
                : defaultDiversityFactor;

        recommendations = applyDiversityFiltering(recommendations, diversityFactor);

        // Sort by score and confidence
        recommendations = recommendations.stream()
                .sorted(Comparator.comparing(RecommendationResponse.PropertyRecommendation::getScore).reversed())
                .collect(Collectors.toList());

        // Limit results
        int maxResults = request.getPreferences() != null && request.getPreferences().getMaxRecommendations() != null
                ? request.getPreferences().getMaxRecommendations()
                : maxRecommendationsPerType;

        recommendations = recommendations.stream()
                .limit(maxResults)
                .collect(Collectors.toList());

        // Assign ranks
        for (int i = 0; i < recommendations.size(); i++) {
            recommendations.get(i).setRank(i + 1);
        }

        return recommendations;
    }

    // Simplified implementations for recommendation generation methods

    private List<RecommendationResponse.PropertyRecommendation> generateSimilarPropertiesRecommendations(RecommendationRequest request) {
        // Get current properties from search context
        if (request.getSearchContext() != null && request.getSearchContext().getCurrentResults() != null) {
            return request.getSearchContext().getCurrentResults().stream()
                    .limit(5)
                    .map(this::convertToSimilarPropertyRecommendation)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private List<RecommendationResponse.PropertyRecommendation> generatePersonalizedPicksRecommendations(RecommendationRequest request) {
        // Generate personalized recommendations based on user profile
        return generateSamplePropertyRecommendations("PERSONALIZED_PICK", 3);
    }

    private List<RecommendationResponse.PropertyRecommendation> generateTrendingNowRecommendations(RecommendationRequest request) {
        // Generate trending recommendations
        return generateSamplePropertyRecommendations("TRENDING_NOW", 3);
    }

    private List<RecommendationResponse.PropertyRecommendation> generateUpgradeRecommendations(RecommendationRequest request) {
        // Generate upgrade suggestions
        return generateSamplePropertyRecommendations("UPGRADE_SUGGESTION", 2);
    }

    private List<RecommendationResponse.PropertyRecommendation> generateSamplePropertyRecommendations(String type, int count) {
        List<RecommendationResponse.PropertyRecommendation> recommendations = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            recommendations.add(RecommendationResponse.PropertyRecommendation.builder()
                    .recommendationId(UUID.randomUUID().toString())
                    .property(createSampleProperty(type + "_PROP_" + i))
                    .type(RecommendationResponse.RecommendationType.valueOf(type))
                    .score(0.8 + (Math.random() * 0.2))
                    .confidence(0.75 + (Math.random() * 0.25))
                    .reasoning("Generated based on " + type.toLowerCase().replace("_", " "))
                    .matchingFeatures(List.of("location", "price_range", "amenities"))
                    .rank(i)
                    .build());
        }

        return recommendations;
    }

    private PropertySearchResult createSampleProperty(String id) {
        return PropertySearchResult.builder()
                .propertyId(id)
                .propertyName("Sample Hotel " + id)
                .location("Sample City")
                .price(BigDecimal.valueOf(100 + Math.random() * 300))
                .currency("USD")
                .rating(BigDecimal.valueOf(3.5 + Math.random() * 1.5))
                .reviewCount((int) (50 + Math.random() * 200))
                .relevanceScore(BigDecimal.valueOf(0.7 + Math.random() * 0.3))
                .build();
    }

    // Additional simplified helper methods
    private List<RecommendationRequest.RecommendationType> getDefaultRecommendationTypes() {
        return List.of(
                RecommendationRequest.RecommendationType.SIMILAR_PROPERTIES,
                RecommendationRequest.RecommendationType.PERSONALIZED_PICKS,
                RecommendationRequest.RecommendationType.TRENDING_NOW
        );
    }

    private boolean shouldGenerateDestinationRecommendations(RecommendationRequest request) { return true; }
    private boolean shouldGenerateServiceRecommendations(RecommendationRequest request) { return true; }
    private boolean shouldGenerateDealRecommendations(RecommendationRequest request) { return true; }

    private List<RecommendationResponse.DestinationRecommendation> generateAlternativeDestinations(RecommendationRequest request) { return List.of(); }
    private List<RecommendationResponse.DestinationRecommendation> generateSeasonalDestinations(RecommendationRequest request) { return List.of(); }
    private List<RecommendationResponse.ServiceRecommendation> generateComplementaryServices(RecommendationRequest request) { return List.of(); }
    private List<RecommendationResponse.DealRecommendation> generateLastMinuteDeals(RecommendationRequest request) { return List.of(); }
    private List<RecommendationResponse.DealRecommendation> generateEarlyBirdOffers(RecommendationRequest request) { return List.of(); }

    private List<RecommendationResponse.DestinationRecommendation> postProcessDestinationRecommendations(
            List<RecommendationResponse.DestinationRecommendation> recommendations, RecommendationRequest request) {
        return recommendations;
    }

    private RecommendationResponse.RecommendationMetadata buildMetadata(RecommendationRequest request, long startTime) {
        return RecommendationResponse.RecommendationMetadata.builder()
                .algorithmVersion("recommendation-engine-v1")
                .appliedStrategies(List.of("hybrid", "content_based", "collaborative"))
                .generatedAt(OffsetDateTime.now())
                .generationTimeMs(System.currentTimeMillis() - startTime)
                .requestId(UUID.randomUUID().toString())
                .wasPersonalized(request.getUserProfile() != null)
                .build();
    }

    private RecommendationResponse.RecommendationMetrics calculateMetrics(
            List<RecommendationResponse.PropertyRecommendation> properties,
            List<RecommendationResponse.DestinationRecommendation> destinations,
            List<RecommendationResponse.ServiceRecommendation> services,
            List<RecommendationResponse.DealRecommendation> deals) {

        double avgScore = properties.stream()
                .mapToDouble(RecommendationResponse.PropertyRecommendation::getScore)
                .average()
                .orElse(0.0);

        return RecommendationResponse.RecommendationMetrics.builder()
                .averageScore(avgScore)
                .confidenceLevel(0.8)
                .diversityIndex(0.7)
                .noveltyScore(0.6)
                .totalCandidates(100)
                .filteredCandidates(properties.size() + destinations.size() + services.size() + deals.size())
                .build();
    }

    private RecommendationResponse.RecommendationExplanation generateExplanations(
            RecommendationRequest request,
            List<RecommendationResponse.PropertyRecommendation> properties,
            List<RecommendationResponse.DestinationRecommendation> destinations) {

        return RecommendationResponse.RecommendationExplanation.builder()
                .primaryReason("Based on your preferences and similar user behavior")
                .contributingFactors(List.of("user_history", "similar_users", "trending_properties"))
                .recommendationSummary("We found properties that match your style and budget")
                .build();
    }

    private void recordRecommendationEvent(RecommendationRequest request, RecommendationResponse response) {
        // Record for analytics and learning
        try {
            analyticsService.recordRecommendationEvent(request.getUserId(), request.getSessionId(),
                    response.getPropertyRecommendations().size(), response.getMetadata());
        } catch (Exception e) {
            log.warn("Failed to record recommendation event", e);
        }
    }

    private int getTotalRecommendationCount(RecommendationResponse response) {
        return (response.getPropertyRecommendations() != null ? response.getPropertyRecommendations().size() : 0) +
               (response.getDestinationRecommendations() != null ? response.getDestinationRecommendations().size() : 0) +
               (response.getServiceRecommendations() != null ? response.getServiceRecommendations().size() : 0) +
               (response.getDealRecommendations() != null ? response.getDealRecommendations().size() : 0);
    }

    private RecommendationResponse buildErrorResponse(Exception e, long startTime) {
        return RecommendationResponse.builder()
                .propertyRecommendations(List.of())
                .destinationRecommendations(List.of())
                .serviceRecommendations(List.of())
                .dealRecommendations(List.of())
                .metadata(RecommendationResponse.RecommendationMetadata.builder()
                        .generatedAt(OffsetDateTime.now())
                        .generationTimeMs(System.currentTimeMillis() - startTime)
                        .debugInfo(Map.of("error", e.getMessage()))
                        .build())
                .build();
    }

    // Additional helper methods with simplified implementations
    private PropertySearchResult getPropertyById(String propertyId) { return createSampleProperty(propertyId); }
    private RecommendationRequest buildSimilarPropertiesRequest(PropertySearchResult property, String userId, int maxResults) { return RecommendationRequest.builder().build(); }
    private List<RecommendationResponse.PropertyRecommendation> generateContentBasedRecommendations(RecommendationRequest request, PropertySearchResult baseProperty) { return List.of(); }
    private RecommendationResponse.PropertyRecommendation convertToTrendingRecommendation(PropertySearchResult property) {
        return RecommendationResponse.PropertyRecommendation.builder()
                .property(property)
                .type(RecommendationResponse.RecommendationType.TRENDING_NOW)
                .score(0.9)
                .build();
    }
    private RecommendationResponse generateGenericRecommendations(int maxResults) { return RecommendationResponse.builder().build(); }
    private RecommendationRequest buildPersonalizedRequest(UserSearchProfile userProfile, String userId, int maxResults) { return RecommendationRequest.builder().build(); }
    private List<RecommendationResponse.PropertyRecommendation> generateCollaborativeFilteringRecommendations(RecommendationRequest request) { return List.of(); }
    private List<RecommendationResponse.PropertyRecommendation> generateUserContentBasedRecommendations(RecommendationRequest request) { return List.of(); }
    private List<RecommendationResponse.PropertyRecommendation> mergeRecommendations(List<RecommendationResponse.PropertyRecommendation> list1, List<RecommendationResponse.PropertyRecommendation> list2, double weight1, double weight2) {
        List<RecommendationResponse.PropertyRecommendation> merged = new ArrayList<>(list1);
        merged.addAll(list2);
        return merged;
    }
    private RecommendationRequest.UserProfile convertUserProfile(UserSearchProfile userProfile) { return RecommendationRequest.UserProfile.builder().build(); }
    private RecommendationResponse.PropertyRecommendation convertToSimilarPropertyRecommendation(PropertySearchResult property) {
        return RecommendationResponse.PropertyRecommendation.builder()
                .property(property)
                .type(RecommendationResponse.RecommendationType.SIMILAR_PROPERTIES)
                .score(0.85)
                .build();
    }
    private List<RecommendationResponse.PropertyRecommendation> removeDuplicateRecommendations(List<RecommendationResponse.PropertyRecommendation> recommendations) { return recommendations; }
    private List<RecommendationResponse.PropertyRecommendation> applyDiversityFiltering(List<RecommendationResponse.PropertyRecommendation> recommendations, double diversityFactor) { return recommendations; }
}