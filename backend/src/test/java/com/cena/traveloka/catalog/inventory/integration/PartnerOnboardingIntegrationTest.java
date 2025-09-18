package com.cena.traveloka.catalog.inventory.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

/**
 * Integration test for complete partner registration and onboarding workflow
 * Tests the full business process from partner registration through contract management
 * These tests MUST FAIL until full implementation is complete
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
class PartnerOnboardingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgis/postgis:16-3.4")
            .withDatabaseName("traveloka_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCompletePartnerOnboardingWorkflow() throws Exception {
        // Step 1: Register new hotel partner
        var partnerRequest = """
            {
                "name": "Grand Hotel Chain",
                "email": "partner@grandhotelchain.com",
                "phone": "+1-555-0123",
                "businessRegistrationNumber": "GHC-2024-001",
                "contractStartDate": "2024-01-01",
                "contractEndDate": "2026-12-31",
                "commissionRate": 15.00
            }
            """;

        // This MUST FAIL until partner registration is implemented
        var partnerResult = mockMvc.perform(post("/api/v1/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partnerRequest))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.commissionRate").value(15.00))
                .andReturn();

        String partnerId = objectMapper.readTree(partnerResult.getResponse().getContentAsString())
                .path("id").asText();

        // Step 2: Verify partner appears in listing
        mockMvc.perform(get("/api/v1/partners")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(partnerId))
                .andExpect(jsonPath("$.totalElements").value(1));

        // Step 3: Update partner information during onboarding
        var updateRequest = """
            {
                "phone": "+1-555-9999",
                "commissionRate": 18.50
            }
            """;

        mockMvc.perform(put("/api/v1/partners/{partnerId}", partnerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+1-555-9999"))
                .andExpect(jsonPath("$.commissionRate").value(18.50));

        // Step 4: Verify partner details are correct
        mockMvc.perform(get("/api/v1/partners/{partnerId}", partnerId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Grand Hotel Chain"))
                .andExpect(jsonPath("$.email").value("partner@grandhotelchain.com"))
                .andExpect(jsonPath("$.phone").value("+1-555-9999"))
                .andExpect(jsonPath("$.commissionRate").value(18.50))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Step 5: Check initial performance metrics
        mockMvc.perform(get("/api/v1/partners/{partnerId}/performance", partnerId)
                        .param("fromDate", "2024-01-01")
                        .param("toDate", "2024-12-31"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partnerId").value(partnerId))
                .andExpect(jsonPath("$.totalBookings").value(0))
                .andExpect(jsonPath("$.totalRevenue").value(0.00))
                .andExpect(jsonPath("$.averageRating").value(0.00));
    }

    @Test
    void testPartnerRegistrationValidation() throws Exception {
        // Test 1: Duplicate email validation
        var partnerRequest1 = """
            {
                "name": "First Hotel",
                "email": "duplicate@hotel.com",
                "phone": "+1-555-0001",
                "businessRegistrationNumber": "FIRST-001",
                "contractStartDate": "2024-01-01",
                "contractEndDate": "2026-12-31",
                "commissionRate": 15.00
            }
            """;

        // First registration should succeed
        mockMvc.perform(post("/api/v1/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partnerRequest1))
                .andExpect(status().isCreated());

        var partnerRequest2 = """
            {
                "name": "Second Hotel",
                "email": "duplicate@hotel.com",
                "phone": "+1-555-0002",
                "businessRegistrationNumber": "SECOND-002",
                "contractStartDate": "2024-01-01",
                "contractEndDate": "2026-12-31",
                "commissionRate": 20.00
            }
            """;

        // Second registration with same email MUST FAIL
        mockMvc.perform(post("/api/v1/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partnerRequest2))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpected(jsonPath("$.message").exists());
    }

    @Test
    void testPartnerContractValidation() throws Exception {
        // Test invalid contract dates (end before start)
        var invalidContractRequest = """
            {
                "name": "Invalid Contract Hotel",
                "email": "invalid@hotel.com",
                "phone": "+1-555-0003",
                "businessRegistrationNumber": "INVALID-003",
                "contractStartDate": "2026-12-31",
                "contractEndDate": "2024-01-01",
                "commissionRate": 15.00
            }
            """;

        // This MUST FAIL due to business rule validation
        mockMvc.perform(post("/api/v1/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidContractRequest))
                .andDo(print())
                .andExpected(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testPartnerListingWithFilters() throws Exception {
        // Create multiple partners with different statuses
        createTestPartner("Active Hotel", "active@hotel.com", "ACTIVE-001");
        createTestPartner("Pending Hotel", "pending@hotel.com", "PENDING-002");
        createTestPartner("Suspended Hotel", "suspended@hotel.com", "SUSPENDED-003");

        // Test filtering by status
        mockMvc.perform(get("/api/v1/partners")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[?(@.status == 'PENDING')]").exists());

        // Test search functionality
        mockMvc.perform(get("/api/v1/partners")
                        .param("search", "Active")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.name =~ /.*Active.*/)]").exists());
    }

    @Test
    void testPartnerPerformanceTracking() throws Exception {
        // Create a partner
        var partnerId = createTestPartner("Performance Hotel", "performance@hotel.com", "PERF-001");

        // Simulate booking activity (this would normally come from booking service)
        // For now, we test that the endpoint accepts the request format
        var bookingActivity = """
            {
                "bookingCount": 5,
                "totalRevenue": 2500.00,
                "averageRating": 4.2,
                "responseTimeHours": 2
            }
            """;

        mockMvc.perform(post("/api/v1/partners/{partnerId}/performance", partnerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingActivity))
                .andDo(print())
                .andExpect(status().isOk());

        // Verify updated performance metrics
        mockMvc.perform(get("/api/v1/partners/{partnerId}/performance", partnerId)
                        .param("fromDate", "2024-01-01")
                        .param("toDate", "2024-12-31"))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpected(jsonPath("$.totalBookings").value(5))
                .andExpected(jsonPath("$.totalRevenue").value(2500.00))
                .andExpected(jsonPath("$.averageRating").value(4.2));
    }

    private String createTestPartner(String name, String email, String regNumber) throws Exception {
        var partnerRequest = String.format("""
            {
                "name": "%s",
                "email": "%s",
                "phone": "+1-555-0123",
                "businessRegistrationNumber": "%s",
                "contractStartDate": "2024-01-01",
                "contractEndDate": "2026-12-31",
                "commissionRate": 15.00
            }
            """, name, email, regNumber);

        var result = mockMvc.perform(post("/api/v1/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partnerRequest))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText();
    }
}