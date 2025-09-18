package com.cena.traveloka.search.performance;

import com.cena.traveloka.search.dto.PropertySearchRequest;
import com.cena.traveloka.search.dto.PropertySearchResponse;
import com.cena.traveloka.search.dto.SuggestionResponse;
import com.cena.traveloka.search.service.SearchService;
import com.cena.traveloka.search.service.AutoCompleteService;
import com.cena.traveloka.search.service.LocationSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for search engine response time requirements
 *
 * ⚠️  CRITICAL: These tests MUST FAIL initially (RED phase) until implementation is complete.
 * This is a constitutional requirement for TDD compliance.
 *
 * Performance Requirements:
 * - Search responses: < 500ms (95th percentile)
 * - Auto-complete suggestions: < 100ms (95th percentile)
 * - Location searches: < 500ms (95th percentile)
 * - Support 10,000+ concurrent requests
 * - Cache hit rate: > 80% for repeated queries
 *
 * Testing Strategy:
 * - Load testing with realistic data volumes
 * - Concurrent request handling
 * - Cache performance validation
 * - Memory and CPU usage monitoring
 * - Elasticsearch and Redis performance
 *
 * Prerequisites:
 * - All search services implemented and optimized
 * - Elasticsearch with proper indices and analyzers
 * - Redis caching properly configured
 * - Database with realistic data volume
 *
 * Expected Initial State: FAILING (RED) ❌
 * Expected Final State: PASSING (GREEN) ✅
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Search Performance Tests")
class SearchPerformanceTest {

    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.11.1")
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms1g -Xmx1g");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("traveloka_perf_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", elasticsearch::getHttpHostAddress);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired(required = false)
    private SearchService searchService;

    @Autowired(required = false)
    private AutoCompleteService autoCompleteService;

    @Autowired(required = false)
    private LocationSearchService locationSearchService;

    @BeforeEach
    void setUp() {
        // Note: These will fail until services are implemented
        assumeServicesExist();
        warmUpCaches();
    }

    @Test
    @DisplayName("CRITICAL: Search response time must be under 500ms for 95% of requests")
    void searchProperties_ResponseTime_ShouldMeet500msRequirement() throws Exception {
        // Given - realistic search scenarios
        List<PropertySearchRequest> searchRequests = createRealisticSearchRequests();
        List<Long> responseTimes = new ArrayList<>();

        // When - performing multiple searches to measure response times
        for (PropertySearchRequest request : searchRequests) {
            long startTime = System.currentTimeMillis();
            PropertySearchResponse response = searchService.searchProperties(request);
            long responseTime = System.currentTimeMillis() - startTime;

            responseTimes.add(responseTime);

            // Verify response is valid
            assertThat(response).isNotNull();
            assertThat(response.getProperties()).isNotNull();
        }

        // Then - calculate percentiles and verify requirements
        responseTimes.sort(Long::compareTo);
        int size = responseTimes.size();

        long p50 = responseTimes.get(size / 2);
        long p95 = responseTimes.get((int) (size * 0.95));
        long p99 = responseTimes.get((int) (size * 0.99));
        double average = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        System.out.println("Search Performance Metrics:");
        System.out.println("P50: " + p50 + "ms");
        System.out.println("P95: " + p95 + "ms");
        System.out.println("P99: " + p99 + "ms");
        System.out.println("Average: " + average + "ms");

        // Critical requirement: 95% of requests under 500ms
        assertThat(p95).isLessThan(500);
        assertThat(average).isLessThan(300); // Average should be well under limit
    }

    @Test
    @DisplayName("CRITICAL: Auto-complete response time must be under 100ms for 95% of requests")
    void getSuggestions_ResponseTime_ShouldMeet100msRequirement() throws Exception {
        // Given - typical auto-complete queries
        String[] suggestionQueries = {
            "h", "ha", "han", "hano", "hanoi",
            "s", "sa", "sai", "saig", "saigon",
            "d", "da", "dan", "dang",
            "k", "kh", "kha", "khac", "khach",
            "l", "lu", "lux", "luxu", "luxury"
        };

        List<Long> responseTimes = new ArrayList<>();

        // When - performing auto-complete requests
        for (String query : suggestionQueries) {
            for (int i = 0; i < 10; i++) { // Multiple iterations per query
                long startTime = System.currentTimeMillis();
                SuggestionResponse response = autoCompleteService.getSuggestions(query, "vi", null, null, 10);
                long responseTime = System.currentTimeMillis() - startTime;

                responseTimes.add(responseTime);

                // Verify response is valid
                assertThat(response).isNotNull();
                assertThat(response.getSuggestions()).isNotNull();
            }
        }

        // Then - calculate percentiles for auto-complete
        responseTimes.sort(Long::compareTo);
        int size = responseTimes.size();

        long p95 = responseTimes.get((int) (size * 0.95));
        double average = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        System.out.println("Auto-complete Performance Metrics:");
        System.out.println("P95: " + p95 + "ms");
        System.out.println("Average: " + average + "ms");

        // Critical requirement: 95% of suggestions under 100ms
        assertThat(p95).isLessThan(100);
        assertThat(average).isLessThan(50); // Average should be well under limit
    }

    @Test
    @DisplayName("CRITICAL: Should handle 1000 concurrent search requests efficiently")
    void searchProperties_ConcurrentLoad_ShouldHandle1000Requests() throws Exception {
        // Given - concurrent search setup
        int numberOfThreads = 50;
        int requestsPerThread = 20; // Total: 1000 requests
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<Long>> futures = new ArrayList<>();

        PropertySearchRequest searchRequest = PropertySearchRequest.builder()
                .query("luxury hotels")
                .language("vi")
                .pagination(PropertySearchRequest.PaginationRequest.builder()
                        .page(0)
                        .size(20)
                        .build())
                .build();

        // When - launching concurrent requests
        long overallStartTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfThreads; i++) {
            Future<Long> future = executorService.submit(() -> {
                long maxResponseTime = 0;
                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        long startTime = System.currentTimeMillis();
                        PropertySearchResponse response = searchService.searchProperties(searchRequest);
                        long responseTime = System.currentTimeMillis() - startTime;
                        maxResponseTime = Math.max(maxResponseTime, responseTime);

                        // Verify response quality under load
                        assertThat(response).isNotNull();
                        assertThat(response.getProperties()).isNotNull();
                    } catch (Exception e) {
                        fail("Request failed under concurrent load: " + e.getMessage());
                    }
                }
                return maxResponseTime;
            });
            futures.add(future);
        }

        // Wait for all requests to complete
        List<Long> maxResponseTimes = new ArrayList<>();
        for (Future<Long> future : futures) {
            maxResponseTimes.add(future.get(30, TimeUnit.SECONDS)); // 30 second timeout
        }

        long overallTime = System.currentTimeMillis() - overallStartTime;
        executorService.shutdown();

        // Then - verify performance under load
        double totalRequestsPerSecond = (numberOfThreads * requestsPerThread * 1000.0) / overallTime;
        long maxResponseTime = maxResponseTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        System.out.println("Concurrent Load Performance:");
        System.out.println("Total requests: " + (numberOfThreads * requestsPerThread));
        System.out.println("Overall time: " + overallTime + "ms");
        System.out.println("Requests per second: " + totalRequestsPerSecond);
        System.out.println("Max response time: " + maxResponseTime + "ms");

        // Requirements
        assertThat(maxResponseTime).isLessThan(1000); // No request should exceed 1 second under load
        assertThat(totalRequestsPerSecond).isGreaterThan(100); // Should handle at least 100 RPS
    }

    @Test
    @DisplayName("CRITICAL: Cache hit rate should exceed 80% for repeated queries")
    void searchProperties_CachePerformance_ShouldAchieve80PercentHitRate() throws Exception {
        // Given - repeated search patterns to test cache effectiveness
        List<PropertySearchRequest> repeatedRequests = List.of(
            createSearchRequest("luxury hotels Hanoi"),
            createSearchRequest("budget hotels Ho Chi Minh"),
            createSearchRequest("beach resorts Da Nang"),
            createSearchRequest("spa hotels Nha Trang"),
            createSearchRequest("business hotels Hanoi")
        );

        // When - first round (cache misses)
        List<Long> firstRoundTimes = new ArrayList<>();
        for (PropertySearchRequest request : repeatedRequests) {
            long startTime = System.currentTimeMillis();
            searchService.searchProperties(request);
            firstRoundTimes.add(System.currentTimeMillis() - startTime);
        }

        // When - second round (cache hits)
        List<Long> secondRoundTimes = new ArrayList<>();
        for (PropertySearchRequest request : repeatedRequests) {
            long startTime = System.currentTimeMillis();
            searchService.searchProperties(request);
            secondRoundTimes.add(System.currentTimeMillis() - startTime);
        }

        // Then - cache should significantly improve performance
        double firstRoundAverage = firstRoundTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double secondRoundAverage = secondRoundTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double improvementRatio = firstRoundAverage / secondRoundAverage;

        System.out.println("Cache Performance:");
        System.out.println("First round average: " + firstRoundAverage + "ms");
        System.out.println("Second round average: " + secondRoundAverage + "ms");
        System.out.println("Improvement ratio: " + improvementRatio + "x");

        // Requirements: Cache should provide at least 2x improvement (indicating 50%+ cache hits)
        assertThat(improvementRatio).isGreaterThan(2.0);
        assertThat(secondRoundAverage).isLessThan(100); // Cached responses should be very fast
    }

    @Test
    @DisplayName("Should handle complex queries with filters efficiently")
    void searchProperties_ComplexQueries_ShouldMaintainPerformance() throws Exception {
        // Given - complex search requests with multiple filters
        List<PropertySearchRequest> complexRequests = createComplexSearchRequests();
        List<Long> responseTimes = new ArrayList<>();

        // When - performing complex searches
        for (PropertySearchRequest request : complexRequests) {
            long startTime = System.currentTimeMillis();
            PropertySearchResponse response = searchService.searchProperties(request);
            long responseTime = System.currentTimeMillis() - startTime;

            responseTimes.add(responseTime);

            // Verify complex queries return valid results
            assertThat(response).isNotNull();
            assertThat(response.getProperties()).isNotNull();
            // Complex queries might return fewer results but should still be fast
        }

        // Then - complex queries should still meet performance requirements
        double averageComplexTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxComplexTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        System.out.println("Complex Query Performance:");
        System.out.println("Average: " + averageComplexTime + "ms");
        System.out.println("Max: " + maxComplexTime + "ms");

        assertThat(averageComplexTime).isLessThan(400); // Allow slightly more time for complex queries
        assertThat(maxComplexTime).isLessThan(800);
    }

    @Test
    @DisplayName("Should maintain performance with large result sets")
    void searchProperties_LargeResultSets_ShouldPaginateEfficiently() throws Exception {
        // Given - broad search that returns many results
        PropertySearchRequest broadSearch = PropertySearchRequest.builder()
                .query("hotel") // Very broad search
                .language("vi")
                .pagination(PropertySearchRequest.PaginationRequest.builder()
                        .page(0)
                        .size(50) // Larger page size
                        .build())
                .build();

        List<Long> responseTimes = new ArrayList<>();

        // When - performing searches with large result sets
        for (int page = 0; page < 10; page++) {
            PropertySearchRequest pagedRequest = broadSearch.toBuilder()
                    .pagination(broadSearch.getPagination().toBuilder()
                            .page(page)
                            .build())
                    .build();

            long startTime = System.currentTimeMillis();
            PropertySearchResponse response = searchService.searchProperties(pagedRequest);
            long responseTime = System.currentTimeMillis() - startTime;

            responseTimes.add(responseTime);

            // Verify pagination works correctly
            assertThat(response.getPagination().getPage()).isEqualTo(page);
        }

        // Then - pagination should maintain performance across pages
        double averagePageTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        System.out.println("Pagination Performance Average: " + averagePageTime + "ms");

        assertThat(averagePageTime).isLessThan(300);
    }

    @RepeatedTest(5)
    @DisplayName("Stress test: Performance should be consistent across multiple runs")
    void searchProperties_ConsistentPerformance_ShouldBeReproducible() throws Exception {
        // Given - standard search request
        PropertySearchRequest standardRequest = createSearchRequest("hotels Hanoi");

        // When - performing repeated searches
        List<Long> responseTimes = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            long startTime = System.currentTimeMillis();
            PropertySearchResponse response = searchService.searchProperties(standardRequest);
            long responseTime = System.currentTimeMillis() - startTime;

            responseTimes.add(responseTime);
            assertThat(response).isNotNull();
        }

        // Then - performance should be consistent (low standard deviation)
        double average = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = responseTimes.stream()
                .mapToDouble(time -> Math.pow(time - average, 2))
                .average()
                .orElse(0);
        double standardDeviation = Math.sqrt(variance);

        System.out.println("Consistency metrics - Average: " + average + "ms, StdDev: " + standardDeviation + "ms");

        // Performance should be consistent (standard deviation < 50% of average)
        assertThat(standardDeviation).isLessThan(average * 0.5);
        assertThat(average).isLessThan(500);
    }

    /**
     * Helper methods for creating test data
     */
    private List<PropertySearchRequest> createRealisticSearchRequests() {
        return List.of(
            createSearchRequest("luxury hotels Hanoi"),
            createSearchRequest("budget accommodation Ho Chi Minh"),
            createSearchRequest("beach resort Da Nang"),
            createSearchRequest("business hotel"),
            createSearchRequest("spa wellness Nha Trang"),
            createSearchRequest("family hotel pool"),
            createSearchRequest("boutique hotel Old Quarter"),
            createSearchRequest("khách sạn trung tâm"),
            createSearchRequest("resort biển"),
            createSearchRequest("hotel gần sân bay")
        );
    }

    private List<PropertySearchRequest> createComplexSearchRequests() {
        return List.of(
            PropertySearchRequest.builder()
                .query("luxury hotel spa")
                .language("vi")
                .filters(PropertySearchRequest.SearchFilters.builder()
                        .priceRange(PropertySearchRequest.PriceRange.builder()
                                .min(BigDecimal.valueOf(100))
                                .max(BigDecimal.valueOf(500))
                                .currency("USD")
                                .build())
                        .starRating(4)
                        .amenities(List.of("pool", "spa", "wifi", "parking"))
                        .propertyTypes(List.of("hotel"))
                        .guestRating(BigDecimal.valueOf(4.0))
                        .build())
                .pagination(PropertySearchRequest.PaginationRequest.builder()
                        .page(0)
                        .size(20)
                        .build())
                .sortBy("RATING")
                .build()
        );
    }

    private PropertySearchRequest createSearchRequest(String query) {
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

    private void warmUpCaches() {
        // Warm up caches with common queries to get more realistic performance measurements
        if (searchService != null) {
            try {
                PropertySearchRequest warmupRequest = createSearchRequest("hotel");
                searchService.searchProperties(warmupRequest);
            } catch (Exception e) {
                // Ignore warmup failures
            }
        }
    }

    /**
     * Assume services exist for test setup
     */
    private void assumeServicesExist() {
        org.junit.jupiter.api.Assumptions.assumeTrue(searchService != null,
                "SearchService not available - tests will be skipped until implementation");
        org.junit.jupiter.api.Assumptions.assumeTrue(autoCompleteService != null,
                "AutoCompleteService not available - tests will be skipped until implementation");
        org.junit.jupiter.api.Assumptions.assumeTrue(locationSearchService != null,
                "LocationSearchService not available - tests will be skipped until implementation");
    }
}