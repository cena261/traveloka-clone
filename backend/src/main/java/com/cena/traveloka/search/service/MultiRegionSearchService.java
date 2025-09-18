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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultiRegionSearchService {

    private final RegionConfigService regionConfigService;
    private final SearchService searchService;
    private final CacheService cacheService;
    private final AnalyticsService analyticsService;

    @Value("${search.multiregion.max-parallel-regions:3}")
    private int maxParallelRegions;

    @Value("${search.multiregion.default-timeout:10000}")
    private long defaultTimeoutMs;

    @Value("${search.multiregion.enable-caching:true}")
    private boolean enableCaching;

    @Value("${search.multiregion.deduplication-threshold:0.85}")
    private double deduplicationThreshold;

    @Cacheable(value = "multiRegionSearch", key = "#request.hashCode()", condition = "#request.optimizationSettings?.enableCaching == true")
    public MultiRegionSearchResponse executeMultiRegionSearch(MultiRegionSearchRequest request) {
        log.info("Executing multi-region search for query: {}, destination: {}",
                request.getQuery(), request.getDestination());

        long startTime = System.currentTimeMillis();

        try {
            // Validate request
            validateMultiRegionSearchRequest(request);

            // Determine target regions
            List<String> targetRegions = determineTargetRegions(request);

            // Execute searches across regions
            List<CompletableFuture<RegionSearchResult>> searchFutures = executeParallelSearches(request, targetRegions);

            // Collect results with timeout handling
            List<RegionSearchResult> regionResults = collectSearchResults(searchFutures, request);

            // Merge and deduplicate results
            MultiRegionSearchResponse.ResultsMergingInfo mergingInfo = mergeAndDeduplicateResults(regionResults, request);

            // Build final response
            MultiRegionSearchResponse response = buildMultiRegionResponse(regionResults, mergingInfo, request, startTime);

            // Record analytics
            recordMultiRegionAnalytics(request, response, startTime);

            log.info("Multi-region search completed in {}ms with {} results from {} regions",
                    System.currentTimeMillis() - startTime,
                    response.getResults().size(),
                    response.getRegionResults().size());

            return response;

        } catch (Exception e) {
            log.error("Multi-region search failed", e);
            return buildErrorResponse(e, startTime);
        }
    }

    public List<String> getOptimalRegionsForRequest(MultiRegionSearchRequest request) {
        log.debug("Determining optimal regions for search request");

        try {
            Set<String> candidateRegions = new HashSet<>();

            // Strategy 1: User-specified preferred regions
            if (request.getRegionalPreferences() != null &&
                request.getRegionalPreferences().getPreferredRegions() != null) {
                candidateRegions.addAll(request.getRegionalPreferences().getPreferredRegions());
            }

            // Strategy 2: Geographic constraints
            if (request.getGeographicConstraints() != null) {
                candidateRegions.addAll(getRegionsByGeographicConstraints(request.getGeographicConstraints()));
            }

            // Strategy 3: User context (IP geolocation)
            if (request.getUserContext() != null && request.getUserContext().getDetectedCountry() != null) {
                candidateRegions.addAll(regionConfigService.getRegionsForCountry(request.getUserContext().getDetectedCountry()));
            }

            // Strategy 4: Destination-based regions
            if (request.getDestination() != null) {
                candidateRegions.addAll(getRegionsByDestination(request.getDestination()));
            }

            // Filter out excluded regions
            if (request.getRegionalPreferences() != null &&
                request.getRegionalPreferences().getExcludedRegions() != null) {
                candidateRegions.removeAll(request.getRegionalPreferences().getExcludedRegions());
            }

            // Filter by health and availability
            List<String> healthyRegions = candidateRegions.stream()
                    .filter(regionConfigService::isRegionHealthy)
                    .collect(Collectors.toList());

            // Sort by priority and limit
            return prioritizeAndLimitRegions(healthyRegions, request);

        } catch (Exception e) {
            log.error("Failed to determine optimal regions", e);
            return List.of(regionConfigService.determineOptimalRegion(null, null, null));
        }
    }

    public MultiRegionSearchResponse.SearchQualityMetrics assessSearchQuality(MultiRegionSearchResponse response) {
        log.debug("Assessing search quality for multi-region response");

        try {
            BigDecimal relevanceScore = calculateRelevanceScore(response);
            BigDecimal diversityScore = calculateDiversityScore(response);
            BigDecimal freshnessScore = calculateFreshnessScore(response);
            BigDecimal coverageScore = calculateCoverageScore(response);

            BigDecimal overallQuality = relevanceScore
                    .multiply(BigDecimal.valueOf(0.4))
                    .add(diversityScore.multiply(BigDecimal.valueOf(0.25)))
                    .add(freshnessScore.multiply(BigDecimal.valueOf(0.2)))
                    .add(coverageScore.multiply(BigDecimal.valueOf(0.15)));

            String qualityGrade = determineQualityGrade(overallQuality);

            return MultiRegionSearchResponse.SearchQualityMetrics.builder()
                    .overallQuality(overallQuality)
                    .relevanceScore(relevanceScore)
                    .diversityScore(diversityScore)
                    .freshnessScore(freshnessScore)
                    .coverageScore(coverageScore)
                    .qualityGrade(qualityGrade)
                    .qualityFactors(generateQualityFactors(response))
                    .improvementSuggestions(generateImprovementSuggestions(response))
                    .build();

        } catch (Exception e) {
            log.error("Failed to assess search quality", e);
            return createDefaultQualityMetrics();
        }
    }

    // Private helper methods

    private void validateMultiRegionSearchRequest(MultiRegionSearchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Search request cannot be null");
        }

        if (request.getQuery() == null && request.getDestination() == null) {
            throw new IllegalArgumentException("Either query or destination must be provided");
        }

        if (request.getCrossRegionSettings() != null &&
            request.getCrossRegionSettings().getMaxRegionsToQuery() != null &&
            request.getCrossRegionSettings().getMaxRegionsToQuery() > 10) {
            throw new IllegalArgumentException("Maximum regions to query cannot exceed 10");
        }
    }

    private List<String> determineTargetRegions(MultiRegionSearchRequest request) {
        log.debug("Determining target regions for multi-region search");

        List<String> optimalRegions = getOptimalRegionsForRequest(request);

        int maxRegions = request.getCrossRegionSettings() != null &&
                         request.getCrossRegionSettings().getMaxRegionsToQuery() != null
                         ? request.getCrossRegionSettings().getMaxRegionsToQuery()
                         : maxParallelRegions;

        return optimalRegions.stream()
                .limit(maxRegions)
                .collect(Collectors.toList());
    }

    private List<CompletableFuture<RegionSearchResult>> executeParallelSearches(
            MultiRegionSearchRequest request, List<String> targetRegions) {

        log.debug("Executing parallel searches across {} regions", targetRegions.size());

        return targetRegions.stream()
                .map(regionId -> executeSearchInRegionAsync(request, regionId))
                .collect(Collectors.toList());
    }

    private CompletableFuture<RegionSearchResult> executeSearchInRegionAsync(
            MultiRegionSearchRequest request, String regionId) {

        return CompletableFuture.supplyAsync(() -> {
            long regionStartTime = System.currentTimeMillis();

            try {
                log.debug("Executing search in region: {}", regionId);

                // Check cache first
                if (enableCaching) {
                    RegionSearchResult cachedResult = getCachedRegionResult(request, regionId);
                    if (cachedResult != null) {
                        log.debug("Cache hit for region: {}", regionId);
                        return cachedResult;
                    }
                }

                // Convert to region-specific search request
                SearchRequest regionSearchRequest = convertToRegionSearchRequest(request, regionId);

                // Execute search
                SearchResponse searchResponse = searchService.search(regionSearchRequest);

                // Create region result
                RegionSearchResult regionResult = RegionSearchResult.builder()
                        .regionId(regionId)
                        .regionName(getRegionName(regionId))
                        .status("SUCCESS")
                        .responseTime(System.currentTimeMillis() - regionStartTime)
                        .results(searchResponse.getResults())
                        .resultCount(searchResponse.getResults().size())
                        .cacheHit(false)
                        .build();

                // Cache result
                if (enableCaching) {
                    cacheRegionResult(request, regionId, regionResult);
                }

                return regionResult;

            } catch (Exception e) {
                log.error("Search failed in region: {}", regionId, e);

                return RegionSearchResult.builder()
                        .regionId(regionId)
                        .regionName(getRegionName(regionId))
                        .status("FAILURE")
                        .responseTime(System.currentTimeMillis() - regionStartTime)
                        .results(List.of())
                        .resultCount(0)
                        .errorMessage(e.getMessage())
                        .cacheHit(false)
                        .build();
            }
        });
    }

    private List<RegionSearchResult> collectSearchResults(
            List<CompletableFuture<RegionSearchResult>> searchFutures,
            MultiRegionSearchRequest request) {

        long timeout = request.getRoutingPreferences() != null &&
                       request.getRoutingPreferences().getMaxResponseTime() != null
                       ? request.getRoutingPreferences().getMaxResponseTime()
                       : defaultTimeoutMs;

        return searchFutures.stream()
                .map(future -> {
                    try {
                        return future.get(timeout, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.error("Failed to collect search result", e);
                        return RegionSearchResult.builder()
                                .status("TIMEOUT")
                                .errorMessage("Region search timeout: " + e.getMessage())
                                .results(List.of())
                                .resultCount(0)
                                .build();
                    }
                })
                .collect(Collectors.toList());
    }

    private MultiRegionSearchResponse.ResultsMergingInfo mergeAndDeduplicateResults(
            List<RegionSearchResult> regionResults, MultiRegionSearchRequest request) {

        log.debug("Merging and deduplicating results from {} regions", regionResults.size());

        long mergingStartTime = System.currentTimeMillis();

        List<PropertySearchResult> allResults = regionResults.stream()
                .flatMap(regionResult -> regionResult.getResults().stream())
                .collect(Collectors.toList());

        int totalBeforeMerging = allResults.size();

        // Apply merging strategy
        String mergingStrategy = request.getCrossRegionSettings() != null &&
                                request.getCrossRegionSettings().getResultsMergingStrategy() != null
                                ? request.getCrossRegionSettings().getResultsMergingStrategy()
                                : "weighted";

        List<PropertySearchResult> mergedResults = applyMergingStrategy(allResults, mergingStrategy, request);

        // Deduplicate if enabled
        int duplicatesRemoved = 0;
        if (request.getCrossRegionSettings() != null &&
            Boolean.TRUE.equals(request.getCrossRegionSettings().getDeduplicateResults())) {
            int beforeDeduplication = mergedResults.size();
            mergedResults = deduplicateResults(mergedResults, request);
            duplicatesRemoved = beforeDeduplication - mergedResults.size();
        }

        return MultiRegionSearchResponse.ResultsMergingInfo.builder()
                .mergingStrategy(mergingStrategy)
                .totalResultsBeforeMerging(totalBeforeMerging)
                .totalResultsAfterMerging(mergedResults.size())
                .duplicatesRemoved(duplicatesRemoved)
                .deduplicationStrategy(getDeduplicationStrategy(request))
                .mergingConfidence(calculateMergingConfidence(regionResults))
                .build();
    }

    private List<PropertySearchResult> applyMergingStrategy(
            List<PropertySearchResult> results, String strategy, MultiRegionSearchRequest request) {

        return switch (strategy.toLowerCase()) {
            case "union" -> results; // No special processing needed
            case "weighted" -> applyWeightedMerging(results, request);
            case "round_robin" -> applyRoundRobinMerging(results, request);
            case "relevance_based" -> applyRelevanceBasedMerging(results);
            case "distance_based" -> applyDistanceBasedMerging(results, request);
            default -> results;
        };
    }

    private List<PropertySearchResult> applyWeightedMerging(
            List<PropertySearchResult> results, MultiRegionSearchRequest request) {

        // Apply regional weights if specified
        Map<String, BigDecimal> regionWeights = request.getCrossRegionSettings() != null &&
                                               request.getCrossRegionSettings().getRegionWeights() != null
                                               ? request.getCrossRegionSettings().getRegionWeights()
                                               : Map.of();

        return results.stream()
                .sorted((r1, r2) -> {
                    // Sort by weighted score (implement based on your PropertySearchResult structure)
                    return Double.compare(calculateWeightedScore(r2, regionWeights),
                                         calculateWeightedScore(r1, regionWeights));
                })
                .collect(Collectors.toList());
    }

    private List<PropertySearchResult> applyRoundRobinMerging(
            List<PropertySearchResult> results, MultiRegionSearchRequest request) {
        // Implement round-robin merging logic
        return results; // Simplified implementation
    }

    private List<PropertySearchResult> applyRelevanceBasedMerging(List<PropertySearchResult> results) {
        // Sort by relevance score
        return results.stream()
                .sorted(Comparator.comparing(PropertySearchResult::getRelevanceScore).reversed())
                .collect(Collectors.toList());
    }

    private List<PropertySearchResult> applyDistanceBasedMerging(
            List<PropertySearchResult> results, MultiRegionSearchRequest request) {
        // Sort by distance from user location or specified coordinates
        return results; // Simplified implementation
    }

    private List<PropertySearchResult> deduplicateResults(
            List<PropertySearchResult> results, MultiRegionSearchRequest request) {

        String deduplicationStrategy = request.getCrossRegionSettings() != null &&
                                      request.getCrossRegionSettings().getDuplicateDetectionStrategy() != null
                                      ? request.getCrossRegionSettings().getDuplicateDetectionStrategy()
                                      : "property_id";

        Set<PropertySearchResult> uniqueResults = new LinkedHashSet<>();
        Set<String> seenIdentifiers = new HashSet<>();

        for (PropertySearchResult result : results) {
            String identifier = generateDeduplicationIdentifier(result, deduplicationStrategy);

            if (!seenIdentifiers.contains(identifier)) {
                uniqueResults.add(result);
                seenIdentifiers.add(identifier);
            }
        }

        return new ArrayList<>(uniqueResults);
    }

    private String generateDeduplicationIdentifier(PropertySearchResult result, String strategy) {
        return switch (strategy.toLowerCase()) {
            case "property_id" -> result.getPropertyId();
            case "coordinates" -> result.getLatitude() + "," + result.getLongitude();
            case "name_similarity" -> result.getPropertyName().toLowerCase().trim();
            default -> result.getPropertyId();
        };
    }

    private MultiRegionSearchResponse buildMultiRegionResponse(
            List<RegionSearchResult> regionResults,
            MultiRegionSearchResponse.ResultsMergingInfo mergingInfo,
            MultiRegionSearchRequest request,
            long startTime) {

        // Collect all merged results
        List<PropertySearchResult> allResults = regionResults.stream()
                .flatMap(regionResult -> regionResult.getResults().stream())
                .collect(Collectors.toList());

        // Build performance metrics
        MultiRegionSearchResponse.RegionPerformanceMetrics performanceMetrics = buildPerformanceMetrics(regionResults, startTime);

        // Build region results summary
        List<MultiRegionSearchResponse.RegionResult> regionResultsSummary = buildRegionResultsSummary(regionResults);

        // Build compliance info
        MultiRegionSearchResponse.ComplianceInfo complianceInfo = buildComplianceInfo(request, regionResults);

        // Build caching info
        MultiRegionSearchResponse.CachingInfo cachingInfo = buildCachingInfo(regionResults);

        return MultiRegionSearchResponse.builder()
                .results(allResults)
                .performanceMetrics(performanceMetrics)
                .mergingInfo(mergingInfo)
                .regionResults(regionResultsSummary)
                .complianceInfo(complianceInfo)
                .cachingInfo(cachingInfo)
                .pagination(buildPaginationInfo(allResults, request))
                .metadata(buildResponseMetadata(request, regionResults))
                .build();
    }

    private MultiRegionSearchResponse.RegionPerformanceMetrics buildPerformanceMetrics(
            List<RegionSearchResult> regionResults, long startTime) {

        List<MultiRegionSearchResponse.RegionPerformance> regionPerformances = regionResults.stream()
                .map(result -> MultiRegionSearchResponse.RegionPerformance.builder()
                        .regionId(result.getRegionId())
                        .regionName(result.getRegionName())
                        .status(result.getStatus())
                        .responseTime(result.getResponseTime())
                        .resultCount(result.getResultCount())
                        .cacheHit(result.getCacheHit())
                        .errorMessage(result.getErrorMessage())
                        .build())
                .collect(Collectors.toList());

        int successfulRegions = (int) regionResults.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
        int failedRegions = regionResults.size() - successfulRegions;

        OptionalLong fastestTime = regionResults.stream().mapToLong(RegionSearchResult::getResponseTime).min();
        OptionalLong slowestTime = regionResults.stream().mapToLong(RegionSearchResult::getResponseTime).max();

        return MultiRegionSearchResponse.RegionPerformanceMetrics.builder()
                .totalRegionsQueried(regionResults.size())
                .successfulRegions(successfulRegions)
                .failedRegions(failedRegions)
                .totalResponseTime(System.currentTimeMillis() - startTime)
                .fastestRegionTime(fastestTime.isPresent() ? fastestTime.getAsLong() : 0L)
                .slowestRegionTime(slowestTime.isPresent() ? slowestTime.getAsLong() : 0L)
                .regionPerformances(regionPerformances)
                .overallHealthStatus(failedRegions == 0 ? "HEALTHY" : failedRegions < regionResults.size() ? "DEGRADED" : "UNHEALTHY")
                .build();
    }

    // Additional helper methods...

    private List<String> getRegionsByGeographicConstraints(MultiRegionSearchRequest.GeographicConstraints constraints) {
        if (constraints.getBoundingBox() != null) {
            String[] bounds = constraints.getBoundingBox().split(",");
            if (bounds.length == 4) {
                BigDecimal north = new BigDecimal(bounds[0]);
                BigDecimal south = new BigDecimal(bounds[1]);
                BigDecimal east = new BigDecimal(bounds[2]);
                BigDecimal west = new BigDecimal(bounds[3]);
                return regionConfigService.getRegionsByGeographicBounds(north, south, east, west);
            }
        }
        return List.of();
    }

    private List<String> getRegionsByDestination(String destination) {
        // Simplified implementation - in production, this would use geolocation services
        return List.of();
    }

    private List<String> prioritizeAndLimitRegions(List<String> regions, MultiRegionSearchRequest request) {
        return regions.stream()
                .sorted(Comparator.comparing(region -> regionConfigService.getRegionHealthScores().getOrDefault(region, 0)).reversed())
                .limit(maxParallelRegions)
                .collect(Collectors.toList());
    }

    private double calculateWeightedScore(PropertySearchResult result, Map<String, BigDecimal> regionWeights) {
        // Implement weighted scoring logic
        return result.getRelevanceScore().doubleValue();
    }

    private BigDecimal calculateMergingConfidence(List<RegionSearchResult> regionResults) {
        int successfulRegions = (int) regionResults.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
        return BigDecimal.valueOf((double) successfulRegions / regionResults.size()).setScale(2, RoundingMode.HALF_UP);
    }

    // Quality assessment methods
    private BigDecimal calculateRelevanceScore(MultiRegionSearchResponse response) {
        return response.getResults().stream()
                .map(PropertySearchResult::getRelevanceScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(response.getResults().size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDiversityScore(MultiRegionSearchResponse response) {
        // Implement diversity calculation
        return BigDecimal.valueOf(0.75);
    }

    private BigDecimal calculateFreshnessScore(MultiRegionSearchResponse response) {
        // Implement freshness calculation
        return BigDecimal.valueOf(0.80);
    }

    private BigDecimal calculateCoverageScore(MultiRegionSearchResponse response) {
        // Implement coverage calculation
        return BigDecimal.valueOf(0.85);
    }

    private String determineQualityGrade(BigDecimal overallQuality) {
        if (overallQuality.compareTo(BigDecimal.valueOf(0.90)) >= 0) return "A";
        if (overallQuality.compareTo(BigDecimal.valueOf(0.80)) >= 0) return "B";
        if (overallQuality.compareTo(BigDecimal.valueOf(0.70)) >= 0) return "C";
        if (overallQuality.compareTo(BigDecimal.valueOf(0.60)) >= 0) return "D";
        return "F";
    }

    // Simplified implementations for other helper methods
    private RegionSearchResult getCachedRegionResult(MultiRegionSearchRequest request, String regionId) { return null; }
    private void cacheRegionResult(MultiRegionSearchRequest request, String regionId, RegionSearchResult result) {}
    private SearchRequest convertToRegionSearchRequest(MultiRegionSearchRequest request, String regionId) { return SearchRequest.builder().build(); }
    private String getRegionName(String regionId) { return regionId; }
    private String getDeduplicationStrategy(MultiRegionSearchRequest request) { return "property_id"; }
    private List<MultiRegionSearchResponse.RegionResult> buildRegionResultsSummary(List<RegionSearchResult> regionResults) { return List.of(); }
    private MultiRegionSearchResponse.ComplianceInfo buildComplianceInfo(MultiRegionSearchRequest request, List<RegionSearchResult> regionResults) { return MultiRegionSearchResponse.ComplianceInfo.builder().build(); }
    private MultiRegionSearchResponse.CachingInfo buildCachingInfo(List<RegionSearchResult> regionResults) { return MultiRegionSearchResponse.CachingInfo.builder().build(); }
    private PaginationInfo buildPaginationInfo(List<PropertySearchResult> results, MultiRegionSearchRequest request) { return PaginationInfo.builder().build(); }
    private Map<String, Object> buildResponseMetadata(MultiRegionSearchRequest request, List<RegionSearchResult> regionResults) { return Map.of(); }
    private List<String> generateQualityFactors(MultiRegionSearchResponse response) { return List.of(); }
    private List<String> generateImprovementSuggestions(MultiRegionSearchResponse response) { return List.of(); }
    private MultiRegionSearchResponse.SearchQualityMetrics createDefaultQualityMetrics() { return MultiRegionSearchResponse.SearchQualityMetrics.builder().build(); }
    private void recordMultiRegionAnalytics(MultiRegionSearchRequest request, MultiRegionSearchResponse response, long startTime) {}
    private MultiRegionSearchResponse buildErrorResponse(Exception e, long startTime) { return MultiRegionSearchResponse.builder().build(); }

    // Inner class for region search results
    @lombok.Data
    @lombok.Builder
    private static class RegionSearchResult {
        private String regionId;
        private String regionName;
        private String status;
        private Long responseTime;
        private List<PropertySearchResult> results;
        private Integer resultCount;
        private Boolean cacheHit;
        private String errorMessage;
    }
}