package com.cena.traveloka.search.controller;

import com.cena.traveloka.search.dto.DashboardData;
import com.cena.traveloka.search.dto.RealTimeMetrics;
import com.cena.traveloka.search.service.RealTimeAnalyticsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search/dashboard")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardController {

    RealTimeAnalyticsService realTimeAnalyticsService;

    @GetMapping("/metrics/realtime")
    public ResponseEntity<RealTimeMetrics> getRealTimeMetrics() {
        log.info("Getting real-time search metrics");

        try {
            RealTimeMetrics metrics = realTimeAnalyticsService.getCurrentMetrics();
            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            log.error("Failed to get real-time metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/data")
    public ResponseEntity<DashboardData> getDashboardData(
            @RequestParam(defaultValue = "24h") String timeRange,
            @RequestParam(defaultValue = "EXECUTIVE_SUMMARY") String dashboardType) {

        log.info("Getting dashboard data for time range: {}, type: {}", timeRange, dashboardType);

        try {
            DashboardData dashboardData = realTimeAnalyticsService.getDashboardData(timeRange);
            return ResponseEntity.ok(dashboardData);

        } catch (Exception e) {
            log.error("Failed to get dashboard data", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/widgets/{widgetType}")
    public ResponseEntity<Map<String, Object>> getWidgetData(
            @PathVariable String widgetType,
            @RequestParam(defaultValue = "1h") String timeRange,
            @RequestParam(required = false) Map<String, String> parameters) {

        log.info("Getting widget data for type: {}, timeRange: {}", widgetType, timeRange);

        try {
            Map<String, Object> widgetData = getWidgetDataByType(widgetType, timeRange, parameters);
            return ResponseEntity.ok(widgetData);

        } catch (Exception e) {
            log.error("Failed to get widget data for type: {}", widgetType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/alerts/{alertId}/acknowledge")
    public ResponseEntity<Void> acknowledgeAlert(
            @PathVariable String alertId,
            @RequestParam String userId) {

        log.info("Acknowledging alert: {} by user: {}", alertId, userId);

        try {
            // Would implement alert acknowledgment logic
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to acknowledge alert: {}", alertId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<Void> resolveAlert(
            @PathVariable String alertId,
            @RequestParam String userId,
            @RequestParam(required = false) String resolution) {

        log.info("Resolving alert: {} by user: {}", alertId, userId);

        try {
            // Would implement alert resolution logic
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to resolve alert: {}", alertId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/exports/csv")
    public ResponseEntity<String> exportMetricsCsv(
            @RequestParam String timeRange,
            @RequestParam(required = false) String metrics) {

        log.info("Exporting metrics to CSV for time range: {}", timeRange);

        try {
            String csvData = generateMetricsCsv(timeRange, metrics);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/csv")
                    .header("Content-Disposition", "attachment; filename=search_metrics.csv")
                    .body(csvData);

        } catch (Exception e) {
            log.error("Failed to export metrics to CSV", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/dashboard/customize")
    public ResponseEntity<DashboardData> customizeDashboard(
            @RequestBody DashboardCustomizationRequest request) {

        log.info("Customizing dashboard for user: {}", request.getUserId());

        try {
            DashboardData customizedDashboard = createCustomDashboard(request);
            return ResponseEntity.ok(customizedDashboard);

        } catch (Exception e) {
            log.error("Failed to customize dashboard", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // WebSocket endpoints for real-time updates
    @MessageMapping("/dashboard/subscribe")
    @SendTo("/topic/search-metrics")
    public RealTimeMetrics subscribeToMetrics() {
        log.info("Client subscribed to real-time metrics");
        return realTimeAnalyticsService.getCurrentMetrics();
    }

    // Private helper methods

    private Map<String, Object> getWidgetDataByType(String widgetType, String timeRange, Map<String, String> parameters) {
        return switch (widgetType.toLowerCase()) {
            case "search_volume" -> getSearchVolumeData(timeRange);
            case "response_time" -> getResponseTimeData(timeRange);
            case "success_rate" -> getSuccessRateData(timeRange);
            case "top_queries" -> getTopQueriesData(timeRange);
            case "geographic_distribution" -> getGeographicData(timeRange);
            case "cache_performance" -> getCachePerformanceData(timeRange);
            case "alert_summary" -> getAlertSummaryData(timeRange);
            case "user_activity" -> getUserActivityData(timeRange);
            default -> Map.of("error", "Unknown widget type: " + widgetType);
        };
    }

    private Map<String, Object> getSearchVolumeData(String timeRange) {
        var metrics = realTimeAnalyticsService.getCurrentMetrics();
        return Map.of(
                "totalSearches", metrics.getTotalSearches(),
                "searchesPerMinute", metrics.getSearchesPerMinute(),
                "trend", "up",
                "changePercent", 5.2
        );
    }

    private Map<String, Object> getResponseTimeData(String timeRange) {
        var metrics = realTimeAnalyticsService.getCurrentMetrics();
        return Map.of(
                "averageResponseTime", metrics.getAverageResponseTime(),
                "p95ResponseTime", metrics.getResponseTimeP95(),
                "trend", "stable",
                "changePercent", -2.1
        );
    }

    private Map<String, Object> getSuccessRateData(String timeRange) {
        var metrics = realTimeAnalyticsService.getCurrentMetrics();
        return Map.of(
                "successRate", metrics.getSuccessRate(),
                "failedSearches", metrics.getFailedSearches(),
                "trend", "up",
                "changePercent", 1.5
        );
    }

    private Map<String, Object> getTopQueriesData(String timeRange) {
        var metrics = realTimeAnalyticsService.getCurrentMetrics();
        return Map.of(
                "queries", metrics.getTopQueries(),
                "totalUniqueQueries", metrics.getTopQueries().size(),
                "timeRange", timeRange
        );
    }

    private Map<String, Object> getGeographicData(String timeRange) {
        var metrics = realTimeAnalyticsService.getCurrentMetrics();
        return Map.of(
                "distribution", metrics.getGeographicDistribution(),
                "totalCountries", metrics.getGeographicDistribution().size(),
                "timeRange", timeRange
        );
    }

    private Map<String, Object> getCachePerformanceData(String timeRange) {
        var metrics = realTimeAnalyticsService.getCurrentMetrics();
        return Map.of(
                "hitRate", metrics.getCacheHitRate(),
                "hits", metrics.getCacheHits(),
                "misses", metrics.getCacheMisses(),
                "trend", "stable"
        );
    }

    private Map<String, Object> getAlertSummaryData(String timeRange) {
        return Map.of(
                "activeAlerts", 2,
                "resolvedAlerts", 15,
                "criticalAlerts", 0,
                "timeRange", timeRange
        );
    }

    private Map<String, Object> getUserActivityData(String timeRange) {
        var metrics = realTimeAnalyticsService.getCurrentMetrics();
        return Map.of(
                "activeUsers", metrics.getActiveUsers(),
                "activeSessions", metrics.getActiveSessions(),
                "timeRange", timeRange
        );
    }

    private String generateMetricsCsv(String timeRange, String metrics) {
        // Would generate CSV data from metrics
        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,Total Searches,Success Rate,Average Response Time,Active Users\n");

        var currentMetrics = realTimeAnalyticsService.getCurrentMetrics();
        csv.append(String.format("%s,%d,%s,%s,%d\n",
                currentMetrics.getTimestamp(),
                currentMetrics.getTotalSearches(),
                currentMetrics.getSuccessRate(),
                currentMetrics.getAverageResponseTime(),
                currentMetrics.getActiveUsers()));

        return csv.toString();
    }

    private DashboardData createCustomDashboard(DashboardCustomizationRequest request) {
        // Would create a customized dashboard based on user preferences
        return realTimeAnalyticsService.getDashboardData(request.getTimeRange());
    }

    // Inner classes for requests
    public static class DashboardCustomizationRequest {
        private String userId;
        private String timeRange;
        private String dashboardType;
        private Map<String, Object> widgetConfiguration;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getTimeRange() { return timeRange; }
        public void setTimeRange(String timeRange) { this.timeRange = timeRange; }
        public String getDashboardType() { return dashboardType; }
        public void setDashboardType(String dashboardType) { this.dashboardType = dashboardType; }
        public Map<String, Object> getWidgetConfiguration() { return widgetConfiguration; }
        public void setWidgetConfiguration(Map<String, Object> widgetConfiguration) { this.widgetConfiguration = widgetConfiguration; }
    }
}