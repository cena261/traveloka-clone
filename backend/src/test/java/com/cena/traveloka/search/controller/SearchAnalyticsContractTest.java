package com.cena.traveloka.search.controller;

import com.cena.traveloka.search.dto.SearchAnalyticsRequest;
import com.cena.traveloka.search.dto.AnalyticsResponse;
import com.cena.traveloka.search.service.SearchAnalyticsService;
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

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract tests for Search Analytics Controller
 *
 * ⚠️  CRITICAL: These tests MUST FAIL initially (RED phase) until implementation is complete.
 * This is a constitutional requirement for TDD compliance.
 *
 * Testing Strategy:
 * - Analytics event tracking validation
 * - Search interaction recording
 * - User behavior pattern capture
 * - Performance metrics collection
 * - Privacy and security compliance
 * - Database integration with search schema
 *
 * Prerequisites:
 * - SearchController class exists (currently empty - tests will fail)
 * - SearchAnalyticsRequest DTO exists (to be implemented)
 * - AnalyticsResponse DTO exists (to be implemented)
 * - SearchAnalyticsService interface exists (currently empty - tests will fail)
 * - Database schema V10__search_analytics_tables.sql applied
 *
 * Expected Initial State: FAILING (RED) ❌
 * Expected Final State: PASSING (GREEN) ✅
 */
@WebMvcTest(SearchController.class)
@DisplayName("Search Analytics Controller Contract Tests")
class SearchAnalyticsContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SearchAnalyticsService searchAnalyticsService;

    private SearchAnalyticsRequest validAnalyticsRequest;
    private AnalyticsResponse mockAnalyticsResponse;

    @BeforeEach
    void setUp() {
        // Setup valid analytics request - will fail until DTO is implemented
        validAnalyticsRequest = SearchAnalyticsRequest.builder()
                .eventType("SEARCH_PERFORMED")
                .sessionId(UUID.randomUUID())
                .searchQuery("luxury hotels Hanoi")
                .resultCount(45)
                .responseTime(234)
                .userLocation(SearchAnalyticsRequest.Coordinates.builder()
                        .latitude(21.0285)
                        .longitude(105.8542)
                        .build())
                .deviceInfo(SearchAnalyticsRequest.DeviceInfo.builder()
                        .deviceType("MOBILE")
                        .userAgent("Mozilla/5.0 (Mobile; Vietnamese)")
                        .build())
                .build();

        // Setup mock analytics response - will fail until DTO is implemented
        mockAnalyticsResponse = AnalyticsResponse.builder()
                .recorded(true)
                .eventId(UUID.randomUUID())
                .build();
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/search/analytics - Should record SEARCH_PERFORMED event")
    void trackAnalytics_SearchPerformed_ShouldRecordEvent() throws Exception {
        // Given - will fail until SearchAnalyticsService.trackSearchEvent method exists
        when(searchAnalyticsService.trackSearchEvent(any(SearchAnalyticsRequest.class)))
                .thenReturn(mockAnalyticsResponse);

        // When & Then - will fail until SearchController analytics endpoint exists
        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAnalyticsRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.recorded", equalTo(true)))
                .andExpect(jsonPath("$.eventId", notNullValue()));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/search/analytics - Should record PROPERTY_CLICKED event with position")
    void trackAnalytics_PropertyClicked_ShouldRecordClickEvent() throws Exception {
        // Given - property click event with position tracking
        SearchAnalyticsRequest clickRequest = SearchAnalyticsRequest.builder()
                .eventType("PROPERTY_CLICKED")
                .sessionId(UUID.randomUUID())
                .propertyId(UUID.randomUUID())
                .clickPosition(3)
                .searchQuery("beach resorts Da Nang")
                .userLocation(validAnalyticsRequest.getUserLocation())
                .deviceInfo(validAnalyticsRequest.getDeviceInfo())
                .build();

        when(searchAnalyticsService.trackSearchEvent(any(SearchAnalyticsRequest.class)))
                .thenReturn(mockAnalyticsResponse);

        // When & Then - will fail until click tracking exists
        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clickRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recorded", equalTo(true)))
                .andExpect(jsonPath("$.eventId", notNullValue()));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/search/analytics - Should record FILTER_APPLIED event with filter details")
    void trackAnalytics_FilterApplied_ShouldRecordFilterEvent() throws Exception {
        // Given - filter applied event with detailed filter information
        SearchAnalyticsRequest filterRequest = SearchAnalyticsRequest.builder()
                .eventType("FILTER_APPLIED")
                .sessionId(UUID.randomUUID())
                .searchQuery("hotels Hanoi")
                .filters(SearchAnalyticsRequest.SearchFilters.builder()
                        .priceRange(SearchAnalyticsRequest.PriceRange.builder()
                                .min(50.0)
                                .max(200.0)
                                .currency("USD")
                                .build())
                        .starRating(4)
                        .amenities(java.util.Arrays.asList("pool", "wifi", "spa"))
                        .propertyTypes(java.util.Arrays.asList("hotel"))
                        .build())
                .resultCount(23)
                .responseTime(156)
                .userLocation(validAnalyticsRequest.getUserLocation())
                .deviceInfo(validAnalyticsRequest.getDeviceInfo())
                .build();

        when(searchAnalyticsService.trackSearchEvent(any(SearchAnalyticsRequest.class)))
                .thenReturn(mockAnalyticsResponse);

        // When & Then - will fail until filter tracking exists
        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filterRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recorded", equalTo(true)));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/search/analytics - Should record BOOKING_INITIATED event for conversion tracking")
    void trackAnalytics_BookingInitiated_ShouldRecordBookingEvent() throws Exception {
        // Given - booking conversion event
        SearchAnalyticsRequest bookingRequest = SearchAnalyticsRequest.builder()
                .eventType("BOOKING_INITIATED")
                .sessionId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .propertyId(UUID.randomUUID())
                .searchQuery("luxury hotels Hanoi")
                .conversionValue(250.00)
                .userLocation(validAnalyticsRequest.getUserLocation())
                .deviceInfo(validAnalyticsRequest.getDeviceInfo())
                .build();

        when(searchAnalyticsService.trackSearchEvent(any(SearchAnalyticsRequest.class)))
                .thenReturn(mockAnalyticsResponse);

        // When & Then - will fail until booking conversion tracking exists
        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recorded", equalTo(true)));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/search/analytics - Should record SUGGESTION_SELECTED event")
    void trackAnalytics_SuggestionSelected_ShouldRecordSuggestionEvent() throws Exception {
        // Given - auto-complete suggestion selection event
        SearchAnalyticsRequest suggestionRequest = SearchAnalyticsRequest.builder()
                .eventType("SUGGESTION_SELECTED")
                .sessionId(UUID.randomUUID())
                .searchQuery("hà nội") // Original partial query
                .selectedSuggestion("Hà Nội hotels") // What user selected
                .userLocation(validAnalyticsRequest.getUserLocation())
                .deviceInfo(validAnalyticsRequest.getDeviceInfo())
                .build();

        when(searchAnalyticsService.trackSearchEvent(any(SearchAnalyticsRequest.class)))
                .thenReturn(mockAnalyticsResponse);

        // When & Then - will fail until suggestion tracking exists
        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(suggestionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recorded", equalTo(true)));
    }

    @Test
    @DisplayName("POST /api/search/analytics - Should validate required fields")
    void trackAnalytics_MissingRequiredFields_ShouldReturnBadRequest() throws Exception {
        // Test missing eventType
        SearchAnalyticsRequest invalidRequest1 = SearchAnalyticsRequest.builder()
                .sessionId(UUID.randomUUID())
                .searchQuery("test")
                .build();

        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()))
                .andExpect(jsonPath("$.message", containsString("eventType")));

        // Test missing sessionId
        SearchAnalyticsRequest invalidRequest2 = SearchAnalyticsRequest.builder()
                .eventType("SEARCH_PERFORMED")
                .searchQuery("test")
                .build();

        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()))
                .andExpect(jsonPath("$.message", containsString("sessionId")));
    }

    @Test
    @DisplayName("POST /api/search/analytics - Should validate event type")
    void trackAnalytics_InvalidEventType_ShouldReturnBadRequest() throws Exception {
        // Given - invalid event type
        SearchAnalyticsRequest invalidRequest = validAnalyticsRequest.toBuilder()
                .eventType("INVALID_EVENT_TYPE")
                .build();

        // When & Then - will fail until validation exists
        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()))
                .andExpect(jsonPath("$.message", containsString("eventType")));
    }

    @Test
    @DisplayName("POST /api/search/analytics - Should validate coordinate ranges")
    void trackAnalytics_InvalidCoordinates_ShouldReturnBadRequest() throws Exception {
        // Test invalid latitude
        SearchAnalyticsRequest invalidLatRequest = validAnalyticsRequest.toBuilder()
                .userLocation(SearchAnalyticsRequest.Coordinates.builder()
                        .latitude(91.0) // Invalid latitude > 90
                        .longitude(105.8542)
                        .build())
                .build();

        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLatRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()));

        // Test invalid longitude
        SearchAnalyticsRequest invalidLngRequest = validAnalyticsRequest.toBuilder()
                .userLocation(SearchAnalyticsRequest.Coordinates.builder()
                        .latitude(21.0285)
                        .longitude(181.0) // Invalid longitude > 180
                        .build())
                .build();

        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLngRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/search/analytics - Should validate device type")
    void trackAnalytics_InvalidDeviceType_ShouldReturnBadRequest() throws Exception {
        // Given - invalid device type
        SearchAnalyticsRequest invalidDeviceRequest = validAnalyticsRequest.toBuilder()
                .deviceInfo(SearchAnalyticsRequest.DeviceInfo.builder()
                        .deviceType("INVALID_DEVICE") // Must be MOBILE, DESKTOP, or TABLET
                        .userAgent("Mozilla/5.0")
                        .build())
                .build();

        // When & Then - will fail until device type validation exists
        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDeviceRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/search/analytics - Should validate response time is non-negative")
    void trackAnalytics_NegativeResponseTime_ShouldReturnBadRequest() throws Exception {
        // Given - negative response time
        SearchAnalyticsRequest invalidTimeRequest = validAnalyticsRequest.toBuilder()
                .responseTime(-100) // Negative response time
                .build();

        // When & Then - will fail until validation exists
        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTimeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/search/analytics - Should validate result count is non-negative")
    void trackAnalytics_NegativeResultCount_ShouldReturnBadRequest() throws Exception {
        // Given - negative result count
        SearchAnalyticsRequest invalidCountRequest = validAnalyticsRequest.toBuilder()
                .resultCount(-5) // Negative result count
                .build();

        // When & Then - will fail until validation exists
        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCountRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/search/analytics - Should handle anonymous user events")
    void trackAnalytics_AnonymousUser_ShouldRecordEvent() throws Exception {
        // Given - anonymous user event (no userId)
        SearchAnalyticsRequest anonymousRequest = validAnalyticsRequest.toBuilder()
                .userId(null) // Anonymous user
                .build();

        when(searchAnalyticsService.trackSearchEvent(any(SearchAnalyticsRequest.class)))
                .thenReturn(mockAnalyticsResponse);

        // When & Then - will fail until anonymous user tracking exists
        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(anonymousRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recorded", equalTo(true)));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/search/analytics - Should validate click position for PROPERTY_CLICKED events")
    void trackAnalytics_PropertyClickedMissingPosition_ShouldReturnBadRequest() throws Exception {
        // Given - property clicked event without position
        SearchAnalyticsRequest clickWithoutPosition = SearchAnalyticsRequest.builder()
                .eventType("PROPERTY_CLICKED")
                .sessionId(UUID.randomUUID())
                .propertyId(UUID.randomUUID())
                // Missing clickPosition for click event
                .searchQuery("hotels")
                .build();

        // When & Then - will fail until business rule validation exists
        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clickWithoutPosition)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/search/analytics - Should handle Vietnamese search queries properly")
    void trackAnalytics_VietnameseQuery_ShouldHandleUnicodeCorrectly() throws Exception {
        // Given - Vietnamese search query with diacritics
        SearchAnalyticsRequest vietnameseRequest = validAnalyticsRequest.toBuilder()
                .searchQuery("khách sạn cao cấp Hồ Chí Minh")
                .language("vi")
                .build();

        when(searchAnalyticsService.trackSearchEvent(any(SearchAnalyticsRequest.class)))
                .thenReturn(mockAnalyticsResponse);

        // When & Then - will fail until Vietnamese text handling exists
        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vietnameseRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recorded", equalTo(true)));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/search/analytics - Should handle high-frequency analytics events")
    void trackAnalytics_HighFrequency_ShouldHandleLoad() throws Exception {
        // Given - high-frequency analytics events
        when(searchAnalyticsService.trackSearchEvent(any(SearchAnalyticsRequest.class)))
                .thenReturn(mockAnalyticsResponse);

        // When & Then - will fail until high-frequency handling exists
        for (int i = 0; i < 10; i++) {
            SearchAnalyticsRequest rapidRequest = validAnalyticsRequest.toBuilder()
                    .sessionId(UUID.randomUUID())
                    .searchQuery("rapid test " + i)
                    .build();

            mockMvc.perform(post("/api/search/analytics")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rapidRequest)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/search/analytics - Should handle malformed JSON gracefully")
    void trackAnalytics_MalformedJson_ShouldReturnBadRequest() throws Exception {
        // When & Then - will fail until error handling exists
        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/search/analytics - Should handle service exceptions gracefully")
    void trackAnalytics_ServiceException_ShouldReturnInternalServerError() throws Exception {
        // Given - service throws exception
        when(searchAnalyticsService.trackSearchEvent(any(SearchAnalyticsRequest.class)))
                .thenThrow(new RuntimeException("Analytics service unavailable"));

        // When & Then - will fail until exception handling exists
        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAnalyticsRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    @WithMockUser
    @DisplayName("Database Integration: Analytics should persist to search.search_history table")
    void trackAnalytics_DatabaseIntegration_ShouldPersistToSearchSchema() throws Exception {
        // Given - analytics event that should be persisted to search.search_history
        SearchAnalyticsRequest dbRequest = validAnalyticsRequest.toBuilder()
                .searchQuery("database integration test")
                .build();

        when(searchAnalyticsService.trackSearchEvent(any(SearchAnalyticsRequest.class)))
                .thenReturn(mockAnalyticsResponse);

        // When & Then - will fail until database integration exists
        mockMvc.perform(post("/api/search/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dbRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recorded", equalTo(true)))
                .andExpect(jsonPath("$.eventId", notNullValue()));

        // Note: Integration test will verify actual database persistence
        // This contract test only verifies the endpoint exists and responds correctly
    }
}