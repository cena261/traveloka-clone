package com.cena.traveloka.search.controller;

import com.cena.traveloka.search.service.SearchAnalyticsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/search/admin/analytics")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AnalyticsController {

    SearchAnalyticsService analyticsService;

    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics(
            @RequestParam OffsetDateTime startDate,
            @RequestParam OffsetDateTime endDate) {

        log.info("Performance metrics requested from {} to {}", startDate, endDate);

        try {
            // Validate date range
            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest().build();
            }

            // Limit to reasonable date ranges (e.g., 1 year max)
            if (startDate.isBefore(OffsetDateTime.now().minusYears(1))) {
                return ResponseEntity.badRequest().build();
            }

            Map<String, Object> metrics = analyticsService.getSearchPerformanceMetrics(startDate, endDate);
            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            log.error("Failed to get performance metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/popular-queries")
    public ResponseEntity<?> getPopularQueries(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "5") int minCount,
            @RequestParam(defaultValue = "50") int limit) {

        try {
            // Validate parameters
            if (days < 1 || days > 365) {
                return ResponseEntity.badRequest().build();
            }
            if (limit > 500) {
                return ResponseEntity.badRequest().build();
            }

            var queries = analyticsService.getPopularSearchQueries(days, minCount, limit);
            return ResponseEntity.ok(queries);

        } catch (Exception e) {
            log.error("Failed to get popular queries", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/trending-queries")
    public ResponseEntity<?> getTrendingQueries(
            @RequestParam(defaultValue = "7") int recentDays,
            @RequestParam(defaultValue = "30") int historicalDays,
            @RequestParam(defaultValue = "3") int minRecentCount,
            @RequestParam(defaultValue = "20") int limit) {

        try {
            // Validate parameters
            if (recentDays < 1 || historicalDays < 1) {
                return ResponseEntity.badRequest().build();
            }
            if (limit > 200) {
                return ResponseEntity.badRequest().build();
            }

            var queries = analyticsService.getTrendingSearchQueries(
                recentDays, historicalDays, minRecentCount, limit);
            return ResponseEntity.ok(queries);

        } catch (Exception e) {
            log.error("Failed to get trending queries", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserSearchPatterns(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "30") int days) {

        log.info("User search patterns requested for user: {} over {} days", userId, days);

        try {
            // Validate parameters
            if (days < 1 || days > 365) {
                return ResponseEntity.badRequest().build();
            }

            Map<String, Object> patterns = analyticsService.getUserSearchPatterns(userId, days);
            return ResponseEntity.ok(patterns);

        } catch (Exception e) {
            log.error("Failed to get user search patterns for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSessionAnalytics(@PathVariable UUID sessionId) {
        log.info("Session analytics requested for session: {}", sessionId);

        try {
            var analytics = analyticsService.getSessionAnalytics(sessionId);
            return ResponseEntity.ok(analytics);

        } catch (Exception e) {
            log.error("Failed to get session analytics for session: {}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/location")
    public ResponseEntity<Map<String, Object>> getLocationBasedAnalytics(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radiusKm) {

        log.info("Location analytics requested for lat={}, lng={}, radius={}km",
                latitude, longitude, radiusKm);

        try {
            // Validate coordinates
            if (latitude < -90 || latitude > 90) {
                return ResponseEntity.badRequest().build();
            }
            if (longitude < -180 || longitude > 180) {
                return ResponseEntity.badRequest().build();
            }
            if (radiusKm <= 0 || radiusKm > 100) {
                return ResponseEntity.badRequest().build();
            }

            Map<String, Object> analytics = analyticsService.getLocationBasedAnalytics(
                latitude, longitude, radiusKm);
            return ResponseEntity.ok(analytics);

        } catch (Exception e) {
            log.error("Failed to get location-based analytics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/destinations")
    public ResponseEntity<Map<String, Object>> getDestinationAnalytics(
            @RequestParam(defaultValue = "30") int days) {

        try {
            // Validate parameters
            if (days < 1 || days > 365) {
                return ResponseEntity.badRequest().build();
            }

            Map<String, Object> analytics = analyticsService.getDestinationAnalytics(days);
            return ResponseEntity.ok(analytics);

        } catch (Exception e) {
            log.error("Failed to get destination analytics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/destinations/update-trends")
    public ResponseEntity<Void> updateDestinationTrends() {
        log.info("Request to update destination trending scores");

        try {
            analyticsService.updateDestinationTrendingScores();
            return ResponseEntity.accepted().build();

        } catch (Exception e) {
            log.error("Failed to update destination trends", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/cleanup")
    public ResponseEntity<Void> cleanupOldAnalytics(
            @RequestParam(defaultValue = "365") int retentionDays) {

        log.info("Request to cleanup analytics data older than {} days", retentionDays);

        try {
            // Validate retention period (minimum 30 days)
            if (retentionDays < 30) {
                return ResponseEntity.badRequest().build();
            }

            analyticsService.cleanupOldAnalytics(retentionDays);
            return ResponseEntity.accepted().build();

        } catch (Exception e) {
            log.error("Failed to cleanup old analytics data", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getAnalyticsHealth() {
        try {
            // Basic health check for analytics service
            Map<String, Object> health = Map.of(
                "service", "SearchAnalyticsService",
                "status", "active",
                "timestamp", OffsetDateTime.now(),
                "version", "1.0"
            );

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            log.error("Failed to get analytics health status", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}