package com.cena.traveloka.search.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@Jacksonized
public class RegionConfig {

    private String regionId;
    private String regionName;
    private String regionCode;
    private List<String> countryCodes;
    private List<String> timeZones;
    private String primaryLanguage;
    private List<String> supportedLanguages;
    private String primaryCurrency;
    private List<String> supportedCurrencies;

    // Geographic boundaries
    private GeographicBounds bounds;

    // Search engine configuration
    private SearchEngineConfig searchConfig;

    // Caching configuration
    private CacheConfig cacheConfig;

    // Performance tuning
    private PerformanceConfig performanceConfig;

    // Business rules
    private BusinessRuleConfig businessRules;

    // Regulatory compliance
    private ComplianceConfig compliance;

    private Boolean isActive;
    private OffsetDateTime lastUpdated;
    private Map<String, Object> metadata;

    @Data
    @Builder
    @Jacksonized
    public static class GeographicBounds {
        private BigDecimal northLat;
        private BigDecimal southLat;
        private BigDecimal eastLng;
        private BigDecimal westLng;
        private String coordinateSystem; // WGS84, etc.
        private Integer zoomLevel;
    }

    @Data
    @Builder
    @Jacksonized
    public static class SearchEngineConfig {
        private String primarySearchCluster;
        private List<String> fallbackClusters;
        private String indexPrefix;
        private Map<String, String> indexMapping;
        private Integer maxResultsPerPage;
        private Integer defaultPageSize;
        private Long queryTimeout;
        private Boolean enableFacets;
        private Boolean enablePersonalization;
        private List<String> enabledFeatures;
        private Map<String, Object> customSettings;
    }

    @Data
    @Builder
    @Jacksonized
    public static class CacheConfig {
        private String cacheCluster;
        private List<String> fallbackCaches;
        private Integer defaultTtlSeconds;
        private Integer searchResultsTtl;
        private Integer suggestionsTtl;
        private Integer facetsTtl;
        private Integer userProfilesTtl;
        private String evictionPolicy;
        private Integer maxCacheSize;
        private Boolean enableDistributedCache;
        private Map<String, Integer> customTtls;
    }

    @Data
    @Builder
    @Jacksonized
    public static class PerformanceConfig {
        private Integer maxConcurrentRequests;
        private Integer circuitBreakerThreshold;
        private Long circuitBreakerTimeout;
        private Integer retryAttempts;
        private Long retryDelay;
        private BigDecimal loadBalancingWeight;
        private String routingStrategy; // nearest, round_robin, weighted
        private Integer healthCheckInterval;
        private Map<String, Object> optimizations;
    }

    @Data
    @Builder
    @Jacksonized
    public static class BusinessRuleConfig {
        private List<String> preferredPartners;
        private List<String> excludedPartners;
        private Map<String, BigDecimal> partnerCommissions;
        private List<String> featuredProperties;
        private String inventoryPriority; // availability, price, rating
        private Boolean enableDynamicPricing;
        private Boolean enablePromotions;
        private List<String> localPaymentMethods;
        private Map<String, Object> regionSpecificRules;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ComplianceConfig {
        private Boolean gdprCompliant;
        private Boolean ccpaCompliant;
        private List<String> dataRetentionPolicies;
        private List<String> prohibitedContent;
        private List<String> requiredDisclosures;
        private String privacyPolicyUrl;
        private String termsOfServiceUrl;
        private Boolean requiresUserConsent;
        private List<String> sensitiveDataCategories;
        private Map<String, String> localRegulations;
    }

    public enum RegionStatus {
        ACTIVE,
        INACTIVE,
        MAINTENANCE,
        DEGRADED,
        EMERGENCY_ONLY
    }

    public enum RoutingStrategy {
        NEAREST_REGION,
        ROUND_ROBIN,
        WEIGHTED_RANDOM,
        LEAST_CONNECTIONS,
        RESPONSE_TIME_BASED,
        CUSTOM
    }
}