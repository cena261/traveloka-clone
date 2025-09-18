package com.cena.traveloka.search.resilience;

import com.cena.traveloka.search.dto.PropertySearchRequest;
import com.cena.traveloka.search.dto.PropertySearchResponse;
import com.cena.traveloka.search.dto.SuggestionResponse;
import com.cena.traveloka.search.service.SearchService;
import com.cena.traveloka.search.service.AutoCompleteService;
import com.cena.traveloka.search.service.FilterService;
import com.cena.traveloka.search.service.LocationSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ErrorHandlingTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private AutoCompleteService autoCompleteService;

    @Autowired
    private FilterService filterService;

    @Autowired
    private LocationSearchService locationSearchService;

    @Test
    @DisplayName("T036.1: Invalid search request handling")
    void testInvalidSearchRequestHandling() {
        // Test with null query
        PropertySearchRequest nullQueryRequest = PropertySearchRequest.builder()
            .query(null)
            .language("vi")
            .pageable(PageRequest.of(0, 10))
            .build();

        // Should handle gracefully and return empty results or appropriate response
        PropertySearchResponse response = searchService.searchProperties(nullQueryRequest);
        assertNotNull(response, "Response should not be null even with null query");

        // Test with empty query
        PropertySearchRequest emptyQueryRequest = PropertySearchRequest.builder()
            .query("")
            .language("vi")
            .pageable(PageRequest.of(0, 10))
            .build();

        response = searchService.searchProperties(emptyQueryRequest);
        assertNotNull(response, "Response should not be null even with empty query");

        // Test with extremely long query
        String longQuery = "a".repeat(10000);
        PropertySearchRequest longQueryRequest = PropertySearchRequest.builder()
            .query(longQuery)
            .language("vi")
            .pageable(PageRequest.of(0, 10))
            .build();

        response = searchService.searchProperties(longQueryRequest);
        assertNotNull(response, "Response should not be null even with very long query");
    }

    @Test
    @DisplayName("T036.2: Invalid filter validation")
    void testInvalidFilterValidation() {
        // Test with negative prices
        PropertySearchRequest invalidPriceRequest = PropertySearchRequest.builder()
            .query("hotel")
            .price(PropertySearchRequest.PriceFilter.builder()
                .minPrice(BigDecimal.valueOf(-100))
                .maxPrice(BigDecimal.valueOf(-50))
                .currency("VND")
                .build())
            .build();

        boolean isValid = filterService.validateFilters(invalidPriceRequest);
        assertFalse(isValid, "Request with negative prices should be invalid");

        // Test with invalid price range (min > max)
        PropertySearchRequest invalidRangeRequest = PropertySearchRequest.builder()
            .query("hotel")
            .price(PropertySearchRequest.PriceFilter.builder()
                .minPrice(BigDecimal.valueOf(2000000))
                .maxPrice(BigDecimal.valueOf(1000000))
                .currency("VND")
                .build())
            .build();

        isValid = filterService.validateFilters(invalidRangeRequest);
        assertFalse(isValid, "Request with min price > max price should be invalid");

        // Test with invalid coordinates
        PropertySearchRequest invalidLocationRequest = PropertySearchRequest.builder()
            .query("hotel")
            .location(PropertySearchRequest.LocationFilter.builder()
                .latitude(BigDecimal.valueOf(200.0)) // Invalid latitude
                .longitude(BigDecimal.valueOf(400.0)) // Invalid longitude
                .build())
            .build();

        isValid = filterService.validateFilters(invalidLocationRequest);
        assertFalse(isValid, "Request with invalid coordinates should be invalid");
    }

    @Test
    @DisplayName("T036.3: Autocomplete error handling")
    void testAutocompleteErrorHandling() {
        // Test with null query
        SuggestionResponse response = autoCompleteService.getSuggestions(
            null, "vi", null, null, 10);
        assertNotNull(response, "Response should not be null even with null query");
        assertTrue(response.getSuggestions().isEmpty(), "Should return empty suggestions for null query");

        // Test with empty query
        response = autoCompleteService.getSuggestions(
            "", "vi", null, null, 10);
        assertNotNull(response, "Response should not be null even with empty query");
        assertTrue(response.getSuggestions().isEmpty(), "Should return empty suggestions for empty query");

        // Test with very large limit
        response = autoCompleteService.getSuggestions(
            "hotel", "vi", null, null, 10000);
        assertNotNull(response, "Response should not be null even with large limit");
        assertTrue(response.getSuggestions().size() <= 50, "Should cap suggestions at reasonable limit");

        // Test with invalid language
        response = autoCompleteService.getSuggestions(
            "hotel", "invalid_lang", null, null, 10);
        assertNotNull(response, "Response should not be null even with invalid language");

        // Test with special characters
        response = autoCompleteService.getSuggestions(
            "!@#$%^&*()", "vi", null, null, 10);
        assertNotNull(response, "Response should handle special characters gracefully");
    }

    @Test
    @DisplayName("T036.4: Location search error handling")
    void testLocationSearchErrorHandling() {
        // Test with invalid coordinates
        PropertySearchResponse response = locationSearchService.searchNearLocation(
            200.0, 400.0, 10.0, "hotel", PageRequest.of(0, 10));
        assertNotNull(response, "Response should not be null even with invalid coordinates");

        // Test with negative radius
        response = locationSearchService.searchNearLocation(
            21.0285, 105.8542, -10.0, "hotel", PageRequest.of(0, 10));
        assertNotNull(response, "Response should not be null even with negative radius");

        // Test with extremely large radius
        response = locationSearchService.searchNearLocation(
            21.0285, 105.8542, 100000.0, "hotel", PageRequest.of(0, 10));
        assertNotNull(response, "Response should not be null even with very large radius");

        // Test with null city name
        response = locationSearchService.searchByCity(
            null, "hotel", PageRequest.of(0, 10));
        assertNotNull(response, "Response should not be null even with null city");

        // Test with empty city name
        response = locationSearchService.searchByCity(
            "", "hotel", PageRequest.of(0, 10));
        assertNotNull(response, "Response should not be null even with empty city");
    }

    @Test
    @DisplayName("T036.5: Pagination error handling")
    void testPaginationErrorHandling() {
        PropertySearchRequest request = PropertySearchRequest.builder()
            .query("hotel")
            .language("vi")
            .build();

        // Test with negative page number
        PropertySearchRequest negativePageRequest = request.toBuilder()
            .pageable(PageRequest.of(-1, 10))
            .build();

        PropertySearchResponse response = searchService.searchProperties(negativePageRequest);
        assertNotNull(response, "Response should not be null even with negative page");

        // Test with zero page size
        PropertySearchRequest zeroSizeRequest = request.toBuilder()
            .pageable(PageRequest.of(0, 0))
            .build();

        response = searchService.searchProperties(zeroSizeRequest);
        assertNotNull(response, "Response should not be null even with zero page size");

        // Test with extremely large page size
        PropertySearchRequest largeSizeRequest = request.toBuilder()
            .pageable(PageRequest.of(0, 10000))
            .build();

        response = searchService.searchProperties(largeSizeRequest);
        assertNotNull(response, "Response should not be null even with large page size");
        assertTrue(response.getResults().size() <= 100, "Should cap results at reasonable limit");
    }

    @Test
    @DisplayName("T036.6: Special character handling")
    void testSpecialCharacterHandling() {
        // Test with SQL injection-like strings
        String sqlInjectionQuery = "'; DROP TABLE properties; --";
        PropertySearchRequest sqlInjectionRequest = PropertySearchRequest.builder()
            .query(sqlInjectionQuery)
            .language("vi")
            .pageable(PageRequest.of(0, 10))
            .build();

        PropertySearchResponse response = searchService.searchProperties(sqlInjectionRequest);
        assertNotNull(response, "Response should handle SQL injection attempts safely");

        // Test with HTML/XSS strings
        String xssQuery = "<script>alert('xss')</script>";
        PropertySearchRequest xssRequest = PropertySearchRequest.builder()
            .query(xssQuery)
            .language("vi")
            .pageable(PageRequest.of(0, 10))
            .build();

        response = searchService.searchProperties(xssRequest);
        assertNotNull(response, "Response should handle XSS attempts safely");

        // Test with Unicode characters
        String unicodeQuery = "🏨🌟⭐️";
        PropertySearchRequest unicodeRequest = PropertySearchRequest.builder()
            .query(unicodeQuery)
            .language("vi")
            .pageable(PageRequest.of(0, 10))
            .build();

        response = searchService.searchProperties(unicodeRequest);
        assertNotNull(response, "Response should handle Unicode characters safely");
    }

    @Test
    @DisplayName("T036.7: Concurrent request resilience")
    void testConcurrentRequestResilience() {
        PropertySearchRequest baseRequest = PropertySearchRequest.builder()
            .query("concurrent_test")
            .language("vi")
            .pageable(PageRequest.of(0, 10))
            .build();

        // Execute multiple concurrent requests
        Thread[] threads = new Thread[20];
        volatile boolean[] results = new boolean[threads.length];

        for (int i = 0; i < threads.length; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    PropertySearchRequest request = baseRequest.toBuilder()
                        .query("concurrent_test_" + threadIndex)
                        .build();

                    PropertySearchResponse response = searchService.searchProperties(request);
                    results[threadIndex] = (response != null);
                } catch (Exception e) {
                    results[threadIndex] = false;
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join(10000); // 10 second timeout
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Thread was interrupted");
            }
        }

        // Verify all requests completed successfully
        for (int i = 0; i < results.length; i++) {
            assertTrue(results[i], "Request " + i + " should have completed successfully");
        }
    }

    @Test
    @DisplayName("T036.8: Resource exhaustion handling")
    void testResourceExhaustionHandling() {
        // Test with many rapid requests to check resource management
        PropertySearchRequest request = PropertySearchRequest.builder()
            .query("resource_test")
            .language("vi")
            .pageable(PageRequest.of(0, 10))
            .build();

        int successCount = 0;
        int totalRequests = 100;

        for (int i = 0; i < totalRequests; i++) {
            try {
                PropertySearchRequest iterationRequest = request.toBuilder()
                    .query("resource_test_" + i)
                    .build();

                PropertySearchResponse response = searchService.searchProperties(iterationRequest);
                if (response != null) {
                    successCount++;
                }
            } catch (Exception e) {
                // Count failures but don't fail the test - system should handle gracefully
                System.out.printf("Request %d failed with: %s%n", i, e.getMessage());
            }
        }

        // At least 80% of requests should succeed even under load
        double successRate = (double) successCount / totalRequests;
        assertTrue(successRate >= 0.8,
            String.format("Success rate (%f) should be at least 80%% under load", successRate));
    }

    @Test
    @DisplayName("T036.9: Graceful degradation test")
    void testGracefulDegradation() {
        // Test behavior when services return empty results
        PropertySearchRequest request = PropertySearchRequest.builder()
            .query("nonexistent_12345_test")
            .language("vi")
            .pageable(PageRequest.of(0, 10))
            .build();

        PropertySearchResponse response = searchService.searchProperties(request);
        assertNotNull(response, "Response should not be null even for non-matching queries");
        assertNotNull(response.getResults(), "Results should not be null even when empty");
        assertNotNull(response.getPagination(), "Pagination should not be null even for empty results");
        assertNotNull(response.getMetadata(), "Metadata should not be null even for empty results");

        // Test autocomplete with non-matching query
        SuggestionResponse suggestions = autoCompleteService.getSuggestions(
            "nonexistent_12345_test", "vi", null, null, 10);
        assertNotNull(suggestions, "Suggestions should not be null even for non-matching queries");
        assertNotNull(suggestions.getSuggestions(), "Suggestions list should not be null even when empty");
        assertNotNull(suggestions.getResponseMetadata(), "Response metadata should not be null");
    }

    @Test
    @DisplayName("T036.10: System boundary validation")
    void testSystemBoundaryValidation() {
        // Test with null service dependencies (should be handled by Spring's dependency injection)
        assertNotNull(searchService, "SearchService should be properly injected");
        assertNotNull(autoCompleteService, "AutoCompleteService should be properly injected");
        assertNotNull(filterService, "FilterService should be properly injected");
        assertNotNull(locationSearchService, "LocationSearchService should be properly injected");

        // Test with extreme input values
        PropertySearchRequest extremeRequest = PropertySearchRequest.builder()
            .query("a".repeat(1000)) // Very long query
            .language("unknown_language")
            .price(PropertySearchRequest.PriceFilter.builder()
                .minPrice(BigDecimal.valueOf(Double.MAX_VALUE))
                .maxPrice(BigDecimal.valueOf(Double.MAX_VALUE))
                .currency("INVALID")
                .build())
            .location(PropertySearchRequest.LocationFilter.builder()
                .latitude(BigDecimal.valueOf(Double.MAX_VALUE))
                .longitude(BigDecimal.valueOf(Double.MAX_VALUE))
                .radiusKm(BigDecimal.valueOf(Double.MAX_VALUE))
                .build())
            .pageable(PageRequest.of(Integer.MAX_VALUE, Integer.MAX_VALUE))
            .build();

        // Should handle extreme values gracefully
        PropertySearchResponse response = searchService.searchProperties(extremeRequest);
        assertNotNull(response, "Response should handle extreme input values gracefully");
    }
}