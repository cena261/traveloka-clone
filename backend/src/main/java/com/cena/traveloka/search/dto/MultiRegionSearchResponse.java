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
public class MultiRegionSearchResponse {

    private List<PropertySearchResult> results;
    private RegionPerformanceMetrics performanceMetrics;
    private ResultsMergingInfo mergingInfo;
    private List<RegionResult> regionResults;
    private ComplianceInfo complianceInfo;
    private CachingInfo cachingInfo;
    private PaginationInfo pagination;
    private Map<String, Object> metadata;

    @Data
    @Builder
    @Jacksonized
    public static class RegionPerformanceMetrics {
        private String primaryRegion;
        private Integer totalRegionsQueried;
        private Integer successfulRegions;
        private Integer failedRegions;
        private Long totalResponseTime;
        private Long fastestRegionTime;
        private Long slowestRegionTime;
        private BigDecimal averageResponseTime;
        private List<RegionPerformance> regionPerformances;
        private String overallHealthStatus;
        private Map<String, Object> performanceDetails;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RegionPerformance {
        private String regionId;
        private String regionName;
        private String status; // success, failure, timeout, degraded
        private Long responseTime;
        private Integer resultCount;
        private BigDecimal relevanceScore;
        private Boolean cacheHit;
        private String errorMessage;
        private Map<String, Object> regionSpecificMetrics;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ResultsMergingInfo {
        private String mergingStrategy;
        private Integer totalResultsBeforeMerging;
        private Integer totalResultsAfterMerging;
        private Integer duplicatesRemoved;
        private String deduplicationStrategy;
        private List<MergingStep> mergingSteps;
        private Map<String, Integer> resultsByRegion;
        private BigDecimal mergingConfidence;
        private String qualityScore;
    }

    @Data
    @Builder
    @Jacksonized
    public static class MergingStep {
        private String stepName;
        private String stepType;
        private Integer inputCount;
        private Integer outputCount;
        private Long processingTime;
        private String description;
        private Map<String, Object> stepDetails;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RegionResult {
        private String regionId;
        private String regionName;
        private Integer resultCount;
        private List<PropertySearchResult> topResults;
        private String searchQuality;
        private Boolean wasUsedInFinalResults;
        private BigDecimal contributionPercentage;
        private Map<String, Object> regionMetadata;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ComplianceInfo {
        private List<String> appliedRegulations;
        private Boolean gdprCompliant;
        private Boolean ccpaCompliant;
        private List<String> dataProcessingRestrictions;
        private String privacyLevel;
        private List<String> consentRequirements;
        private Map<String, String> regionalCompliance;
        private List<String> disclaimers;
        private String dataRetentionPolicy;
    }

    @Data
    @Builder
    @Jacksonized
    public static class CachingInfo {
        private Boolean cacheHit;
        private String cacheStrategy;
        private List<RegionCacheInfo> regionCacheInfo;
        private OffsetDateTime cacheExpiry;
        private String cacheKey;
        private BigDecimal cacheEfficiency;
        private Integer cacheMisses;
        private String cachingRecommendation;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RegionCacheInfo {
        private String regionId;
        private Boolean cacheHit;
        private String cacheType; // local, distributed, memory
        private OffsetDateTime lastUpdated;
        private Integer hitCount;
        private BigDecimal hitRate;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RoutingInfo {
        private String routingStrategy;
        private String selectedRegion;
        private List<String> candidateRegions;
        private List<String> fallbackRegions;
        private Map<String, BigDecimal> regionScores;
        private String routingReason;
        private Boolean usedFallback;
        private List<RoutingDecision> routingDecisions;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RoutingDecision {
        private String regionId;
        private String decision; // selected, rejected, fallback
        private String reason;
        private BigDecimal score;
        private Long latency;
        private String healthStatus;
        private Map<String, Object> decisionFactors;
    }

    // Enhanced property result with multi-region context
    @Data
    @Builder
    @Jacksonized
    public static class MultiRegionPropertyResult extends PropertySearchResult {
        private String sourceRegion;
        private List<String> availableInRegions;
        private Map<String, BigDecimal> regionalPrices;
        private Map<String, String> regionalCurrencies;
        private BigDecimal crossRegionScore;
        private Boolean isGlobalProperty;
        private String regionRanking;
        private Map<String, Object> regionalMetadata;
    }

    // Search quality assessment
    @Data
    @Builder
    @Jacksonized
    public static class SearchQualityMetrics {
        private BigDecimal overallQuality;
        private BigDecimal relevanceScore;
        private BigDecimal diversityScore;
        private BigDecimal freshnessScore;
        private BigDecimal coverageScore;
        private Map<String, BigDecimal> qualityByRegion;
        private List<String> qualityFactors;
        private String qualityGrade; // A, B, C, D, F
        private List<String> improvementSuggestions;
    }

    // Global search insights
    @Data
    @Builder
    @Jacksonized
    public static class GlobalSearchInsights {
        private String marketTrends;
        private List<String> popularDestinations;
        private Map<String, BigDecimal> priceComparisons;
        private List<String> regionalRecommendations;
        private String seasonalAdjustments;
        private Map<String, Object> businessIntelligence;
    }

    public enum SearchStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        FAILURE,
        TIMEOUT,
        DEGRADED_PERFORMANCE,
        MAINTENANCE_MODE
    }

    public enum QualityGrade {
        EXCELLENT("A"),
        GOOD("B"),
        FAIR("C"),
        POOR("D"),
        FAILING("F");

        private final String grade;

        QualityGrade(String grade) {
            this.grade = grade;
        }

        public String getGrade() {
            return grade;
        }
    }
}