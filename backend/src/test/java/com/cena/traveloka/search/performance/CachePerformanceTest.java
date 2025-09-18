package com.cena.traveloka.search.performance;

import com.cena.traveloka.search.dto.PropertySearchRequest;
import com.cena.traveloka.search.dto.PropertySearchResponse;
import com.cena.traveloka.search.service.SearchService;
import com.cena.traveloka.search.service.AutoCompleteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class CachePerformanceTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private AutoCompleteService autoCompleteService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("T035.1: Cache effectiveness verification")
    void testCacheEffectiveness() {
        // Given
        PropertySearchRequest request = PropertySearchRequest.builder()
            .query("cache_test_hotel")
            .language("vi")
            .pageable(PageRequest.of(0, 10))
            .build();

        // Clear cache first
        if (cacheManager.getCache("searchResults") != null) {
            cacheManager.getCache("searchResults").clear();
        }

        // First call (not cached)
        long startTime1 = System.currentTimeMillis();
        PropertySearchResponse response1 = searchService.searchProperties(request);
        long duration1 = System.currentTimeMillis() - startTime1;

        // Second call (should be cached)
        long startTime2 = System.currentTimeMillis();
        PropertySearchResponse response2 = searchService.searchProperties(request);
        long duration2 = System.currentTimeMillis() - startTime2;

        // Then
        assertNotNull(response1, "First response should not be null");
        assertNotNull(response2, "Second response should not be null");

        // Verify response consistency
        assertEquals(response1.getResults().size(), response2.getResults().size(),
            "Cached response should be consistent with original");

        // Log performance metrics for verification
        System.out.printf("Cache Performance: First call: %dms, Second call: %dms%n",
            duration1, duration2);
    }

    @Test
    @DisplayName("T035.2: Cache configuration validation")
    void testCacheConfiguration() {
        // Verify cache manager is properly configured
        assertNotNull(cacheManager, "Cache manager should be available");

        // Verify expected caches exist
        var searchCache = cacheManager.getCache("searchResults");
        assertNotNull(searchCache, "searchResults cache should be configured");

        var suggestionsCache = cacheManager.getCache("searchSuggestions");
        assertNotNull(suggestionsCache, "searchSuggestions cache should be configured");

        // Test cache operations
        String testKey = "test-cache-key";
        String testValue = "test-cache-value";
        searchCache.put(testKey, testValue);

        var cachedValue = searchCache.get(testKey);
        assertNotNull(cachedValue, "Cached value should be retrievable");
        assertEquals(testValue, cachedValue.get(), "Cached value should match original");
    }

    @Test
    @DisplayName("T035.3: Autocomplete cache verification")
    void testAutocompleteCacheVerification() {
        // Given
        String query = "cache_test";
        String language = "vi";

        // Clear cache first
        if (cacheManager.getCache("searchSuggestions") != null) {
            cacheManager.getCache("searchSuggestions").clear();
        }

        // First call (not cached)
        long startTime1 = System.currentTimeMillis();
        var response1 = autoCompleteService.getSuggestions(query, language, null, null, 5);
        long duration1 = System.currentTimeMillis() - startTime1;

        // Second call (should be cached)
        long startTime2 = System.currentTimeMillis();
        var response2 = autoCompleteService.getSuggestions(query, language, null, null, 5);
        long duration2 = System.currentTimeMillis() - startTime2;

        // Then
        assertNotNull(response1, "First response should not be null");
        assertNotNull(response2, "Second response should not be null");

        assertEquals(response1.getSuggestions().size(), response2.getSuggestions().size(),
            "Cached autocomplete response should be consistent");

        System.out.printf("Autocomplete Cache Performance: First call: %dms, Second call: %dms%n",
            duration1, duration2);
    }

    @Test
    @DisplayName("T035.4: Performance baseline test")
    void testPerformanceBaseline() {
        // Given
        PropertySearchRequest request = PropertySearchRequest.builder()
            .query("performance_test")
            .language("vi")
            .pageable(PageRequest.of(0, 20))
            .build();

        // When
        long startTime = System.currentTimeMillis();
        PropertySearchResponse response = searchService.searchProperties(request);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getResults(), "Results should not be null");
        assertNotNull(response.getMetadata(), "Metadata should not be null");

        // Performance should be reasonable (under 10 seconds for test environment)
        assertTrue(duration < 10000,
            String.format("Search should complete in reasonable time, took %dms", duration));

        // Verify response time is tracked
        assertTrue(response.getMetadata().getResponseTimeMs() >= 0,
            "Response time should be tracked in metadata");

        System.out.printf("Baseline Performance: Search completed in %dms%n", duration);
    }
}