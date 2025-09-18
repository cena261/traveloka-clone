package com.cena.traveloka.search.controller;

import com.cena.traveloka.search.dto.PropertySearchRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class SearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("T034.C1: Search properties endpoint integration test")
    void testSearchPropertiesEndpoint() throws Exception {
        // Given
        PropertySearchRequest request = PropertySearchRequest.builder()
            .query("hotel")
            .language("vi")
            .build();

        // When & Then
        mockMvc.perform(post("/api/search/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .param("page", "0")
                .param("size", "10")
                .param("sort", "score,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.results").exists())
                .andExpect(jsonPath("$.pagination").exists())
                .andExpect(jsonPath("$.metadata").exists())
                .andExpect(jsonPath("$.metadata.responseTimeMs").isNumber());
    }

    @Test
    @DisplayName("T034.C2: Get available filters endpoint integration test")
    void testGetAvailableFiltersEndpoint() throws Exception {
        mockMvc.perform(get("/api/search/filters")
                .param("query", "hotel")
                .param("language", "vi"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.priceRanges").exists())
                .andExpect(jsonPath("$.starRatings").exists())
                .andExpect(jsonPath("$.propertyTypes").exists())
                .andExpect(jsonPath("$.amenities").exists());
    }

    @Test
    @DisplayName("T034.C3: Get filter counts endpoint integration test")
    void testGetFilterCountsEndpoint() throws Exception {
        // Given
        PropertySearchRequest request = PropertySearchRequest.builder()
            .query("hotel")
            .language("vi")
            .build();

        // When & Then
        mockMvc.perform(post("/api/search/filters/counts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.priceRangeCounts").exists())
                .andExpect(jsonPath("$.starRatingCounts").exists())
                .andExpect(jsonPath("$.propertyTypeCounts").exists());
    }

    @Test
    @DisplayName("T034.C4: Invalid search request handling test")
    void testInvalidSearchRequestHandling() throws Exception {
        // Given - Invalid request with negative price
        PropertySearchRequest invalidRequest = PropertySearchRequest.builder()
            .query("hotel")
            .price(PropertySearchRequest.PriceFilter.builder()
                .minPrice(BigDecimal.valueOf(-100))
                .maxPrice(BigDecimal.valueOf(50))
                .build())
            .build();

        // When & Then
        mockMvc.perform(post("/api/search/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("T034.C5: Search analytics endpoints integration test")
    void testSearchAnalyticsEndpoints() throws Exception {
        // Test popular queries endpoint
        mockMvc.perform(get("/api/search/analytics/popular-queries")
                .param("days", "30")
                .param("minCount", "5")
                .param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        // Test trending queries endpoint
        mockMvc.perform(get("/api/search/analytics/trending-queries")
                .param("recentDays", "7")
                .param("historicalDays", "30")
                .param("minRecentCount", "3")
                .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("T034.C6: Pagination and sorting test")
    void testPaginationAndSorting() throws Exception {
        PropertySearchRequest request = PropertySearchRequest.builder()
            .query("hotel")
            .language("vi")
            .build();

        // Test different page sizes and sorting
        mockMvc.perform(post("/api/search/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .param("page", "1")
                .param("size", "5")
                .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.size").value(5));

        // Test maximum page size limit
        mockMvc.perform(post("/api/search/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .param("size", "200")) // Should be limited to 100
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("T034.C7: Content type and encoding test")
    void testContentTypeAndEncoding() throws Exception {
        PropertySearchRequest request = PropertySearchRequest.builder()
            .query("khách sạn") // Vietnamese text
            .language("vi")
            .build();

        mockMvc.perform(post("/api/search/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().encoding("UTF-8"));
    }
}