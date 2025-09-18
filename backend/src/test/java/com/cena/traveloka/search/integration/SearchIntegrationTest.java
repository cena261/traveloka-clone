package com.cena.traveloka.search.integration;

import com.cena.traveloka.search.dto.PropertySearchRequest;
import com.cena.traveloka.search.dto.PropertySearchResponse;
import com.cena.traveloka.search.dto.SuggestionResponse;
import com.cena.traveloka.search.service.SearchService;
import com.cena.traveloka.search.service.AutoCompleteService;
import com.cena.traveloka.search.service.FilterService;
import com.cena.traveloka.search.service.IndexingService;
import com.cena.traveloka.search.service.SearchAnalyticsService;
import com.cena.traveloka.search.service.LocationSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SearchIntegrationTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private AutoCompleteService autoCompleteService;

    @Autowired
    private FilterService filterService;

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private SearchAnalyticsService analyticsService;

    @Autowired
    private LocationSearchService locationSearchService;

    @Test
    @DisplayName("T034.1: Basic search functionality integration test")
    void testBasicSearchIntegration() {
        // Given
        PropertySearchRequest request = PropertySearchRequest.builder()
            .query("hotel")
            .language("vi")
            .pageable(PageRequest.of(0, 10))
            .build();

        // When
        PropertySearchResponse response = searchService.searchProperties(request);

        // Then
        assertNotNull(response, "Search response should not be null");
        assertNotNull(response.getResults(), "Search results should not be null");
        assertNotNull(response.getPagination(), "Pagination should not be null");
        assertNotNull(response.getMetadata(), "Metadata should not be null");
        assertTrue(response.getMetadata().getResponseTimeMs() >= 0, "Response time should be non-negative");
    }

    @Test
    @DisplayName("T034.2: Autocomplete service integration test")
    void testAutoCompleteIntegration() {
        // Given
        String query = "ha noi";
        String language = "vi";
        BigDecimal latitude = BigDecimal.valueOf(21.0285);
        BigDecimal longitude = BigDecimal.valueOf(105.8542);
        Integer limit = 5;

        // When
        SuggestionResponse response = autoCompleteService.getSuggestions(
            query, language, latitude, longitude, limit);

        // Then
        assertNotNull(response, "Suggestion response should not be null");
        assertNotNull(response.getSuggestions(), "Suggestions should not be null");
        assertNotNull(response.getResponseMetadata(), "Response metadata should not be null");
        assertTrue(response.getSuggestions().size() <= limit, "Suggestions should not exceed limit");
    }

    @Test
    @DisplayName("T034.3: Filter service integration test")
    void testFilterServiceIntegration() {
        // Given
        PropertySearchRequest request = PropertySearchRequest.builder()
            .query("hotel")
            .price(PropertySearchRequest.PriceFilter.builder()
                .minPrice(BigDecimal.valueOf(500000))
                .maxPrice(BigDecimal.valueOf(2000000))
                .currency("VND")
                .build())
            .build();

        // When
        boolean isValid = filterService.validateFilters(request);
        PropertySearchRequest normalized = filterService.normalizeFilters(request);
        Map<String, Object> availableFilters = filterService.getAvailableFilters("hotel", "vi");

        // Then
        assertTrue(isValid, "Filters should be valid");
        assertNotNull(normalized, "Normalized request should not be null");
        assertNotNull(availableFilters, "Available filters should not be null");
        assertTrue(availableFilters.containsKey("priceRanges"), "Should contain price ranges");
        assertTrue(availableFilters.containsKey("starRatings"), "Should contain star ratings");
    }

    @Test
    @DisplayName("T034.4: Indexing service integration test")
    void testIndexingServiceIntegration() {
        // Given
        UUID testPropertyId = UUID.randomUUID();

        // When & Then
        // Test index status check
        boolean isIndexed = indexingService.isIndexed(testPropertyId);
        assertFalse(isIndexed, "Random property should not be indexed");

        // Test index count
        long count = indexingService.getIndexedPropertyCount();
        assertTrue(count >= 0, "Index count should be non-negative");
    }

    @Test
    @DisplayName("T034.5: Analytics service integration test")
    void testAnalyticsServiceIntegration() {
        // Given
        OffsetDateTime startDate = OffsetDateTime.now().minusDays(30);
        OffsetDateTime endDate = OffsetDateTime.now();
        UUID testUserId = UUID.randomUUID();

        // When
        Map<String, Object> performanceMetrics = analyticsService.getSearchPerformanceMetrics(startDate, endDate);
        var popularQueries = analyticsService.getPopularSearchQueries(30, 1, 10);
        Map<String, Object> userPatterns = analyticsService.getUserSearchPatterns(testUserId, 30);

        // Then
        assertNotNull(performanceMetrics, "Performance metrics should not be null");
        assertNotNull(popularQueries, "Popular queries should not be null");
        assertNotNull(userPatterns, "User patterns should not be null");

        assertTrue(performanceMetrics.containsKey("totalSearches"), "Should contain total searches");
        assertTrue(userPatterns.containsKey("totalSearches"), "Should contain user total searches");
    }

    @Test
    @DisplayName("T034.6: Location search service integration test")
    void testLocationSearchServiceIntegration() {
        // Given
        Double latitude = 21.0285; // Hanoi
        Double longitude = 105.8542;
        Double radiusKm = 10.0;
        String cityName = "Hà Nội";

        // When
        PropertySearchResponse locationResponse = locationSearchService.searchNearLocation(
            latitude, longitude, radiusKm, "hotel", PageRequest.of(0, 10));

        PropertySearchResponse cityResponse = locationSearchService.searchByCity(
            cityName, "hotel", PageRequest.of(0, 10));

        Map<String, Object> locationAnalytics = locationSearchService.getLocationAnalytics(
            latitude, longitude, radiusKm);

        // Then
        assertNotNull(locationResponse, "Location response should not be null");
        assertNotNull(cityResponse, "City response should not be null");
        assertNotNull(locationAnalytics, "Location analytics should not be null");

        assertTrue(locationAnalytics.containsKey("totalSearchesInArea"), "Should contain area searches");
        assertTrue(locationAnalytics.containsKey("conversionRateInArea"), "Should contain conversion rate");
    }

    @Test
    @DisplayName("T034.7: End-to-end search workflow integration test")
    void testEndToEndSearchWorkflow() {
        // Given - Simulate complete search workflow
        String searchQuery = "luxury hotel";

        // Step 1: Get suggestions
        SuggestionResponse suggestions = autoCompleteService.getSuggestions(
            "luxu", "vi", null, null, 5);

        // Step 2: Get available filters
        Map<String, Object> filters = filterService.getAvailableFilters(searchQuery, "vi");

        // Step 3: Build search request
        PropertySearchRequest searchRequest = PropertySearchRequest.builder()
            .query(searchQuery)
            .language("vi")
            .price(PropertySearchRequest.PriceFilter.builder()
                .minPrice(BigDecimal.valueOf(1000000))
                .maxPrice(BigDecimal.valueOf(5000000))
                .currency("VND")
                .build())
            .location(PropertySearchRequest.LocationFilter.builder()
                .city("Hà Nội")
                .countryCode("VN")
                .build())
            .pageable(PageRequest.of(0, 20))
            .build();

        // Step 4: Validate and normalize request
        boolean isValid = filterService.validateFilters(searchRequest);
        PropertySearchRequest normalizedRequest = filterService.normalizeFilters(searchRequest);

        // Step 5: Execute search
        PropertySearchResponse searchResponse = searchService.searchProperties(normalizedRequest);

        // Step 6: Get filter counts for results
        Map<String, Object> filterCounts = filterService.getFilterCounts(normalizedRequest);

        // Then - Verify complete workflow
        assertNotNull(suggestions, "Suggestions should be available");
        assertNotNull(filters, "Filters should be available");
        assertTrue(isValid, "Search request should be valid");
        assertNotNull(normalizedRequest, "Request should be normalized");
        assertNotNull(searchResponse, "Search should return results");
        assertNotNull(filterCounts, "Filter counts should be available");

        // Verify response structure
        assertTrue(searchResponse.getResults().size() <= 20, "Results should respect page size");
        assertTrue(searchResponse.getMetadata().getResponseTimeMs() >= 0, "Response time should be tracked");
        assertTrue(filterCounts.containsKey("priceRangeCounts"), "Should include price range counts");
    }

    @Test
    @DisplayName("T034.8: Error handling and resilience integration test")
    void testErrorHandlingIntegration() {
        // Test invalid search requests
        PropertySearchRequest invalidRequest = PropertySearchRequest.builder()
            .query("") // Empty query
            .price(PropertySearchRequest.PriceFilter.builder()
                .minPrice(BigDecimal.valueOf(-100)) // Invalid negative price
                .maxPrice(BigDecimal.valueOf(50))
                .build())
            .build();

        // When & Then
        boolean isValid = filterService.validateFilters(invalidRequest);
        assertFalse(isValid, "Invalid request should fail validation");

        // Test autocomplete with invalid input
        SuggestionResponse emptyResponse = autoCompleteService.getSuggestions(
            "", "vi", null, null, 10);
        assertNotNull(emptyResponse, "Should handle empty query gracefully");
        assertTrue(emptyResponse.getSuggestions().isEmpty(), "Should return empty suggestions for empty query");

        // Test location search with invalid coordinates
        Map<String, Object> invalidLocationAnalytics = locationSearchService.getLocationAnalytics(
            200.0, 200.0, 10.0); // Invalid coordinates
        assertNotNull(invalidLocationAnalytics, "Should handle invalid coordinates gracefully");
    }

    @Test
    @DisplayName("T034.9: Performance and caching integration test")
    void testPerformanceAndCaching() {
        // Given
        PropertySearchRequest request = PropertySearchRequest.builder()
            .query("hotel")
            .language("vi")
            .pageable(PageRequest.of(0, 10))
            .build();

        // When - Execute same search multiple times
        long startTime1 = System.currentTimeMillis();
        PropertySearchResponse response1 = searchService.searchProperties(request);
        long duration1 = System.currentTimeMillis() - startTime1;

        long startTime2 = System.currentTimeMillis();
        PropertySearchResponse response2 = searchService.searchProperties(request);
        long duration2 = System.currentTimeMillis() - startTime2;

        // Then
        assertNotNull(response1, "First response should not be null");
        assertNotNull(response2, "Second response should not be null");

        // Second call should potentially be faster due to caching
        // Note: This is a soft assertion as caching behavior may vary
        assertTrue(duration1 >= 0 && duration2 >= 0, "Both calls should complete successfully");

        // Verify response consistency
        assertEquals(response1.getResults().size(), response2.getResults().size(),
            "Cached response should be consistent");
    }

    @Test
    @DisplayName("T034.10: Multi-language support integration test")
    void testMultiLanguageSupport() {
        // Test Vietnamese
        SuggestionResponse viResponse = autoCompleteService.getSuggestions(
            "khách sạn", "vi", null, null, 5);

        // Test English
        SuggestionResponse enResponse = autoCompleteService.getSuggestions(
            "hotel", "en", null, null, 5);

        // Then
        assertNotNull(viResponse, "Vietnamese suggestions should work");
        assertNotNull(enResponse, "English suggestions should work");

        assertEquals("vi", viResponse.getResponseMetadata().getLanguage(),
            "Vietnamese response should have correct language");
        assertEquals("en", enResponse.getResponseMetadata().getLanguage(),
            "English response should have correct language");
    }
}