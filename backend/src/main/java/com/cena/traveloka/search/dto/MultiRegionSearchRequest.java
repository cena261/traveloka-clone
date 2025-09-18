package com.cena.traveloka.search.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@Jacksonized
public class MultiRegionSearchRequest {

    // Core search parameters
    private String query;
    private String destination;
    private String checkInDate;
    private String checkOutDate;
    private Integer adults;
    private Integer children;
    private Integer rooms;

    // Regional preferences
    private RegionalPreferences regionalPreferences;

    // Location and geographic constraints
    private GeographicConstraints geographicConstraints;

    // Cross-region settings
    private CrossRegionSettings crossRegionSettings;

    // Performance and routing
    private RoutingPreferences routingPreferences;

    // Compliance and privacy
    private CompliancePreferences compliancePreferences;

    // User context
    private UserContext userContext;

    // Search optimization
    private OptimizationSettings optimizationSettings;

    @Data
    @Builder
    @Jacksonized
    public static class RegionalPreferences {
        private List<String> preferredRegions;
        private List<String> excludedRegions;
        private String primaryRegion;
        private Boolean allowCrossRegionResults;
        private String currencyPreference;
        private String languagePreference;
        private String timeZonePreference;
        private Map<String, Object> regionSpecificSettings;
    }

    @Data
    @Builder
    @Jacksonized
    public static class GeographicConstraints {
        private BigDecimal centerLatitude;
        private BigDecimal centerLongitude;
        private BigDecimal radiusKm;
        private List<String> includedCountries;
        private List<String> excludedCountries;
        private List<String> includedCities;
        private List<String> excludedCities;
        private String boundingBox; // "north,south,east,west"
        private Boolean strictGeographicFiltering;
    }

    @Data
    @Builder
    @Jacksonized
    public static class CrossRegionSettings {
        private Boolean enableCrossRegionSearch;
        private Integer maxRegionsToQuery;
        private String resultsMergingStrategy; // union, intersection, weighted
        private Boolean deduplicateResults;
        private String duplicateDetectionStrategy; // property_id, coordinates, name_similarity
        private Boolean enableGlobalRanking;
        private Map<String, BigDecimal> regionWeights;
        private Integer maxResultsPerRegion;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RoutingPreferences {
        private String routingStrategy; // nearest, fastest, cheapest, balanced
        private Boolean allowFallbackRegions;
        private Long maxResponseTime;
        private Integer maxRetries;
        private Boolean enableLoadBalancing;
        private String loadBalancingAlgorithm;
        private List<String> priorityRegions;
        private Map<String, Integer> regionTimeouts;
    }

    @Data
    @Builder
    @Jacksonized
    public static class CompliancePreferences {
        private Boolean enforceGdpr;
        private Boolean enforceCcpa;
        private List<String> dataProcessingRestrictions;
        private Boolean requireExplicitConsent;
        private String privacyLevel; // minimal, standard, strict
        private List<String> allowedDataCategories;
        private Boolean enableDataLocalization;
        private Map<String, String> localComplianceSettings;
    }

    @Data
    @Builder
    @Jacksonized
    public static class UserContext {
        private String userId;
        private String sessionId;
        private String ipAddress;
        private String userAgent;
        private String deviceType;
        private String operatingSystem;
        private String browserType;
        private String appVersion;
        private String locationSource; // gps, ip, manual
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String detectedCountry;
        private String detectedRegion;
        private String detectedTimezone;
        private Map<String, String> customHeaders;
    }

    @Data
    @Builder
    @Jacksonized
    public static class OptimizationSettings {
        private Boolean enableCaching;
        private Integer cacheTimeSeconds;
        private Boolean enableParallelSearch;
        private Integer maxParallelRegions;
        private Boolean enableResultsPrefetching;
        private String cacheStrategy; // local, distributed, hybrid
        private Boolean enableSearchOptimization;
        private String optimizationLevel; // basic, standard, aggressive
        private Boolean enablePredictiveLoading;
        private Map<String, Object> customOptimizations;
    }

    public enum MergingStrategy {
        UNION,           // Combine all results from all regions
        INTERSECTION,    // Only results that appear in multiple regions
        WEIGHTED,        // Weight results based on region preference
        ROUND_ROBIN,     // Alternate results from different regions
        RELEVANCE_BASED, // Merge based on relevance scores
        DISTANCE_BASED   // Merge based on geographic proximity
    }

    public enum RoutingStrategy {
        NEAREST,     // Route to geographically nearest region
        FASTEST,     // Route to fastest responding region
        CHEAPEST,    // Route to most cost-effective region
        BALANCED,    // Balance between speed, cost, and quality
        WEIGHTED,    // Use predefined weights for region selection
        CUSTOM       // Use custom routing logic
    }
}