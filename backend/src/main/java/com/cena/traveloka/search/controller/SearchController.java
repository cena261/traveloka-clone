package com.cena.traveloka.search.controller;

import com.cena.traveloka.common.PageResponse;
import com.cena.traveloka.search.dto.PropertySearchRequest;
import com.cena.traveloka.search.dto.PropertySearchResponse;
import com.cena.traveloka.search.dto.SearchAnalyticsRequest;
import com.cena.traveloka.search.service.SearchService;
import com.cena.traveloka.search.service.SearchAnalyticsService;
import com.cena.traveloka.search.service.FilterService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchController {

    SearchService searchService;
    SearchAnalyticsService analyticsService;
    FilterService filterService;

    @PostMapping("/properties")
    public ResponseEntity<PropertySearchResponse> searchProperties(
            @RequestBody @Valid PropertySearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "score,desc") String sort) {

        log.info("Search request received: query='{}', location='{}'",
                request.getQuery(),
                request.getLocation() != null ? request.getLocation().getCity() : "none");

        try {
            // Validate filters
            if (!filterService.validateFilters(request)) {
                return ResponseEntity.badRequest().build();
            }

            // Normalize filters
            PropertySearchRequest normalizedRequest = filterService.normalizeFilters(request);

            // Create pageable with sorting
            Pageable pageable = createPageable(page, size, sort);
            normalizedRequest = normalizedRequest.toBuilder()
                .pageable(pageable)
                .build();

            // Execute search
            PropertySearchResponse response = searchService.searchProperties(normalizedRequest);

            // Record analytics asynchronously
            recordSearchAnalytics(normalizedRequest, response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Search failed for query: '{}'", request.getQuery(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/filters")
    public ResponseEntity<Map<String, Object>> getAvailableFilters(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "vi") String language) {

        try {
            Map<String, Object> filters = filterService.getAvailableFilters(query, language);
            return ResponseEntity.ok(filters);

        } catch (Exception e) {
            log.error("Failed to get available filters", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/filters/counts")
    public ResponseEntity<Map<String, Object>> getFilterCounts(
            @RequestBody @Valid PropertySearchRequest baseRequest) {

        try {
            Map<String, Object> counts = filterService.getFilterCounts(baseRequest);
            return ResponseEntity.ok(counts);

        } catch (Exception e) {
            log.error("Failed to get filter counts", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/analytics/performance")
    public ResponseEntity<Map<String, Object>> getSearchPerformance(
            @RequestParam OffsetDateTime startDate,
            @RequestParam OffsetDateTime endDate) {

        try {
            Map<String, Object> metrics = analyticsService.getSearchPerformanceMetrics(startDate, endDate);
            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            log.error("Failed to get search performance metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/analytics/popular-queries")
    public ResponseEntity<?> getPopularQueries(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "5") int minCount,
            @RequestParam(defaultValue = "50") int limit) {

        try {
            var queries = analyticsService.getPopularSearchQueries(days, minCount, limit);
            return ResponseEntity.ok(queries);

        } catch (Exception e) {
            log.error("Failed to get popular queries", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/analytics/trending-queries")
    public ResponseEntity<?> getTrendingQueries(
            @RequestParam(defaultValue = "7") int recentDays,
            @RequestParam(defaultValue = "30") int historicalDays,
            @RequestParam(defaultValue = "3") int minRecentCount,
            @RequestParam(defaultValue = "20") int limit) {

        try {
            var queries = analyticsService.getTrendingSearchQueries(
                recentDays, historicalDays, minRecentCount, limit);
            return ResponseEntity.ok(queries);

        } catch (Exception e) {
            log.error("Failed to get trending queries", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/analytics/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserSearchPatterns(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "30") int days) {

        try {
            Map<String, Object> patterns = analyticsService.getUserSearchPatterns(userId, days);
            return ResponseEntity.ok(patterns);

        } catch (Exception e) {
            log.error("Failed to get user search patterns for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/analytics/session/{sessionId}")
    public ResponseEntity<?> getSessionAnalytics(@PathVariable UUID sessionId) {
        try {
            var analytics = analyticsService.getSessionAnalytics(sessionId);
            return ResponseEntity.ok(analytics);

        } catch (Exception e) {
            log.error("Failed to get session analytics for session: {}", sessionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private Pageable createPageable(int page, int size, String sort) {
        try {
            String[] sortParts = sort.split(",");
            String property = sortParts[0];
            Sort.Direction direction = sortParts.length > 1 && "desc".equals(sortParts[1]) ?
                Sort.Direction.DESC : Sort.Direction.ASC;

            return PageRequest.of(page, Math.min(size, 100), Sort.by(direction, property));

        } catch (Exception e) {
            log.warn("Invalid sort parameter: {}, using default", sort);
            return PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "score"));
        }
    }

    private void recordSearchAnalytics(PropertySearchRequest request, PropertySearchResponse response) {
        try {
            SearchAnalyticsRequest analyticsRequest = SearchAnalyticsRequest.builder()
                .sessionId(UUID.randomUUID()) // Would come from session
                .userId(null) // Would come from authentication
                .searchQuery(request.getQuery())
                .searchType("property_search")
                .filters(request.toString())
                .searchTimestamp(OffsetDateTime.now())
                .searchResultsInfo(SearchAnalyticsRequest.SearchResultsInfo.builder()
                    .totalResults(response.getPagination().getTotalElements())
                    .responseTimeMs(response.getMetadata().getResponseTimeMs())
                    .build())
                .build();

            analyticsService.recordSearchAnalytics(analyticsRequest);

        } catch (Exception e) {
            log.warn("Failed to record search analytics", e);
            // Don't let analytics failure affect the search response
        }
    }
}
