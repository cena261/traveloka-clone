package com.cena.traveloka.search.service;

import com.cena.traveloka.search.dto.SearchAnalyticsRequest;
import com.cena.traveloka.search.entity.SearchHistory;
import com.cena.traveloka.search.repository.PopularDestinationRepository;
import com.cena.traveloka.search.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchAnalyticsService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final PopularDestinationRepository popularDestinationRepository;

    @Async
    @Transactional
    public void recordSearchAnalytics(SearchAnalyticsRequest request) {
        log.info("Recording search analytics for query: '{}' by session: {}",
                request.getSearchQuery(), request.getSessionId());

        try {
            SearchHistory searchHistory = convertToSearchHistory(request);
            searchHistoryRepository.save(searchHistory);

            log.info("Successfully recorded search analytics for session: {}", request.getSessionId());

        } catch (Exception e) {
            log.error("Failed to record search analytics for session: {}", request.getSessionId(), e);
            // Don't throw exception to avoid impacting main search flow
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSearchPerformanceMetrics(OffsetDateTime startDate, OffsetDateTime endDate) {
        log.info("Getting search performance metrics from {} to {}", startDate, endDate);

        try {
            Map<String, Object> metrics = new HashMap<>();

            // Basic metrics
            long totalSearches = searchHistoryRepository.countSearchesByUserInDateRange(null, startDate, endDate);
            metrics.put("totalSearches", totalSearches);

            // Conversion rate
            BigDecimal conversionRate = searchHistoryRepository.calculateConversionRate(startDate, endDate);
            metrics.put("conversionRate", conversionRate);

            // Zero results rate
            long zeroResultSearches = searchHistoryRepository.findZeroResultSearches(null).getTotalElements();
            double zeroResultsRate = totalSearches > 0 ? (double) zeroResultSearches / totalSearches : 0.0;
            metrics.put("zeroResultsRate", zeroResultsRate);

            // Response time analytics
            List<Object[]> responseTimeData = searchHistoryRepository.getResponseTimeAnalytics(startDate);
            metrics.put("responseTimeBySearchType", responseTimeData);

            // Device analytics
            List<Object[]> deviceData = searchHistoryRepository.getSearchAnalyticsByDeviceType(startDate);
            metrics.put("deviceAnalytics", deviceData);

            // Search frequency by hour
            List<Object[]> hourlyData = searchHistoryRepository.getSearchFrequencyByHour(startDate);
            metrics.put("searchFrequencyByHour", hourlyData);

            log.info("Retrieved search performance metrics for {} searches", totalSearches);
            return metrics;

        } catch (Exception e) {
            log.error("Failed to get search performance metrics", e);
            return Map.of("error", "Failed to retrieve metrics");
        }
    }

    @Transactional(readOnly = true)
    public List<Object[]> getPopularSearchQueries(int days, int minCount, int limit) {
        log.info("Getting popular search queries for last {} days", days);

        try {
            OffsetDateTime since = OffsetDateTime.now().minusDays(days);
            return searchHistoryRepository.findPopularSearchQueries(since, minCount, limit);

        } catch (Exception e) {
            log.error("Failed to get popular search queries", e);
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public List<Object[]> getTrendingSearchQueries(int recentDays, int historicalDays, int minRecentCount, int limit) {
        log.info("Getting trending search queries comparing last {} days to previous {} days",
                recentDays, historicalDays);

        try {
            OffsetDateTime recentPeriodStart = OffsetDateTime.now().minusDays(recentDays);
            OffsetDateTime historicalPeriodEnd = recentPeriodStart;
            OffsetDateTime historicalPeriodStart = historicalPeriodEnd.minusDays(historicalDays);

            return searchHistoryRepository.findTrendingSearchQueries(
                recentPeriodStart, historicalPeriodStart, historicalPeriodEnd, minRecentCount, limit
            );

        } catch (Exception e) {
            log.error("Failed to get trending search queries", e);
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserSearchPatterns(UUID userId, int days) {
        log.info("Analyzing search patterns for user: {} over {} days", userId, days);

        try {
            Map<String, Object> patterns = new HashMap<>();

            OffsetDateTime since = OffsetDateTime.now().minusDays(days);

            // Total searches
            long totalSearches = searchHistoryRepository.countSearchesByUserInDateRange(userId, since, OffsetDateTime.now());
            patterns.put("totalSearches", totalSearches);

            // User searches with location data
            List<SearchHistory> locationSearches = searchHistoryRepository.findLocationBasedSearchesNearby(
                21.0285, 105.8542, 100000.0 // Hanoi area as example
            ).stream()
            .filter(sh -> userId.equals(sh.getUserId()))
            .toList();

            patterns.put("locationBasedSearches", locationSearches.size());

            // Average searches per day
            double avgSearchesPerDay = days > 0 ? (double) totalSearches / days : 0;
            patterns.put("averageSearchesPerDay", avgSearchesPerDay);

            return patterns;

        } catch (Exception e) {
            log.error("Failed to analyze user search patterns for user: {}", userId, e);
            return Map.of("error", "Failed to analyze patterns");
        }
    }

    @Transactional(readOnly = true)
    public List<Object[]> getSessionAnalytics(UUID sessionId) {
        log.info("Getting session analytics for session: {}", sessionId);

        try {
            return searchHistoryRepository.getSessionAnalytics(sessionId);

        } catch (Exception e) {
            log.error("Failed to get session analytics for session: {}", sessionId, e);
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLocationBasedAnalytics(Double latitude, Double longitude, Double radiusKm) {
        log.info("Getting location-based analytics for lat: {}, lng: {}, radius: {}km",
                latitude, longitude, radiusKm);

        try {
            Map<String, Object> analytics = new HashMap<>();

            // Get searches in the area
            List<SearchHistory> nearbySearches = searchHistoryRepository.findLocationBasedSearchesNearby(
                latitude, longitude, radiusKm * 1000 // Convert to meters
            );

            analytics.put("totalSearchesInArea", nearbySearches.size());

            // Calculate conversion rate in area
            long conversions = nearbySearches.stream()
                .mapToLong(sh -> sh.getBookingCompleted() ? 1 : 0)
                .sum();

            double conversionRate = !nearbySearches.isEmpty() ? (double) conversions / nearbySearches.size() : 0.0;
            analytics.put("conversionRateInArea", conversionRate);

            // Get popular destinations near location
            var nearbyDestinations = popularDestinationRepository.findDestinationsNearLocation(
                latitude, longitude, radiusKm * 1000
            );

            analytics.put("popularDestinationsNearby", nearbyDestinations.size());

            return analytics;

        } catch (Exception e) {
            log.error("Failed to get location-based analytics", e);
            return Map.of("error", "Failed to retrieve location analytics");
        }
    }

    @Transactional
    public void updateDestinationTrendingScores() {
        log.info("Updating destination trending scores");

        try {
            popularDestinationRepository.updateTrendingScores(OffsetDateTime.now());
            log.info("Successfully updated destination trending scores");

        } catch (Exception e) {
            log.error("Failed to update destination trending scores", e);
        }
    }

    @Transactional
    public void cleanupOldAnalytics(int retentionDays) {
        log.info("Cleaning up search analytics older than {} days", retentionDays);

        try {
            OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(retentionDays);

            // Clean up old search history
            searchHistoryRepository.deleteOldSearchHistory(cutoffDate);

            // Clean up old destination data
            popularDestinationRepository.deleteOutdatedDestinations(cutoffDate);

            log.info("Successfully cleaned up old analytics data before {}", cutoffDate);

        } catch (Exception e) {
            log.error("Failed to cleanup old analytics data", e);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDestinationAnalytics(int days) {
        log.info("Getting destination analytics for last {} days", days);

        try {
            OffsetDateTime since = OffsetDateTime.now().minusDays(days);

            List<Object[]> destinationSummary = popularDestinationRepository.getDestinationAnalyticsSummary(since);

            Map<String, Object> analytics = new HashMap<>();
            analytics.put("destinationSummary", destinationSummary);

            // Get trending destinations
            var trendingDestinations = popularDestinationRepository.findTrendingDestinations(
                BigDecimal.valueOf(70.0), null
            );

            analytics.put("trendingDestinations", trendingDestinations.getContent());

            return analytics;

        } catch (Exception e) {
            log.error("Failed to get destination analytics", e);
            return Map.of("error", "Failed to retrieve destination analytics");
        }
    }

    private SearchHistory convertToSearchHistory(SearchAnalyticsRequest request) {
        SearchHistory.Builder builder = SearchHistory.builder()
            .sessionId(request.getSessionId())
            .userId(request.getUserId())
            .searchQuery(request.getSearchQuery())
            .searchType(request.getSearchType())
            .filters(request.getFilters())
            .language(request.getDeviceContext() != null ? request.getDeviceContext().getLanguage() : "vi")
            .searchTimestamp(request.getSearchTimestamp());

        // Add location context if available
        if (request.getLocationContext() != null) {
            SearchAnalyticsRequest.LocationContext location = request.getLocationContext();
            if (location.getLatitude() != null && location.getLongitude() != null) {
                // Note: In real implementation, would convert to PostGIS Point
                builder.searchRadius(location.getSearchRadiusKm())
                       .detectedLocation(location.getDetectedLocation());
            }
        }

        // Add search results info if available
        if (request.getSearchResultsInfo() != null) {
            SearchAnalyticsRequest.SearchResultsInfo resultsInfo = request.getSearchResultsInfo();
            builder.totalResults(resultsInfo.getTotalResults())
                   .responseTimeMs(resultsInfo.getResponseTimeMs());
        }

        // Add user interaction info if available
        if (request.getUserInteractionInfo() != null) {
            SearchAnalyticsRequest.UserInteractionInfo interactions = request.getUserInteractionInfo();
            builder.bookingCompleted(interactions.getBookingCompleted())
                   .conversionValue(interactions.getConversionValue());

            // Convert clicked property IDs
            if (interactions.getClickedPropertyIds() != null) {
                UUID[] clickedIds = interactions.getClickedPropertyIds().toArray(new UUID[0]);
                builder.clickedPropertyIds(clickedIds);
            }
        }

        // Add device context if available
        if (request.getDeviceContext() != null) {
            SearchAnalyticsRequest.DeviceContext device = request.getDeviceContext();
            if (device.getDeviceType() != null) {
                builder.deviceType(SearchHistory.DeviceType.valueOf(device.getDeviceType().name()));
            }
            builder.userAgent(device.getUserAgent());
        }

        return builder.build();
    }
}
