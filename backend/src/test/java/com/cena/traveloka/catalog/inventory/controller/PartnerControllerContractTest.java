package com.cena.traveloka.catalog.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.cena.traveloka.catalog.inventory.service.PartnerService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract tests for Partner API endpoints
 * These tests verify the API contract matches the OpenAPI specification
 * Tests MUST FAIL initially before implementation exists
 */
@WebMvcTest(PartnerController.class)
class PartnerControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PartnerService partnerService;

    @Test
    void testCreatePartner_ValidRequest_Returns201() throws Exception {
        // Arrange - Partner creation request matching API contract
        var createRequest = """
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

        // Act & Assert - This MUST FAIL until PartnerController is implemented
        mockMvc.perform(post("/api/v1/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Grand Hotel Chain"))
                .andExpect(jsonPath("$.email").value("partner@grandhotelchain.com"))
                .andExpect(jsonPath("$.phone").value("+1-555-0123"))
                .andExpect(jsonPath("$.businessRegistrationNumber").value("GHC-2024-001"))
                .andExpect(jsonPath("$.contractStartDate").value("2024-01-01"))
                .andExpect(jsonPath("$.contractEndDate").value("2026-12-31"))
                .andExpected(jsonPath("$.commissionRate").value(15.00))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.performanceRating").value(0.00))
                .andExpect(jsonPath("$.totalBookings").value(0))
                .andExpect(jsonPath("$.averageResponseTime").value(0))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void testCreatePartner_InvalidEmail_Returns400() throws Exception {
        // Arrange - Invalid email format
        var createRequest = """
            {
                "name": "Grand Hotel Chain",
                "email": "invalid-email",
                "phone": "+1-555-0123",
                "businessRegistrationNumber": "GHC-2024-001",
                "contractStartDate": "2024-01-01",
                "contractEndDate": "2026-12-31",
                "commissionRate": 15.00
            }
            """;

        // Act & Assert - This MUST FAIL until validation is implemented
        mockMvc.perform(post("/api/v1/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testCreatePartner_InvalidCommissionRate_Returns400() throws Exception {
        // Arrange - Commission rate outside valid range
        var createRequest = """
            {
                "name": "Grand Hotel Chain",
                "email": "partner@grandhotelchain.com",
                "phone": "+1-555-0123",
                "businessRegistrationNumber": "GHC-2024-001",
                "contractStartDate": "2024-01-01",
                "contractEndDate": "2026-12-31",
                "commissionRate": 150.00
            }
            """;

        // Act & Assert - This MUST FAIL until validation is implemented
        mockMvc.perform(post("/api/v1/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testGetPartner_ValidId_Returns200() throws Exception {
        // Arrange - Valid partner ID
        UUID partnerId = UUID.randomUUID();

        // Act & Assert - This MUST FAIL until PartnerController.getPartner is implemented
        mockMvc.perform(get("/api/v1/partners/{partnerId}", partnerId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(partnerId.toString()))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.commissionRate").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void testGetPartner_InvalidId_Returns404() throws Exception {
        // Arrange - Non-existent partner ID
        UUID partnerId = UUID.randomUUID();

        // Act & Assert - This MUST FAIL until error handling is implemented
        mockMvc.perform(get("/api/v1/partners/{partnerId}", partnerId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testListPartners_WithPagination_Returns200() throws Exception {
        // Act & Assert - This MUST FAIL until PartnerController.listPartners is implemented
        mockMvc.perform(get("/api/v1/partners")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpected(jsonPath("$.totalPages").exists())
                .andExpected(jsonPath("$.first").isBoolean())
                .andExpected(jsonPath("$.last").isBoolean());
    }

    @Test
    void testListPartners_WithStatusFilter_Returns200() throws Exception {
        // Act & Assert - This MUST FAIL until filtering is implemented
        mockMvc.perform(get("/api/v1/partners")
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testUpdatePartner_ValidRequest_Returns200() throws Exception {
        // Arrange - Partner update request
        UUID partnerId = UUID.randomUUID();
        var updateRequest = """
            {
                "name": "Updated Hotel Chain",
                "phone": "+1-555-9999",
                "commissionRate": 18.50
            }
            """;

        // Act & Assert - This MUST FAIL until PartnerController.updatePartner is implemented
        mockMvc.perform(put("/api/v1/partners/{partnerId}", partnerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(partnerId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Hotel Chain"))
                .andExpected(jsonPath("$.phone").value("+1-555-9999"))
                .andExpected(jsonPath("$.commissionRate").value(18.50));
    }

    @Test
    void testGetPartnerPerformance_ValidRequest_Returns200() throws Exception {
        // Arrange - Performance metrics request
        UUID partnerId = UUID.randomUUID();

        // Act & Assert - This MUST FAIL until performance tracking is implemented
        mockMvc.perform(get("/api/v1/partners/{partnerId}/performance", partnerId)
                        .param("fromDate", "2024-01-01")
                        .param("toDate", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.partnerId").value(partnerId.toString()))
                .andExpect(jsonPath("$.periodStart").value("2024-01-01"))
                .andExpect(jsonPath("$.periodEnd").value("2024-12-31"))
                .andExpect(jsonPath("$.totalBookings").exists())
                .andExpect(jsonPath("$.totalRevenue").exists())
                .andExpect(jsonPath("$.averageRating").exists())
                .andExpect(jsonPath("$.responseTimeHours").exists())
                .andExpect(jsonPath("$.conversionRate").exists());
    }
}