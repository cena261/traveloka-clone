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
public class DashboardData {

    private RealTimeMetrics realTimeMetrics;
    private List<HistoricalDataPoint> historicalData;
    private List<Alert> alerts;
    private List<Widget> widgets;
    private OffsetDateTime lastUpdated;
    private String timeRange;
    private Map<String, Object> configuration;

    @Data
    @Builder
    @Jacksonized
    public static class HistoricalDataPoint {
        private OffsetDateTime timestamp;
        private Long searches;
        private BigDecimal averageResponseTime;
        private BigDecimal successRate;
        private BigDecimal cacheHitRate;
        private Integer activeUsers;
        private Long zeroResultSearches;
        private Map<String, Object> additionalMetrics;
    }

    @Data
    @Builder
    @Jacksonized
    public static class Alert {
        private String id;
        private String type;
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        private String message;
        private String description;
        private OffsetDateTime timestamp;
        private OffsetDateTime resolvedAt;
        private String status; // ACTIVE, ACKNOWLEDGED, RESOLVED
        private String assignedTo;
        private List<String> tags;
        private Map<String, Object> metadata;
        private String actionUrl;
        private Boolean requiresAction;
    }

    @Data
    @Builder
    @Jacksonized
    public static class Widget {
        private String id;
        private String type; // chart, metric, table, gauge
        private String title;
        private String description;
        private Integer position;
        private Integer width;
        private Integer height;
        private WidgetConfiguration configuration;
        private Object data;
        private OffsetDateTime lastUpdated;
    }

    @Data
    @Builder
    @Jacksonized
    public static class WidgetConfiguration {
        private String chartType; // line, bar, pie, gauge
        private String timeRange;
        private List<String> metrics;
        private Map<String, String> colors;
        private Boolean showLegend;
        private Boolean showTooltips;
        private String refreshInterval;
        private Map<String, Object> customSettings;
    }

    // Predefined dashboard layouts
    public enum DashboardType {
        EXECUTIVE_SUMMARY,
        TECHNICAL_OPERATIONS,
        BUSINESS_METRICS,
        USER_EXPERIENCE,
        PERFORMANCE_MONITORING,
        CUSTOM
    }

    // Alert severity levels
    public enum AlertSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    // Widget types
    public enum WidgetType {
        LINE_CHART,
        BAR_CHART,
        PIE_CHART,
        GAUGE_CHART,
        METRIC_CARD,
        DATA_TABLE,
        HEAT_MAP,
        GEOGRAPHIC_MAP,
        REAL_TIME_LOG,
        ALERT_PANEL
    }
}