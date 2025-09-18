package com.cena.traveloka.search.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@Jacksonized
public class RealTimeMetrics {

    private OffsetDateTime timestamp;

    // Core search metrics
    private Long totalSearches;
    private Long successfulSearches;
    private Long failedSearches;
    private Long zeroResultSearches;
    private BigDecimal successRate;
    private BigDecimal zeroResultRate;

    // Performance metrics
    private BigDecimal averageResponseTime;
    private BigDecimal responseTimeP50;
    private BigDecimal responseTimeP95;
    private BigDecimal responseTimeP99;

    // Cache metrics
    private BigDecimal cacheHitRate;
    private Long cacheHits;
    private Long cacheMisses;

    // User metrics
    private Integer activeUsers;
    private Integer activeSessions;
    private BigDecimal searchesPerMinute;
    private BigDecimal searchesPerSecond;

    // Content metrics
    private List<TopQuery> topQueries;
    private List<PopularFilter> popularFilters;
    private List<GeographicData> geographicDistribution;

    // System metrics
    private BigDecimal cpuUsage;
    private BigDecimal memoryUsage;
    private Integer activeConnections;

    @Data
    @Builder
    @Jacksonized
    public static class TopQuery {
        private String query;
        private Long count;
        private BigDecimal percentage;
        private String trend; // up, down, stable
        private Integer rank;
        private Integer previousRank;
    }

    @Data
    @Builder
    @Jacksonized
    public static class PopularFilter {
        private String filterType;
        private String filterValue;
        private Long usageCount;
        private BigDecimal usagePercentage;
        private String category;
    }

    @Data
    @Builder
    @Jacksonized
    public static class GeographicData {
        private String countryCode;
        private String countryName;
        private Long searchCount;
        private BigDecimal percentage;
        private Integer rank;
        private BigDecimal avgResponseTime;
    }
}