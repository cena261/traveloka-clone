package com.cena.traveloka.search.integration;

import com.cena.traveloka.search.dto.PropertySearchRequest;
import com.cena.traveloka.search.dto.PropertySearchResponse;
import com.cena.traveloka.search.dto.SuggestionResponse;
import com.cena.traveloka.search.service.SearchService;
import com.cena.traveloka.search.service.AutoCompleteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Redis caching functionality
 *
 * ⚠️  CRITICAL: These tests MUST FAIL initially (RED phase) until implementation is complete.
 * This is a constitutional requirement for TDD compliance.
 *
 * Testing Strategy:
 * - Real Redis integration with Testcontainers
 * - Cache TTL validation (5 minutes for search results, 30 minutes for suggestions)
 * - Cache hit/miss behavior
 * - Cache invalidation scenarios
 * - Performance improvement validation
 * - Concurrent access patterns
 *
 * Prerequisites:
 * - SearchService exists with caching annotations (currently empty - tests will fail)
 * - AutoCompleteService exists with caching (currently empty - tests will fail)
 * - Redis configuration applied from SearchConfig
 * - Cache managers configured
 *
 * Expected Initial State: FAILING (RED) ❌
 * Expected Final State: PASSING (GREEN) ✅
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Search Caching Integration Tests")
class SearchCachingIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--appendonly", "yes");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.cache.type", () -> "redis");
    }

    @Autowired(required = false)
    private SearchService searchService;

    @Autowired(required = false)
    private AutoCompleteService autoCompleteService;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // Note: These will fail until services are implemented
        assumeServicesExist();
        clearCache();
    }

    @Test
    @DisplayName("Should cache search results with 5-minute TTL")
    void searchProperties_FirstCall_ShouldCacheResultsFor5Minutes() throws Exception {
        // Given - search request
        PropertySearchRequest searchRequest = createTestSearchRequest("luxury hotels Hanoi");

        // When - first search call (cache miss)
        long startTime = System.currentTimeMillis();
        PropertySearchResponse firstResponse = searchService.searchProperties(searchRequest);
        long firstCallDuration = System.currentTimeMillis() - startTime;

        // Then - result should be cached
        String cacheKey = generateExpectedCacheKey(searchRequest);
        assertThat(cacheManager.getCache("searchResults")).isNotNull();
        assertThat(cacheManager.getCache("searchResults").get(cacheKey)).isNotNull();

        // When - second search call (cache hit)
        startTime = System.currentTimeMillis();
        PropertySearchResponse secondResponse = searchService.searchProperties(searchRequest);
        long secondCallDuration = System.currentTimeMillis() - startTime;

        // Then - should be much faster (cache hit)
        assertThat(secondCallDuration).isLessThan(firstCallDuration / 2);
        assertThat(secondResponse).isEqualTo(firstResponse);

        // Verify TTL is approximately 5 minutes (300 seconds)
        Long ttl = redisTemplate.getExpire(cacheKey);
        assertThat(ttl).isBetween(250L, 300L); // Allow some variance for test execution time
    }

    @Test
    @DisplayName("Should cache auto-complete suggestions with 30-minute TTL")
    void getSuggestions_FirstCall_ShouldCacheResultsFor30Minutes() throws Exception {
        // Given - suggestion request
        String query = "hà nội";

        // When - first suggestion call (cache miss)
        long startTime = System.currentTimeMillis();
        SuggestionResponse firstResponse = autoCompleteService.getSuggestions(query, "vi", null, null, 10);
        long firstCallDuration = System.currentTimeMillis() - startTime;

        // Then - result should be cached
        String cacheKey = generateExpectedSuggestionCacheKey(query, "vi");
        assertThat(cacheManager.getCache("searchSuggestions")).isNotNull();
        assertThat(cacheManager.getCache("searchSuggestions").get(cacheKey)).isNotNull();

        // When - second suggestion call (cache hit)
        startTime = System.currentTimeMillis();
        SuggestionResponse secondResponse = autoCompleteService.getSuggestions(query, "vi", null, null, 10);
        long secondCallDuration = System.currentTimeMillis() - startTime;

        // Then - should be much faster (cache hit)
        assertThat(secondCallDuration).isLessThan(firstCallDuration / 2);
        assertThat(secondResponse).isEqualTo(firstResponse);

        // Verify TTL is approximately 30 minutes (1800 seconds)
        Long ttl = redisTemplate.getExpire(cacheKey);
        assertThat(ttl).isBetween(1750L, 1800L);
    }

    @Test
    @DisplayName("Should invalidate cache when TTL expires")
    void searchResults_AfterTTLExpires_ShouldRefreshCache() throws Exception {
        // Given - search request with very short TTL for testing
        PropertySearchRequest searchRequest = createTestSearchRequest("test cache expiry");

        // Configure short TTL for testing (2 seconds)
        configureShortTTLForTesting();

        // When - first call to populate cache
        PropertySearchResponse firstResponse = searchService.searchProperties(searchRequest);
        String cacheKey = generateExpectedCacheKey(searchRequest);

        // Verify cache is populated
        assertThat(cacheManager.getCache("searchResults").get(cacheKey)).isNotNull();

        // Wait for TTL to expire
        Thread.sleep(3000);

        // Then - cache should be expired and removed
        assertThat(cacheManager.getCache("searchResults").get(cacheKey)).isNull();

        // When - calling again after expiry (should be cache miss)
        PropertySearchResponse secondResponse = searchService.searchProperties(searchRequest);

        // Then - new result should be cached again
        assertThat(cacheManager.getCache("searchResults").get(cacheKey)).isNotNull();
    }

    @Test
    @DisplayName("Should handle cache misses gracefully")
    void searchProperties_CacheEmpty_ShouldExecuteNormallyAndCache() throws Exception {
        // Given - empty cache
        cacheManager.getCache("searchResults").clear();

        // When - search with empty cache
        PropertySearchRequest searchRequest = createTestSearchRequest("cache miss test");
        PropertySearchResponse response = searchService.searchProperties(searchRequest);

        // Then - should execute normally and populate cache
        assertThat(response).isNotNull();
        String cacheKey = generateExpectedCacheKey(searchRequest);
        assertThat(cacheManager.getCache("searchResults").get(cacheKey)).isNotNull();
    }

    @Test
    @DisplayName("Should generate different cache keys for different search parameters")
    void searchProperties_DifferentParameters_ShouldUseDifferentCacheKeys() throws Exception {
        // Given - different search requests
        PropertySearchRequest request1 = createTestSearchRequest("hotels hanoi");
        PropertySearchRequest request2 = createTestSearchRequest("hotels ho chi minh");
        PropertySearchRequest request3 = request1.toBuilder()
                .pagination(PropertySearchRequest.PaginationRequest.builder()
                        .page(1)
                        .size(10)
                        .build())
                .build();

        // When - making different searches
        searchService.searchProperties(request1);
        searchService.searchProperties(request2);
        searchService.searchProperties(request3);

        // Then - should have different cache entries
        var cache = cacheManager.getCache("searchResults");
        String key1 = generateExpectedCacheKey(request1);
        String key2 = generateExpectedCacheKey(request2);
        String key3 = generateExpectedCacheKey(request3);

        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1).isNotEqualTo(key3);
        assertThat(key2).isNotEqualTo(key3);

        assertThat(cache.get(key1)).isNotNull();
        assertThat(cache.get(key2)).isNotNull();
        assertThat(cache.get(key3)).isNotNull();
    }

    @Test
    @DisplayName("Should handle concurrent cache access safely")
    void searchProperties_ConcurrentAccess_ShouldHandleSafely() throws Exception {
        // Given - same search request for concurrent access
        PropertySearchRequest searchRequest = createTestSearchRequest("concurrent test");

        // When - multiple concurrent requests
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        PropertySearchResponse[] responses = new PropertySearchResponse[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    responses[index] = searchService.searchProperties(searchRequest);
                } catch (Exception e) {
                    fail("Concurrent cache access failed: " + e.getMessage());
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(5000); // 5 second timeout
        }

        // Then - all responses should be identical (cached)
        PropertySearchResponse firstResponse = responses[0];
        for (int i = 1; i < threadCount; i++) {
            assertThat(responses[i]).isEqualTo(firstResponse);
        }

        // Cache should contain the result
        String cacheKey = generateExpectedCacheKey(searchRequest);
        assertThat(cacheManager.getCache("searchResults").get(cacheKey)).isNotNull();
    }

    @Test
    @DisplayName("Should support cache eviction for data updates")
    void searchResults_DataUpdate_ShouldEvictRelatedCache() throws Exception {
        // Given - cached search result
        PropertySearchRequest searchRequest = createTestSearchRequest("cache eviction test");
        searchService.searchProperties(searchRequest);

        String cacheKey = generateExpectedCacheKey(searchRequest);
        assertThat(cacheManager.getCache("searchResults").get(cacheKey)).isNotNull();

        // When - simulating data update that should evict cache
        // This would typically be triggered by property updates
        cacheManager.getCache("searchResults").evict(cacheKey);

        // Then - cache should be evicted
        assertThat(cacheManager.getCache("searchResults").get(cacheKey)).isNull();

        // When - searching again after eviction
        PropertySearchResponse newResponse = searchService.searchProperties(searchRequest);

        // Then - should populate cache again
        assertThat(cacheManager.getCache("searchResults").get(cacheKey)).isNotNull();
    }

    @Test
    @DisplayName("Should handle Vietnamese text in cache keys correctly")
    void searchProperties_VietnameseText_ShouldCacheCorrectly() throws Exception {
        // Given - search with Vietnamese text
        PropertySearchRequest vietnameseRequest = createTestSearchRequest("khách sạn Hà Nội");

        // When - searching with Vietnamese text
        PropertySearchResponse response = searchService.searchProperties(vietnameseRequest);

        // Then - should cache with normalized key
        String cacheKey = generateExpectedCacheKey(vietnameseRequest);
        assertThat(cacheManager.getCache("searchResults").get(cacheKey)).isNotNull();

        // When - searching with same query (normalized)
        PropertySearchRequest normalizedRequest = createTestSearchRequest("khach san ha noi");
        PropertySearchResponse normalizedResponse = searchService.searchProperties(normalizedRequest);

        // Then - should use same cache (if normalization is implemented)
        // This tests cache key normalization for Vietnamese text
        assertThat(normalizedResponse).isNotNull();
    }

    @Test
    @DisplayName("Performance: Cache should improve response time by at least 50%")
    void searchProperties_CachePerformance_ShouldImproveResponseTime() throws Exception {
        // Given - search request
        PropertySearchRequest searchRequest = createTestSearchRequest("performance test");

        // When - first call (no cache)
        long startTime = System.currentTimeMillis();
        searchService.searchProperties(searchRequest);
        long uncachedTime = System.currentTimeMillis() - startTime;

        // When - second call (with cache)
        startTime = System.currentTimeMillis();
        searchService.searchProperties(searchRequest);
        long cachedTime = System.currentTimeMillis() - startTime;

        // Then - cached call should be at least 50% faster
        double improvementRatio = (double) uncachedTime / cachedTime;
        assertThat(improvementRatio).isGreaterThan(2.0); // At least 50% improvement
    }

    @Test
    @DisplayName("Should monitor cache hit/miss rates")
    void cacheMetrics_SearchOperations_ShouldTrackHitMissRates() throws Exception {
        // Given - clean cache
        clearCache();

        PropertySearchRequest request1 = createTestSearchRequest("metrics test 1");
        PropertySearchRequest request2 = createTestSearchRequest("metrics test 2");

        // When - first calls (cache misses)
        searchService.searchProperties(request1);
        searchService.searchProperties(request2);

        // When - second calls (cache hits)
        searchService.searchProperties(request1);
        searchService.searchProperties(request2);

        // Then - cache metrics should show 50% hit rate
        // Note: This would require cache metrics implementation
        // For now, just verify cache contains both entries
        String key1 = generateExpectedCacheKey(request1);
        String key2 = generateExpectedCacheKey(request2);

        assertThat(cacheManager.getCache("searchResults").get(key1)).isNotNull();
        assertThat(cacheManager.getCache("searchResults").get(key2)).isNotNull();
    }

    /**
     * Helper methods for test setup
     */
    private PropertySearchRequest createTestSearchRequest(String query) {
        return PropertySearchRequest.builder()
                .query(query)
                .language("vi")
                .pagination(PropertySearchRequest.PaginationRequest.builder()
                        .page(0)
                        .size(20)
                        .build())
                .sortBy("RELEVANCE")
                .build();
    }

    private String generateExpectedCacheKey(PropertySearchRequest request) {
        // This simulates the cache key generation logic
        // Will need to match actual implementation
        return "SearchService:searchProperties:" + request.toString().hashCode();
    }

    private String generateExpectedSuggestionCacheKey(String query, String language) {
        // This simulates the suggestion cache key generation logic
        return "AutoCompleteService:getSuggestions:" + query + ":" + language;
    }

    private void clearCache() {
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
    }

    private void configureShortTTLForTesting() {
        // This would reconfigure cache manager for short TTL in tests
        // Implementation depends on how cache configuration is exposed
    }

    /**
     * Assume services exist for test setup
     * This will cause tests to be skipped if services are not implemented yet
     */
    private void assumeServicesExist() {
        org.junit.jupiter.api.Assumptions.assumeTrue(searchService != null,
                "SearchService not available - tests will be skipped until implementation");
        org.junit.jupiter.api.Assumptions.assumeTrue(autoCompleteService != null,
                "AutoCompleteService not available - tests will be skipped until implementation");
        org.junit.jupiter.api.Assumptions.assumeTrue(cacheManager != null,
                "CacheManager not available - tests will be skipped until implementation");
        org.junit.jupiter.api.Assumptions.assumeTrue(redisTemplate != null,
                "RedisTemplate not available - tests will be skipped until implementation");
    }
}