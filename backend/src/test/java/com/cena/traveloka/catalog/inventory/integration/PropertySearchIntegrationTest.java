package com.cena.traveloka.catalog.inventory.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

/**
 * T011: Integration test for property spatial search with PostGIS radius queries
 * Tests location-based search functionality with real PostGIS spatial operations
 * These tests MUST FAIL until spatial search implementation is complete
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
class PropertySearchIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgis/postgis:16-3.4")
            .withDatabaseName("traveloka_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSpatialSearchWithinRadius() throws Exception {
        // Create properties at different locations
        String partnerId = createTestPartner();

        // Bangkok city center (test location)
        createPropertyAtLocation(partnerId, "Bangkok Center Hotel", 13.7563, 100.5018);

        // Nearby location (within 5km)
        createPropertyAtLocation(partnerId, "Bangkok Near Hotel", 13.7600, 100.5050);

        // Far location (outside 5km radius)
        createPropertyAtLocation(partnerId, "Bangkok Far Hotel", 13.8000, 100.6000);

        // Test spatial search within 5km radius
        mockMvc.perform(get("/api/v1/properties")
                        .param("lat", "13.7563")
                        .param("lng", "100.5018")
                        .param("radius", "5"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2)); // Should find 2 properties within 5km

        // Test wider search radius (20km)
        mockMvc.perform(get("/api/v1/properties")
                        .param("lat", "13.7563")
                        .param("lng", "100.5018")
                        .param("radius", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3)); // Should find all 3 properties
    }

    private String createTestPartner() throws Exception {
        var partnerRequest = """
            {
                "name": "Test Search Partner",
                "email": "search@partner.com",
                "phone": "+1-555-0123",
                "businessRegistrationNumber": "SEARCH-001",
                "contractStartDate": "2024-01-01",
                "contractEndDate": "2026-12-31",
                "commissionRate": 15.00
            }
            """;

        var result = mockMvc.perform(post("/api/v1/partners")
                        .contentType("application/json")
                        .content(partnerRequest))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText();
    }

    private void createPropertyAtLocation(String partnerId, String name, double lat, double lng) throws Exception {
        var propertyRequest = String.format("""
            {
                "partnerId": "%s",
                "propertyTypeId": "123e4567-e89b-12d3-a456-426614174001",
                "name": "%s",
                "description": {"en": "Test property description"},
                "starRating": 4,
                "countryCode": "TH",
                "city": "Bangkok",
                "addressLine": "123 Test Road",
                "latitude": %f,
                "longitude": %f,
                "phoneNumber": "+66-2-123-4567",
                "email": "test@property.com",
                "checkInTime": "15:00",
                "checkOutTime": "11:00",
                "totalRooms": 100
            }
            """, partnerId, name, lat, lng);

        mockMvc.perform(post("/api/v1/properties")
                        .contentType("application/json")
                        .content(propertyRequest))
                .andExpect(status().isCreated());
    }
}