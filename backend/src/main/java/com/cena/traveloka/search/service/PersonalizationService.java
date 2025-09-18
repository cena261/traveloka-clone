package com.cena.traveloka.search.service;

import com.cena.traveloka.search.dto.PersonalizedSearchRequest;
import com.cena.traveloka.search.dto.PersonalizedSearchResponse;
import com.cena.traveloka.search.dto.PropertySearchRequest;
import com.cena.traveloka.search.dto.PropertySearchResponse;
import com.cena.traveloka.search.entity.UserSearchProfile;
import com.cena.traveloka.search.repository.UserSearchProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalizationService {

    private final UserSearchProfileRepository userProfileRepository;
    private final SearchService searchService;
    private final SearchAnalyticsService analyticsService;

    @Transactional(readOnly = true)
    @Cacheable(value = "personalizedSearch", key = "#request.userId + ':' + #request.query")
    public PersonalizedSearchResponse personalizedSearch(PersonalizedSearchRequest request) {
        log.info("Executing personalized search for user: {}, query: '{}'",
                request.getUserId(), request.getQuery());

        long startTime = System.currentTimeMillis();

        try {
            // Get or create user profile
            UserSearchProfile userProfile = getUserProfile(request.getUserId());

            // Apply personalization to base search request
            PropertySearchRequest personalizedRequest = applyPersonalization(request, userProfile);

            // Execute search
            PropertySearchResponse baseResponse = searchService.searchProperties(personalizedRequest);

            // Apply ML-based result reranking
            List<PropertySearchResult> rerankedResults = rerankeResults(
                baseResponse.getResults(), userProfile, request);

            // Generate personalized recommendations
            List<PersonalizedSearchResponse.Recommendation> recommendations =
                generateRecommendations(userProfile, request);

            // Build personalization insights
            PersonalizedSearchResponse.PersonalizationInsights insights =
                buildPersonalizationInsights(userProfile, request, baseResponse);

            long responseTime = System.currentTimeMillis() - startTime;

            // Record personalization interaction
            recordPersonalizationInteraction(request, userProfile, rerankedResults.size());

            return PersonalizedSearchResponse.builder()
                .results(rerankedResults)
                .recommendations(recommendations)
                .insights(insights)
                .pagination(baseResponse.getPagination())
                .metadata(enhanceMetadataWithPersonalization(baseResponse.getMetadata(),
                    userProfile, responseTime))
                .build();

        } catch (Exception e) {
            log.error("Personalized search failed for user: {}, query: '{}'",
                request.getUserId(), request.getQuery(), e);

            // Fallback to regular search
            return fallbackToRegularSearch(request);
        }
    }

    @Transactional
    public UserSearchProfile updateUserProfile(UUID userId, Map<String, Object> interactions) {
        log.info("Updating user profile for user: {} with {} interactions",
                userId, interactions.size());

        try {
            UserSearchProfile profile = getUserProfile(userId);

            // Update behavioral patterns
            updateBehavioralPatterns(profile, interactions);

            // Update preferences based on interactions
            updatePreferencesFromInteractions(profile, interactions);

            // Recalculate ML features
            recalculateMLFeatures(profile);

            // Update clustering segment
            updateClusteringSegment(profile);

            // Save updated profile
            profile.setLastInteractionAt(OffsetDateTime.now());
            return userProfileRepository.save(profile);

        } catch (Exception e) {
            log.error("Failed to update user profile for user: {}", userId, e);
            throw new RuntimeException("Failed to update user profile", e);
        }
    }

    @Transactional(readOnly = true)
    public List<PersonalizedSearchResponse.Recommendation> getPersonalizedRecommendations(
            UUID userId, String context, int limit) {

        log.info("Getting personalized recommendations for user: {}, context: '{}'",
                userId, context);

        try {
            UserSearchProfile profile = getUserProfile(userId);
            return generateContextualRecommendations(profile, context, limit);

        } catch (Exception e) {
            log.error("Failed to get personalized recommendations for user: {}", userId, e);
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Double> getUserSegmentAnalysis(UUID userId) {
        log.info("Analyzing user segment for user: {}", userId);

        try {
            UserSearchProfile profile = getUserProfile(userId);
            return analyzeUserSegment(profile);

        } catch (Exception e) {
            log.error("Failed to analyze user segment for user: {}", userId, e);
            return Map.of();
        }
    }

    @Async
    @Transactional
    public CompletableFuture<Void> asyncProfileUpdate(UUID userId, Map<String, Object> searchEvent) {
        log.debug("Async profile update for user: {}", userId);

        try {
            UserSearchProfile profile = getUserProfile(userId);

            // Update incremental learning features
            updateIncrementalFeatures(profile, searchEvent);

            // Decay old preferences (temporal decay)
            applyTemporalDecay(profile);

            userProfileRepository.save(profile);

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Async profile update failed for user: {}", userId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Transactional(readOnly = true)
    public List<UserSearchProfile> findSimilarUsers(UUID userId, int limit) {
        log.info("Finding similar users to user: {}", userId);

        try {
            UserSearchProfile userProfile = getUserProfile(userId);

            // Use collaborative filtering to find similar users
            return findUsersBySimilarity(userProfile, limit);

        } catch (Exception e) {
            log.error("Failed to find similar users for user: {}", userId, e);
            return List.of();
        }
    }

    // Private helper methods

    private UserSearchProfile getUserProfile(UUID userId) {
        return userProfileRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultProfile(userId));
    }

    private UserSearchProfile createDefaultProfile(UUID userId) {
        log.info("Creating default profile for new user: {}", userId);

        return UserSearchProfile.builder()
            .userId(userId)
            .preferredLanguage("vi")
            .travelPreferences(UserSearchProfile.TravelPreferences.builder()
                .travelFrequency("occasional")
                .tripDurationPreference("medium")
                .travelPurpose(Map.of("leisure", 0.7, "business", 0.3))
                .groupSizePreference(2)
                .advanceBookingDays(30)
                .build())
            .propertyPreferences(UserSearchProfile.PropertyPreferences.builder()
                .propertyTypes(Map.of("hotel", 0.8, "villa", 0.2))
                .starRatingPreference(Map.of(3, 0.3, 4, 0.5, 5, 0.2))
                .amenityImportance(Map.of("wifi", 0.9, "pool", 0.6, "parking", 0.7))
                .sustainabilityPreference(0.5)
                .locationPreference("city_center")
                .build())
            .pricePreferences(UserSearchProfile.PricePreferences.builder()
                .priceSensitivity(0.6)
                .preferredPriceRangeMin(BigDecimal.valueOf(500000))
                .preferredPriceRangeMax(BigDecimal.valueOf(2000000))
                .currencyPreference("VND")
                .dealSeekingBehavior(0.5)
                .priceVsQualityBalance(0.6)
                .bookingTimingPreference("flexible")
                .build())
            .behavioralPatterns(UserSearchProfile.BehavioralPatterns.builder()
                .searchFrequency(5)
                .avgSessionDuration(15)
                .pagesPerSession(8)
                .clickThroughRate(0.15)
                .conversionRate(0.05)
                .devicePreference(Map.of("mobile", 0.7, "desktop", 0.3))
                .filterUsagePattern(Map.of("location", 0.8, "price", 0.9, "amenities", 0.4))
                .build())
            .preferenceVector(initializePreferenceVector())
            .clusteringSegment("GENERAL")
            .personalizationScore(BigDecimal.valueOf(0.5))
            .build();
    }

    private PropertySearchRequest applyPersonalization(PersonalizedSearchRequest request,
                                                     UserSearchProfile profile) {
        var builder = PropertySearchRequest.builder()
            .query(enhanceQuery(request.getQuery(), profile))
            .language(profile.getPreferredLanguage())
            .pageable(request.getPageable());

        // Apply location preferences
        if (request.getLocation() == null && profile.getTravelPreferences().getPreferredDestinations() != null) {
            String topDestination = profile.getTravelPreferences().getPreferredDestinations()
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

            if (topDestination != null) {
                builder.location(PropertySearchRequest.LocationFilter.builder()
                    .city(topDestination)
                    .build());
            }
        }

        // Apply price preferences
        if (request.getPrice() == null) {
            builder.price(PropertySearchRequest.PriceFilter.builder()
                .minPrice(profile.getPricePreferences().getPreferredPriceRangeMin())
                .maxPrice(profile.getPricePreferences().getPreferredPriceRangeMax())
                .currency(profile.getPricePreferences().getCurrencyPreference())
                .build());
        }

        // Apply property preferences
        if (request.getProperty() == null) {
            var preferredTypes = profile.getPropertyPreferences().getPropertyTypes()
                .entrySet().stream()
                .filter(e -> e.getValue() > 0.5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

            var preferredStars = profile.getPropertyPreferences().getStarRatingPreference()
                .entrySet().stream()
                .filter(e -> e.getValue() > 0.3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

            var preferredAmenities = profile.getPropertyPreferences().getAmenityImportance()
                .entrySet().stream()
                .filter(e -> e.getValue() > 0.7)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

            builder.property(PropertySearchRequest.PropertyFilter.builder()
                .propertyTypes(preferredTypes)
                .starRating(preferredStars)
                .amenities(preferredAmenities)
                .build());
        }

        return builder.build();
    }

    private List<PropertySearchResult> rerankeResults(List<PropertySearchResult> originalResults,
                                                    UserSearchProfile profile,
                                                    PersonalizedSearchRequest request) {
        log.debug("Reranking {} results for personalization", originalResults.size());

        return originalResults.stream()
            .map(result -> {
                double personalizedScore = calculatePersonalizedScore(result, profile);
                result.setPersonalizationScore(personalizedScore);
                return result;
            })
            .sorted((r1, r2) -> Double.compare(
                r2.getPersonalizationScore().doubleValue(),
                r1.getPersonalizationScore().doubleValue()))
            .collect(Collectors.toList());
    }

    private double calculatePersonalizedScore(PropertySearchResult result, UserSearchProfile profile) {
        double score = 0.0;

        // Property type preference
        String propertyType = result.getKind();
        if (profile.getPropertyPreferences().getPropertyTypes().containsKey(propertyType)) {
            score += profile.getPropertyPreferences().getPropertyTypes().get(propertyType) * 0.3;
        }

        // Star rating preference
        Integer starRating = result.getStarRating();
        if (starRating != null && profile.getPropertyPreferences().getStarRatingPreference().containsKey(starRating)) {
            score += profile.getPropertyPreferences().getStarRatingPreference().get(starRating) * 0.2;
        }

        // Price preference
        if (result.getPricing() != null && result.getPricing().getStartingPrice() != null) {
            BigDecimal price = result.getPricing().getStartingPrice();
            double priceScore = calculatePriceScore(price, profile.getPricePreferences());
            score += priceScore * 0.25;
        }

        // Amenity preference
        if (result.getAmenities() != null) {
            double amenityScore = calculateAmenityScore(result.getAmenities(), profile.getPropertyPreferences());
            score += amenityScore * 0.15;
        }

        // Rating preference based on user's quality vs price balance
        if (result.getRating() != null && result.getRating().getAverage() != null) {
            double qualityWeight = profile.getPricePreferences().getPriceVsQualityBalance();
            double ratingScore = result.getRating().getAverage().doubleValue() / 5.0;
            score += ratingScore * qualityWeight * 0.1;
        }

        return Math.min(1.0, Math.max(0.0, score));
    }

    private double calculatePriceScore(BigDecimal price, UserSearchProfile.PricePreferences pricePrefs) {
        if (pricePrefs.getPreferredPriceRangeMin() == null || pricePrefs.getPreferredPriceRangeMax() == null) {
            return 0.5; // Neutral score if no price preferences
        }

        if (price.compareTo(pricePrefs.getPreferredPriceRangeMin()) >= 0 &&
            price.compareTo(pricePrefs.getPreferredPriceRangeMax()) <= 0) {
            return 1.0; // Perfect match
        }

        // Calculate distance from preferred range
        BigDecimal distance;
        if (price.compareTo(pricePrefs.getPreferredPriceRangeMin()) < 0) {
            distance = pricePrefs.getPreferredPriceRangeMin().subtract(price);
        } else {
            distance = price.subtract(pricePrefs.getPreferredPriceRangeMax());
        }

        BigDecimal rangeSize = pricePrefs.getPreferredPriceRangeMax().subtract(pricePrefs.getPreferredPriceRangeMin());
        double normalizedDistance = distance.divide(rangeSize, RoundingMode.HALF_UP).doubleValue();

        return Math.max(0.0, 1.0 - normalizedDistance);
    }

    private double calculateAmenityScore(List<String> amenities, UserSearchProfile.PropertyPreferences propPrefs) {
        if (propPrefs.getAmenityImportance() == null || propPrefs.getAmenityImportance().isEmpty()) {
            return 0.5;
        }

        double totalImportance = propPrefs.getAmenityImportance().values().stream()
            .mapToDouble(Double::doubleValue).sum();

        double matchedImportance = amenities.stream()
            .filter(amenity -> propPrefs.getAmenityImportance().containsKey(amenity))
            .mapToDouble(amenity -> propPrefs.getAmenityImportance().get(amenity))
            .sum();

        return totalImportance > 0 ? matchedImportance / totalImportance : 0.0;
    }

    // Placeholder implementations for other methods
    private String enhanceQuery(String originalQuery, UserSearchProfile profile) {
        // Could add user preference keywords or synonyms
        return originalQuery;
    }

    private List<PersonalizedSearchResponse.Recommendation> generateRecommendations(
            UserSearchProfile profile, PersonalizedSearchRequest request) {
        // Generate ML-based recommendations
        return List.of();
    }

    private PersonalizedSearchResponse.PersonalizationInsights buildPersonalizationInsights(
            UserSearchProfile profile, PersonalizedSearchRequest request, PropertySearchResponse baseResponse) {
        return PersonalizedSearchResponse.PersonalizationInsights.builder()
            .userSegment(profile.getClusteringSegment())
            .personalizationScore(profile.getPersonalizationScore())
            .appliedPreferences(List.of("price_range", "property_type", "amenities"))
            .build();
    }

    private Map<String, Object> enhanceMetadataWithPersonalization(
            Object originalMetadata, UserSearchProfile profile, long responseTime) {
        return Map.of(
            "personalized", true,
            "userSegment", profile.getClusteringSegment(),
            "personalizationScore", profile.getPersonalizationScore(),
            "responseTime", responseTime
        );
    }

    private PersonalizedSearchResponse fallbackToRegularSearch(PersonalizedSearchRequest request) {
        // Fallback to regular search if personalization fails
        return PersonalizedSearchResponse.builder()
            .results(List.of())
            .recommendations(List.of())
            .insights(PersonalizedSearchResponse.PersonalizationInsights.builder().build())
            .build();
    }

    private void recordPersonalizationInteraction(PersonalizedSearchRequest request,
                                                UserSearchProfile profile, int resultCount) {
        // Record the personalization interaction for future learning
        log.debug("Recording personalization interaction for user: {}", request.getUserId());
    }

    private Map<String, Double> initializePreferenceVector() {
        // Initialize ML preference vector with default values
        Map<String, Double> vector = new HashMap<>();
        vector.put("price_sensitivity", 0.5);
        vector.put("quality_preference", 0.6);
        vector.put("location_importance", 0.7);
        vector.put("amenity_importance", 0.5);
        vector.put("brand_loyalty", 0.3);
        return vector;
    }

    // Additional placeholder methods for ML operations
    private void updateBehavioralPatterns(UserSearchProfile profile, Map<String, Object> interactions) {}
    private void updatePreferencesFromInteractions(UserSearchProfile profile, Map<String, Object> interactions) {}
    private void recalculateMLFeatures(UserSearchProfile profile) {}
    private void updateClusteringSegment(UserSearchProfile profile) {}
    private List<PersonalizedSearchResponse.Recommendation> generateContextualRecommendations(
            UserSearchProfile profile, String context, int limit) { return List.of(); }
    private Map<String, Double> analyzeUserSegment(UserSearchProfile profile) { return Map.of(); }
    private void updateIncrementalFeatures(UserSearchProfile profile, Map<String, Object> searchEvent) {}
    private void applyTemporalDecay(UserSearchProfile profile) {}
    private List<UserSearchProfile> findUsersBySimilarity(UserSearchProfile userProfile, int limit) { return List.of(); }
}