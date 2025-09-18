package com.cena.traveloka.search.controller;

import com.cena.traveloka.search.dto.PropertySearchRequest;
import com.cena.traveloka.search.dto.PropertySearchResponse;
import com.cena.traveloka.search.service.SearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract tests for PropertySearchController
 *
 * ⚠️  CRITICAL: These tests MUST FAIL initially (RED phase) until implementation is complete.
 * This is a constitutional requirement for TDD compliance.
 *
 * Testing Strategy:
 * - Contract compliance (request/response structure)
 * - HTTP status codes and error handling
 * - Request validation and constraints
 * - Response format and required fields
 * - Integration with SearchService
 *
 * Prerequisites:
 * - SearchController class exists (currently empty - tests will fail)
 * - PropertySearchRequest DTO exists (to be implemented)
 * - PropertySearchResponse DTO exists (to be implemented)
 * - SearchService interface exists (currently empty - tests will fail)
 *
 * Expected Initial State: FAILING (RED) ❌
 * Expected Final State: PASSING (GREEN) ✅
 */
@WebMvcTest(SearchController.class)
@DisplayName("Property Search Controller Contract Tests")
class PropertySearchControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SearchService searchService;

    private PropertySearchRequest validSearchRequest;
    private PropertySearchResponse mockSearchResponse;

    @BeforeEach
    void setUp() {
        // Setup valid search request - will fail until DTOs are implemented
        validSearchRequest = PropertySearchRequest.builder()
                .query("luxury hotels Hanoi")
                .language("vi")
                .pagination(PropertySearchRequest.PaginationRequest.builder()
                        .page(0)
                        .size(20)
                        .build())
                .sortBy("RELEVANCE")
                .build();

        // Setup mock search response - will fail until DTOs are implemented
        mockSearchResponse = PropertySearchResponse.builder()
                .properties(List.of(
                        createMockPropertyResult("Hotel A", "Hanoi"),
                        createMockPropertyResult("Hotel B", "Hanoi")
                ))
                .pagination(PropertySearchResponse.PaginationResponse.builder()
                        .page(0)
                        .size(20)
                        .totalElements(2L)
                        .totalPages(1)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build())
                .metadata(PropertySearchResponse.SearchMetadata.builder()
                        .responseTime(245)
                        .searchId(UUID.randomUUID())
                        .searchType("FULL_TEXT")
                        .cacheHit(false)
                        .build())
                .build();
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/search/properties - Should accept valid search request and return structured response")
    void searchProperties_ValidRequest_ShouldReturnSearchResults() throws Exception {
        // Given - will fail until SearchService.searchProperties method exists
        when(searchService.searchProperties(any(PropertySearchRequest.class)))
                .thenReturn(mockSearchResponse);

        // When & Then - will fail until SearchController.searchProperties endpoint exists
        mockMvc.perform(post("/api/search/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSearchRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.properties", hasSize(2)))
                .andExpect(jsonPath("$.properties[0].propertyId", notNullValue()))
                .andExpect(jsonPath("$.properties[0].name", equalTo("Hotel A")))
                .andExpect(jsonPath("$.properties[0].location.city", equalTo("Hanoi")))
                .andExpect(jsonPath("$.pagination.page", equalTo(0)))
                .andExpect(jsonPath("$.pagination.size", equalTo(20)))
                .andExpect(jsonPath("$.pagination.totalElements", equalTo(2)))
                .andExpect(jsonPath("$.metadata.responseTime", notNullValue()))
                .andExpect(jsonPath("$.metadata.searchId", notNullValue()))
                .andExpect(jsonPath("$.metadata.searchType", equalTo("FULL_TEXT")));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/search/properties - Should handle location-based search request")
    void searchProperties_LocationBasedRequest_ShouldReturnLocationResults() throws Exception {
        // Given - location-based search request
        PropertySearchRequest locationRequest = PropertySearchRequest.builder()
                .location(PropertySearchRequest.LocationFilter.builder()
                        .coordinates(PropertySearchRequest.Coordinates.builder()
                                .latitude(21.0285)
                                .longitude(105.8542)
                                .build())
                        .radius(5.0)
                        .radiusUnit("KILOMETERS")
                        .build())
                .pagination(PropertySearchRequest.PaginationRequest.builder()
                        .page(0)
                        .size(10)
                        .build())
                .sortBy("DISTANCE")
                .build();

        PropertySearchResponse locationResponse = mockSearchResponse.toBuilder()
                .metadata(mockSearchResponse.getMetadata().toBuilder()
                        .searchType("LOCATION_BASED")
                        .build())
                .build();

        when(searchService.searchProperties(any(PropertySearchRequest.class)))
                .thenReturn(locationResponse);

        // When & Then - will fail until endpoint exists
        mockMvc.perform(post("/api/search/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.properties[0].distance", notNullValue()))
                .andExpect(jsonPath("$.metadata.searchType", equalTo("LOCATION_BASED")));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/search/properties - Should handle advanced filters")
    void searchProperties_WithFilters_ShouldReturnFilteredResults() throws Exception {
        // Given - search request with filters
        PropertySearchRequest filteredRequest = validSearchRequest.toBuilder()
                .filters(PropertySearchRequest.SearchFilters.builder()
                        .priceRange(PropertySearchRequest.PriceRange.builder()
                                .min(BigDecimal.valueOf(50.0))
                                .max(BigDecimal.valueOf(200.0))
                                .currency("USD")
                                .build())
                        .starRating(4)
                        .amenities(Arrays.asList("pool", "wifi", "parking"))
                        .propertyTypes(Arrays.asList("hotel"))
                        .guestRating(BigDecimal.valueOf(4.0))
                        .build())
                .build();

        when(searchService.searchProperties(any(PropertySearchRequest.class)))
                .thenReturn(mockSearchResponse);

        // When & Then - will fail until endpoint and DTOs exist
        mockMvc.perform(post("/api/search/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filteredRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.aggregations", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/search/properties - Should return 400 for invalid request")
    void searchProperties_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given - Invalid request with query too long
        PropertySearchRequest invalidRequest = PropertySearchRequest.builder()
                .query("a".repeat(201)) // Exceeds max length
                .language("invalid") // Invalid language
                .pagination(PropertySearchRequest.PaginationRequest.builder()
                        .page(-1) // Invalid page
                        .size(101) // Exceeds max size
                        .build())
                .build();

        // When & Then - will fail until validation exists
        mockMvc.perform(post("/api/search/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/search/locations - Should support location-based search by coordinates")
    void searchByLocation_ValidCoordinates_ShouldReturnNearbyProperties() throws Exception {
        // Given - will fail until LocationSearchService exists
        when(searchService.searchByLocation(any(), any(), any(), any(), any()))
                .thenReturn(mockSearchResponse);

        // When & Then - will fail until endpoint exists
        mockMvc.perform(get("/api/search/locations")
                        .param("lat", "21.0285")
                        .param("lng", "105.8542")
                        .param("radius", "5.0")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.properties[0].distance", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/search/popular - Should return popular destinations")
    void getPopularDestinations_ShouldReturnTrendingLocations() throws Exception {
        // Given - will fail until PopularDestination service exists
        // When & Then - will fail until endpoint exists
        mockMvc.perform(get("/api/search/popular")
                        .param("countryCode", "VN")
                        .param("type", "CITY")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinations", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.destinations[0].name", notNullValue()))
                .andExpect(jsonPath("$.destinations[0].rank", notNullValue()));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/search/analytics - Should track search interactions")
    void trackSearchAnalytics_ValidEvent_ShouldRecordSuccessfully() throws Exception {
        // Given - analytics request - will fail until DTO exists
        String analyticsRequest = """
                {
                    "eventType": "SEARCH_PERFORMED",
                    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
                    "searchQuery": "luxury hotels Hanoi",
                    "resultCount": 45,
                    "responseTime": 234,
                    "userLocation": {
                        "latitude": 21.0285,
                        "longitude": 105.8542
                    },
                    "deviceInfo": {
                        "deviceType": "MOBILE",
                        "userAgent": "Mozilla/5.0 (Mobile)"
                    }
                }
                """;

        // When & Then - will fail until analytics endpoint exists
        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(analyticsRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recorded", equalTo(true)))
                .andExpect(jsonPath("$.eventId", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/search/suggestions - Should return auto-complete suggestions")
    void getSearchSuggestions_ValidQuery_ShouldReturnSuggestions() throws Exception {
        // When & Then - will fail until auto-complete endpoint exists
        mockMvc.perform(get("/api/search/suggestions")
                        .param("query", "ha noi")
                        .param("language", "vi")
                        .param("limit", "10"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.suggestions", hasSize(greaterThan(0))))
                .andExpected(jsonPath("$.suggestions[0].text", notNullValue()))
                .andExpected(jsonPath("$.suggestions[0].type", notNullValue()));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/search/properties - Should support pagination and sorting")
    void searchProperties_WithPaginationAndSorting_ShouldReturnPaginatedResults() throws Exception {
        // Given - paginated request
        PropertySearchRequest paginatedRequest = validSearchRequest.toBuilder()
                .pagination(PropertySearchRequest.PaginationRequest.builder()
                        .page(1)
                        .size(5)
                        .build())
                .sortBy("PRICE_LOW")
                .build();

        PropertySearchResponse paginatedResponse = mockSearchResponse.toBuilder()
                .pagination(mockSearchResponse.getPagination().toBuilder()
                        .page(1)
                        .size(5)
                        .totalElements(25L)
                        .totalPages(5)
                        .hasNext(true)
                        .hasPrevious(true)
                        .build())
                .build();

        when(searchService.searchProperties(any(PropertySearchRequest.class)))
                .thenReturn(paginatedResponse);

        // When & Then - will fail until implementation exists
        mockMvc.perform(post("/api/search/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paginatedRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.page", equalTo(1)))
                .andExpect(jsonPath("$.pagination.size", equalTo(5)))
                .andExpect(jsonPath("$.pagination.totalElements", equalTo(25)))
                .andExpect(jsonPath("$.pagination.hasNext", equalTo(true)))
                .andExpect(jsonPath("$.pagination.hasPrevious", equalTo(true)));
    }

    @Test
    @WithMockUser
    @DisplayName("Performance requirement: Search response time must be under 500ms")
    void searchProperties_PerformanceTest_ShouldRespondUnder500ms() throws Exception {
        // Given - performance test setup
        when(searchService.searchProperties(any(PropertySearchRequest.class)))
                .thenReturn(mockSearchResponse.toBuilder()
                        .metadata(mockSearchResponse.getMetadata().toBuilder()
                                .responseTime(245) // Under 500ms requirement
                                .build())
                        .build());

        long startTime = System.currentTimeMillis();

        // When & Then - will fail until performance optimization exists
        mockMvc.perform(post("/api/search/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSearchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.responseTime", lessThan(500)));

        long responseTime = System.currentTimeMillis() - startTime;

        // Assert total response time including network is under requirement
        assert responseTime < 500 : "Search response time " + responseTime + "ms exceeds 500ms requirement";
    }

    /**
     * Helper method to create mock property search results
     * Will fail until PropertySearchResponse.PropertySearchResult is implemented
     */
    private PropertySearchResponse.PropertySearchResult createMockPropertyResult(String name, String city) {
        return PropertySearchResponse.PropertySearchResult.builder()
                .propertyId(UUID.randomUUID())
                .name(name)
                .location(PropertySearchResponse.PropertyLocation.builder()
                        .coordinates(PropertySearchResponse.Coordinates.builder()
                                .latitude(21.0285)
                                .longitude(105.8542)
                                .build())
                        .city(city)
                        .countryCode("VN")
                        .address("123 Test Street")
                        .build())
                .priceRange(PropertySearchResponse.PriceRange.builder()
                        .min(BigDecimal.valueOf(50.0))
                        .max(BigDecimal.valueOf(150.0))
                        .currency("USD")
                        .build())
                .rating(PropertySearchResponse.PropertyRating.builder()
                        .average(BigDecimal.valueOf(4.2))
                        .count(156)
                        .build())
                .starRating(4)
                .propertyType("hotel")
                .availability(PropertySearchResponse.AvailabilityInfo.builder()
                        .isAvailable(true)
                        .totalRooms(50)
                        .availableRooms(12)
                        .build())
                .distance(2.3) // Distance in kilometers
                .relevanceScore(0.95)
                .build();
    }
}