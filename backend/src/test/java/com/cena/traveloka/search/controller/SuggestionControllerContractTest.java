package com.cena.traveloka.search.controller;

import com.cena.traveloka.search.dto.SuggestionResponse;
import com.cena.traveloka.search.service.AutoCompleteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract tests for SuggestionController
 *
 * ⚠️  CRITICAL: These tests MUST FAIL initially (RED phase) until implementation is complete.
 * This is a constitutional requirement for TDD compliance.
 *
 * Testing Strategy:
 * - Auto-complete suggestion functionality
 * - Multi-language support (Vietnamese/English)
 * - Fuzzy matching capabilities
 * - Query parameter validation
 * - Response format compliance
 * - Performance requirements (sub-100ms for suggestions)
 *
 * Prerequisites:
 * - SuggestionController class exists (currently empty - tests will fail)
 * - SuggestionResponse DTO exists (to be implemented)
 * - AutoCompleteService interface exists (currently empty - tests will fail)
 *
 * Expected Initial State: FAILING (RED) ❌
 * Expected Final State: PASSING (GREEN) ✅
 */
@WebMvcTest(SuggestionController.class)
@DisplayName("Suggestion Controller Contract Tests")
class SuggestionControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AutoCompleteService autoCompleteService;

    private SuggestionResponse mockSuggestionResponse;

    @BeforeEach
    void setUp() {
        // Setup mock suggestion response - will fail until DTO is implemented
        mockSuggestionResponse = SuggestionResponse.builder()
                .suggestions(Arrays.asList(
                        createMockSuggestion("Hà Nội", "CITY", Arrays.asList("Hanoi", "Ha Noi")),
                        createMockSuggestion("Hanoi Hilton", "HOTEL", Arrays.asList("Hanoi Hilton Hotel")),
                        createMockSuggestion("Hải Phòng", "CITY", Arrays.asList("Hai Phong", "Haiphong"))
                ))
                .metadata(SuggestionResponse.Metadata.builder()
                        .responseTime(15)
                        .suggestionCount(3)
                        .build())
                .build();
    }

    @Test
    @DisplayName("GET /api/search/suggestions - Should return suggestions for valid Vietnamese query")
    void getSuggestions_ValidVietnameseQuery_ShouldReturnSuggestions() throws Exception {
        // Given - will fail until AutoCompleteService.getSuggestions method exists
        when(autoCompleteService.getSuggestions(eq("hà nội"), any(), any(), any(), anyInt()))
                .thenReturn(mockSuggestionResponse);

        // When & Then - will fail until SuggestionController endpoint exists
        mockMvc.perform(get("/api/search/suggestions")
                        .param("query", "hà nội")
                        .param("language", "vi")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.suggestions", hasSize(3)))
                .andExpect(jsonPath("$.suggestions[0].text", equalTo("Hà Nội")))
                .andExpect(jsonPath("$.suggestions[0].type", equalTo("CITY")))
                .andExpect(jsonPath("$.suggestions[0].highlights", hasSize(2)))
                .andExpect(jsonPath("$.metadata.responseTime", notNullValue()))
                .andExpect(jsonPath("$.metadata.suggestionCount", equalTo(3)));
    }

    @Test
    @DisplayName("GET /api/search/suggestions - Should support English language")
    void getSuggestions_EnglishLanguage_ShouldReturnEnglishSuggestions() throws Exception {
        // Given - English suggestions
        SuggestionResponse englishResponse = SuggestionResponse.builder()
                .suggestions(Arrays.asList(
                        createMockSuggestion("Hanoi", "CITY", Arrays.asList("Hanoi")),
                        createMockSuggestion("Hanoi Opera House", "LANDMARK", Arrays.asList("Opera House"))
                ))
                .metadata(SuggestionResponse.Metadata.builder()
                        .responseTime(12)
                        .suggestionCount(2)
                        .build())
                .build();

        when(autoCompleteService.getSuggestions(eq("hanoi"), any(), any(), any(), anyInt()))
                .thenReturn(englishResponse);

        // When & Then - will fail until endpoint exists
        mockMvc.perform(get("/api/search/suggestions")
                        .param("query", "hanoi")
                        .param("language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions", hasSize(2)))
                .andExpect(jsonPath("$.suggestions[0].text", equalTo("Hanoi")));
    }

    @Test
    @DisplayName("GET /api/search/suggestions - Should support type filtering")
    void getSuggestions_WithTypeFilter_ShouldReturnFilteredSuggestions() throws Exception {
        // Given - hotel-only suggestions
        SuggestionResponse hotelResponse = SuggestionResponse.builder()
                .suggestions(Arrays.asList(
                        createMockSuggestion("Hanoi Hilton", "HOTEL", Arrays.asList("Hilton")),
                        createMockSuggestion("Hanoi Hotel", "HOTEL", Arrays.asList("Hotel"))
                ))
                .metadata(SuggestionResponse.Metadata.builder()
                        .responseTime(18)
                        .suggestionCount(2)
                        .build())
                .build();

        when(autoCompleteService.getSuggestions(eq("hanoi"), any(), any(), any(), anyInt()))
                .thenReturn(hotelResponse);

        // When & Then - will fail until endpoint and type filtering exist
        mockMvc.perform(get("/api/search/suggestions")
                        .param("query", "hanoi")
                        .param("types", "HOTEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions", hasSize(2)))
                .andExpect(jsonPath("$.suggestions[0].type", equalTo("HOTEL")))
                .andExpect(jsonPath("$.suggestions[1].type", equalTo("HOTEL")));
    }

    @Test
    @DisplayName("GET /api/search/suggestions - Should support location-based suggestions")
    void getSuggestions_WithLocation_ShouldReturnLocationBasedSuggestions() throws Exception {
        // Given - location-based suggestions
        when(autoCompleteService.getSuggestions(eq("hotel"), any(), any(), any(), anyInt()))
                .thenReturn(mockSuggestionResponse);

        // When & Then - will fail until location-based suggestion logic exists
        mockMvc.perform(get("/api/search/suggestions")
                        .param("query", "hotel")
                        .param("location", "21.0285,105.8542"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("GET /api/search/suggestions - Should handle diacritic variations (Vietnamese)")
    void getSuggestions_VietnameseDiacritics_ShouldNormalizeProperly() throws Exception {
        // Given - various diacritic inputs should return same results
        when(autoCompleteService.getSuggestions(anyString(), any(), any(), any(), anyInt()))
                .thenReturn(mockSuggestionResponse);

        // Test variations: "Hà Nội", "Ha Noi", "ha noi", "hà nội"
        String[] variations = {"Hà Nội", "Ha Noi", "ha noi", "hà nội"};

        for (String variation : variations) {
            // When & Then - will fail until diacritic normalization exists
            mockMvc.perform(get("/api/search/suggestions")
                            .param("query", variation)
                            .param("language", "vi"))
                    .andExpect(status().isOk())
                    .andExpected(jsonPath("$.suggestions", hasSize(greaterThan(0))))
                    .andExpected(jsonPath("$.suggestions[0].text", containsString("Nội")));
        }
    }

    @Test
    @DisplayName("GET /api/search/suggestions - Should handle fuzzy matching for typos")
    void getSuggestions_FuzzyMatching_ShouldReturnSimilarSuggestions() throws Exception {
        // Given - fuzzy matching for misspelled queries
        SuggestionResponse fuzzyResponse = SuggestionResponse.builder()
                .suggestions(Arrays.asList(
                        createMockSuggestion("Hà Nội", "CITY", Arrays.asList("Ha Noi", "Hanoi")),
                        createMockSuggestion("Hải Phòng", "CITY", Arrays.asList("Hai Phong"))
                ))
                .metadata(SuggestionResponse.Metadata.builder()
                        .responseTime(22)
                        .suggestionCount(2)
                        .build())
                .build();

        when(autoCompleteService.getSuggestions(eq("hanoy"), any(), any(), any(), anyInt()))
                .thenReturn(fuzzyResponse);

        // When & Then - will fail until fuzzy matching exists
        mockMvc.perform(get("/api/search/suggestions")
                        .param("query", "hanoy")) // Misspelled "hanoi"
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.suggestions", hasSize(2)))
                .andExpected(jsonPath("$.suggestions[0].text", containsString("Nội")));
    }

    @Test
    @DisplayName("GET /api/search/suggestions - Should validate query parameters")
    void getSuggestions_InvalidParameters_ShouldReturnBadRequest() throws Exception {
        // Test empty query
        mockMvc.perform(get("/api/search/suggestions")
                        .param("query", ""))
                .andExpected(status().isBadRequest())
                .andExpected(jsonPath("$.error", notNullValue()));

        // Test missing query
        mockMvc.perform(get("/api/search/suggestions"))
                .andExpected(status().isBadRequest());

        // Test query too long
        mockMvc.perform(get("/api/search/suggestions")
                        .param("query", "a".repeat(101))) // Exceeds max length
                .andExpected(status().isBadRequest());

        // Test invalid language
        mockMvc.perform(get("/api/search/suggestions")
                        .param("query", "hanoi")
                        .param("language", "invalid"))
                .andExpected(status().isBadRequest());

        // Test invalid limit
        mockMvc.perform(get("/api/search/suggestions")
                        .param("query", "hanoi")
                        .param("limit", "0")) // Below minimum
                .andExpected(status().isBadRequest());

        mockMvc.perform(get("/api/search/suggestions")
                        .param("query", "hanoi")
                        .param("limit", "25")) // Above maximum
                .andExpected(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/search/suggestions - Should validate location format")
    void getSuggestions_InvalidLocation_ShouldReturnBadRequest() throws Exception {
        // Test invalid location format
        mockMvc.perform(get("/api/search/suggestions")
                        .param("query", "hotel")
                        .param("location", "invalid-format"))
                .andExpected(status().isBadRequest());

        // Test out of range coordinates
        mockMvc.perform(get("/api/search/suggestions")
                        .param("query", "hotel")
                        .param("location", "91.0,181.0")) // Invalid lat/lng
                .andExpected(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/search/suggestions - Should use default parameters when not provided")
    void getSuggestions_DefaultParameters_ShouldUseDefaults() throws Exception {
        // Given - should use default language (vi) and limit (10)
        when(autoCompleteService.getSuggestions(eq("hanoi"), eq("vi"), eq(null), eq(null), eq(10)))
                .thenReturn(mockSuggestionResponse);

        // When & Then - will fail until default parameter handling exists
        mockMvc.perform(get("/api/search/suggestions")
                        .param("query", "hanoi"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.suggestions", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("GET /api/search/suggestions - Should return empty suggestions when no matches")
    void getSuggestions_NoMatches_ShouldReturnEmptyResponse() throws Exception {
        // Given - no matches for gibberish query
        SuggestionResponse emptyResponse = SuggestionResponse.builder()
                .suggestions(List.of())
                .metadata(SuggestionResponse.Metadata.builder()
                        .responseTime(8)
                        .suggestionCount(0)
                        .build())
                .build();

        when(autoCompleteService.getSuggestions(eq("xyzzyx"), any(), any(), any(), anyInt()))
                .thenReturn(emptyResponse);

        // When & Then - will fail until implementation exists
        mockMvc.perform(get("/api/search/suggestions")
                        .param("query", "xyzzyx"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.suggestions", hasSize(0)))
                .andExpected(jsonPath("$.metadata.suggestionCount", equalTo(0)));
    }

    @Test
    @DisplayName("Performance requirement: Suggestion response time must be under 100ms")
    void getSuggestions_PerformanceTest_ShouldRespondUnder100ms() throws Exception {
        // Given - performance-optimized response
        SuggestionResponse fastResponse = mockSuggestionResponse.toBuilder()
                .metadata(mockSuggestionResponse.getMetadata().toBuilder()
                        .responseTime(45) // Well under 100ms requirement
                        .build())
                .build();

        when(autoCompleteService.getSuggestions(anyString(), any(), any(), any(), anyInt()))
                .thenReturn(fastResponse);

        long startTime = System.currentTimeMillis();

        // When & Then - will fail until performance optimization exists
        mockMvc.perform(get("/api/search/suggestions")
                        .param("query", "hanoi"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.metadata.responseTime", lessThan(100)));

        long responseTime = System.currentTimeMillis() - startTime;

        // Assert total response time is under requirement
        assert responseTime < 100 : "Suggestion response time " + responseTime + "ms exceeds 100ms requirement";
    }

    @Test
    @DisplayName("GET /api/search/suggestions - Should handle high-frequency requests")
    void getSuggestions_HighFrequency_ShouldHandleLoad() throws Exception {
        // Given - simulate rapid suggestion requests
        when(autoCompleteService.getSuggestions(anyString(), any(), any(), any(), anyInt()))
                .thenReturn(mockSuggestionResponse);

        // When & Then - will fail until rate limiting and caching exist
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/search/suggestions")
                            .param("query", "test" + i))
                    .andExpected(status().isOk());
        }
    }

    @Test
    @DisplayName("GET /api/search/suggestions - Should support mixed-language queries")
    void getSuggestions_MixedLanguage_ShouldHandleBothLanguages() throws Exception {
        // Given - mixed Vietnamese-English query
        SuggestionResponse mixedResponse = SuggestionResponse.builder()
                .suggestions(Arrays.asList(
                        createMockSuggestion("Hanoi Hotel Hà Nội", "HOTEL", Arrays.asList("Hotel", "Hà Nội")),
                        createMockSuggestion("Luxury Resort Da Nang", "HOTEL", Arrays.asList("Resort", "Da Nang"))
                ))
                .metadata(SuggestionResponse.Metadata.builder()
                        .responseTime(25)
                        .suggestionCount(2)
                        .build())
                .build();

        when(autoCompleteService.getSuggestions(eq("hotel hà nội"), any(), any(), any(), anyInt()))
                .thenReturn(mixedResponse);

        // When & Then - will fail until mixed-language support exists
        mockMvc.perform(get("/api/search/suggestions")
                        .param("query", "hotel hà nội"))
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.suggestions", hasSize(2)))
                .andExpected(jsonPath("$.suggestions[0].text", containsString("Hotel")))
                .andExpected(jsonPath("$.suggestions[0].text", containsString("Hà Nội")));
    }

    /**
     * Helper method to create mock suggestion objects
     * Will fail until SuggestionResponse.SearchSuggestion is implemented
     */
    private SuggestionResponse.SearchSuggestion createMockSuggestion(String text, String type, List<String> highlights) {
        return SuggestionResponse.SearchSuggestion.builder()
                .text(text)
                .type(type)
                .highlights(highlights)
                .location(SuggestionResponse.Coordinates.builder()
                        .latitude(21.0285)
                        .longitude(105.8542)
                        .build())
                .additionalInfo(SuggestionResponse.AdditionalInfo.builder()
                        .propertyCount(type.equals("CITY") ? 150 : null)
                        .starRating(type.equals("HOTEL") ? 4 : null)
                        .build())
                .build();
    }
}