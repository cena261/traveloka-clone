package com.cena.traveloka.search.service;

import com.cena.traveloka.search.dto.RealTimeMetrics;
import com.cena.traveloka.search.dto.DashboardData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealTimeAnalyticsService {

    private final SimpMessagingTemplate messagingTemplate;
    private final SearchAnalyticsService searchAnalyticsService;

    // Real-time metrics storage (in production, this would be Redis or similar)
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, AtomicReference<BigDecimal>> gauges = new ConcurrentHashMap<>();
    private final Map<String, Queue<TimestampedValue>> timeSeriesData = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> activeUsers = new ConcurrentHashMap<>();

    // Real-time search metrics
    private final AtomicLong totalSearches = new AtomicLong(0);
    private final AtomicLong successfulSearches = new AtomicLong(0);
    private final AtomicLong failedSearches = new AtomicLong(0);
    private final AtomicLong zeroResultSearches = new AtomicLong(0);
    private final AtomicReference<BigDecimal> avgResponseTime = new AtomicReference<>(BigDecimal.ZERO);

    // Cache metrics
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    // User metrics
    private final Set<String> activeSearchSessions = ConcurrentHashMap.newKeySet();
    private final Map<String, OffsetDateTime> sessionStartTimes = new ConcurrentHashMap<>();

    public void recordSearchEvent(SearchEventData eventData) {
        log.debug("Recording real-time search event: type={}, userId={}",
                eventData.getEventType(), eventData.getUserId());

        try {
            // Update counters
            totalSearches.incrementAndGet();

            if (eventData.isSuccessful()) {
                successfulSearches.incrementAndGet();
            } else {
                failedSearches.incrementAndGet();
            }

            if (eventData.getResultCount() == 0) {
                zeroResultSearches.incrementAndGet();
            }

            // Update response time
            if (eventData.getResponseTime() != null) {
                updateAverageResponseTime(eventData.getResponseTime());
            }

            // Update cache metrics
            if (eventData.isCacheHit()) {
                cacheHits.incrementAndGet();
            } else {
                cacheMisses.incrementAndGet();
            }

            // Update active sessions
            if (eventData.getSessionId() != null) {
                activeSearchSessions.add(eventData.getSessionId());
                sessionStartTimes.putIfAbsent(eventData.getSessionId(), OffsetDateTime.now());
            }

            // Update time series data
            updateTimeSeriesData(eventData);

            // Update top queries
            updateTopQueries(eventData.getQuery());

            // Update geographic data
            updateGeographicData(eventData);

        } catch (Exception e) {
            log.error("Failed to record search event", e);
        }
    }

    public RealTimeMetrics getCurrentMetrics() {
        log.debug("Getting current real-time metrics");

        try {
            long total = totalSearches.get();
            long successful = successfulSearches.get();
            long failed = failedSearches.get();
            long zeroResults = zeroResultSearches.get();

            long cacheHitsCount = cacheHits.get();
            long cacheMissesCount = cacheMisses.get();
            long totalCacheRequests = cacheHitsCount + cacheMissesCount;

            return RealTimeMetrics.builder()
                .timestamp(OffsetDateTime.now())
                .totalSearches(total)
                .successfulSearches(successful)
                .failedSearches(failed)
                .zeroResultSearches(zeroResults)
                .successRate(total > 0 ? BigDecimal.valueOf((double) successful / total).setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .zeroResultRate(total > 0 ? BigDecimal.valueOf((double) zeroResults / total).setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .averageResponseTime(avgResponseTime.get())
                .cacheHitRate(totalCacheRequests > 0 ? BigDecimal.valueOf((double) cacheHitsCount / totalCacheRequests).setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .activeUsers(activeSearchSessions.size())
                .searchesPerMinute(calculateSearchesPerMinute())
                .topQueries(getTopQueries(10))
                .responseTimeP95(calculateP95ResponseTime())
                .geographicDistribution(getGeographicDistribution())
                .build();

        } catch (Exception e) {
            log.error("Failed to get current metrics", e);
            return createEmptyMetrics();
        }
    }

    public DashboardData getDashboardData(String timeRange) {
        log.info("Getting dashboard data for time range: {}", timeRange);

        try {
            var currentMetrics = getCurrentMetrics();
            var historicalData = getHistoricalData(timeRange);
            var alerts = getActiveAlerts();

            return DashboardData.builder()
                .realTimeMetrics(currentMetrics)
                .historicalData(historicalData)
                .alerts(alerts)
                .lastUpdated(OffsetDateTime.now())
                .timeRange(timeRange)
                .build();

        } catch (Exception e) {
            log.error("Failed to get dashboard data", e);
            return createEmptyDashboardData();
        }
    }

    @Scheduled(fixedRate = 10000) // Every 10 seconds
    public void broadcastRealTimeMetrics() {
        log.debug("Broadcasting real-time metrics to dashboard clients");

        try {
            var metrics = getCurrentMetrics();
            messagingTemplate.convertAndSend("/topic/search-metrics", metrics);

        } catch (Exception e) {
            log.error("Failed to broadcast real-time metrics", e);
        }
    }

    @Scheduled(fixedRate = 60000) // Every minute
    public void cleanupOldData() {
        log.debug("Cleaning up old real-time data");

        try {
            OffsetDateTime cutoff = OffsetDateTime.now().minusHours(24);

            // Clean up old time series data
            timeSeriesData.values().forEach(queue -> {
                queue.removeIf(value -> value.getTimestamp().isBefore(cutoff));
            });

            // Clean up inactive sessions
            sessionStartTimes.entrySet().removeIf(entry -> {
                if (entry.getValue().isBefore(cutoff)) {
                    activeSearchSessions.remove(entry.getKey());
                    return true;
                }
                return false;
            });

        } catch (Exception e) {
            log.error("Failed to cleanup old data", e);
        }
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void checkAlerts() {
        log.debug("Checking for alert conditions");

        try {
            var metrics = getCurrentMetrics();

            // Check success rate alert
            if (metrics.getSuccessRate().compareTo(BigDecimal.valueOf(0.95)) < 0) {
                triggerAlert("LOW_SUCCESS_RATE", "Search success rate below 95%: " + metrics.getSuccessRate());
            }

            // Check response time alert
            if (metrics.getAverageResponseTime().compareTo(BigDecimal.valueOf(5000)) > 0) {
                triggerAlert("HIGH_RESPONSE_TIME", "Average response time above 5s: " + metrics.getAverageResponseTime() + "ms");
            }

            // Check zero result rate alert
            if (metrics.getZeroResultRate().compareTo(BigDecimal.valueOf(0.15)) > 0) {
                triggerAlert("HIGH_ZERO_RESULT_RATE", "Zero result rate above 15%: " + metrics.getZeroResultRate());
            }

            // Check cache hit rate alert
            if (metrics.getCacheHitRate().compareTo(BigDecimal.valueOf(0.70)) < 0) {
                triggerAlert("LOW_CACHE_HIT_RATE", "Cache hit rate below 70%: " + metrics.getCacheHitRate());
            }

        } catch (Exception e) {
            log.error("Failed to check alerts", e);
        }
    }

    // Private helper methods

    private void updateAverageResponseTime(Long responseTime) {
        // Simple moving average (in production, use more sophisticated calculation)
        BigDecimal currentAvg = avgResponseTime.get();
        BigDecimal newValue = BigDecimal.valueOf(responseTime);
        BigDecimal updatedAvg = currentAvg.multiply(BigDecimal.valueOf(0.9))
                .add(newValue.multiply(BigDecimal.valueOf(0.1)));
        avgResponseTime.set(updatedAvg);
    }

    private void updateTimeSeriesData(SearchEventData eventData) {
        String key = "searches_per_minute";
        Queue<TimestampedValue> queue = timeSeriesData.computeIfAbsent(key, k -> new LinkedList<>());

        OffsetDateTime now = OffsetDateTime.now();
        queue.offer(new TimestampedValue(now, 1.0));

        // Keep only last hour of data
        OffsetDateTime cutoff = now.minusHours(1);
        queue.removeIf(value -> value.getTimestamp().isBefore(cutoff));
    }

    private void updateTopQueries(String query) {
        if (query == null || query.trim().isEmpty()) return;

        String key = "top_query_" + query.toLowerCase().trim();
        counters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }

    private void updateGeographicData(SearchEventData eventData) {
        if (eventData.getCountryCode() != null) {
            String key = "country_" + eventData.getCountryCode();
            counters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        }
    }

    private BigDecimal calculateSearchesPerMinute() {
        Queue<TimestampedValue> searchData = timeSeriesData.get("searches_per_minute");
        if (searchData == null || searchData.isEmpty()) {
            return BigDecimal.ZERO;
        }

        OffsetDateTime oneMinuteAgo = OffsetDateTime.now().minusMinutes(1);
        long recentSearches = searchData.stream()
                .filter(value -> value.getTimestamp().isAfter(oneMinuteAgo))
                .mapToLong(value -> value.getValue().longValue())
                .sum();

        return BigDecimal.valueOf(recentSearches);
    }

    private List<RealTimeMetrics.TopQuery> getTopQueries(int limit) {
        return counters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("top_query_"))
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
                .limit(limit)
                .map(entry -> {
                    String query = entry.getKey().substring("top_query_".length());
                    return RealTimeMetrics.TopQuery.builder()
                            .query(query)
                            .count(entry.getValue().get())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private BigDecimal calculateP95ResponseTime() {
        // Simplified calculation - in production, use proper percentile calculation
        return avgResponseTime.get().multiply(BigDecimal.valueOf(1.5));
    }

    private List<RealTimeMetrics.GeographicData> getGeographicDistribution() {
        return counters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("country_"))
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
                .limit(10)
                .map(entry -> {
                    String country = entry.getKey().substring("country_".length());
                    return RealTimeMetrics.GeographicData.builder()
                            .countryCode(country)
                            .searchCount(entry.getValue().get())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<DashboardData.HistoricalDataPoint> getHistoricalData(String timeRange) {
        // This would fetch historical data from the database
        // For now, return sample data
        List<DashboardData.HistoricalDataPoint> points = new ArrayList<>();

        OffsetDateTime now = OffsetDateTime.now();
        for (int i = 0; i < 24; i++) {
            points.add(DashboardData.HistoricalDataPoint.builder()
                    .timestamp(now.minusHours(24 - i))
                    .searches(1000L + (long) (Math.random() * 500))
                    .averageResponseTime(BigDecimal.valueOf(200 + Math.random() * 100))
                    .successRate(BigDecimal.valueOf(0.95 + Math.random() * 0.04))
                    .build());
        }

        return points;
    }

    private List<DashboardData.Alert> getActiveAlerts() {
        // This would get active alerts from storage
        return new ArrayList<>();
    }

    private void triggerAlert(String type, String message) {
        log.warn("ALERT TRIGGERED: {} - {}", type, message);

        DashboardData.Alert alert = DashboardData.Alert.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .severity("HIGH")
                .message(message)
                .timestamp(OffsetDateTime.now())
                .status("ACTIVE")
                .build();

        // Broadcast alert to dashboard
        messagingTemplate.convertAndSend("/topic/search-alerts", alert);
    }

    private RealTimeMetrics createEmptyMetrics() {
        return RealTimeMetrics.builder()
                .timestamp(OffsetDateTime.now())
                .totalSearches(0L)
                .successfulSearches(0L)
                .failedSearches(0L)
                .successRate(BigDecimal.ZERO)
                .averageResponseTime(BigDecimal.ZERO)
                .activeUsers(0)
                .build();
    }

    private DashboardData createEmptyDashboardData() {
        return DashboardData.builder()
                .realTimeMetrics(createEmptyMetrics())
                .historicalData(List.of())
                .alerts(List.of())
                .lastUpdated(OffsetDateTime.now())
                .build();
    }

    // Inner classes for data structures

    public static class SearchEventData {
        private String eventType;
        private String userId;
        private String sessionId;
        private String query;
        private boolean successful;
        private int resultCount;
        private Long responseTime;
        private boolean cacheHit;
        private String countryCode;
        private String deviceType;
        private OffsetDateTime timestamp;

        // Getters and setters
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public boolean isSuccessful() { return successful; }
        public void setSuccessful(boolean successful) { this.successful = successful; }
        public int getResultCount() { return resultCount; }
        public void setResultCount(int resultCount) { this.resultCount = resultCount; }
        public Long getResponseTime() { return responseTime; }
        public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }
        public boolean isCacheHit() { return cacheHit; }
        public void setCacheHit(boolean cacheHit) { this.cacheHit = cacheHit; }
        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
        public OffsetDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
    }

    private static class TimestampedValue {
        private final OffsetDateTime timestamp;
        private final Double value;

        public TimestampedValue(OffsetDateTime timestamp, Double value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        public OffsetDateTime getTimestamp() { return timestamp; }
        public Double getValue() { return value; }
    }
}