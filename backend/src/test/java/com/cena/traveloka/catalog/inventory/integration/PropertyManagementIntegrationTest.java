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
 * Integration test for property creation and management with PostGIS spatial features
 * Tests the complete property lifecycle including spatial data storage and retrieval
 * These tests MUST FAIL until full implementation is complete
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
class PropertyManagementIntegrationTest {

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
    void testCompletePropertyCreationWorkflow() throws Exception {
        // Step 1: Create a partner first
        String partnerId = createTestPartner();

        // Step 2: Create property with PostGIS location data
        var propertyRequest = String.format("""
            {
                "partnerId": "%s",
                "propertyTypeId": "123e4567-e89b-12d3-a456-426614174001",
                "name": "Grand Hotel Bangkok",
                "description": {
                    "en": "Luxury 5-star hotel in downtown Bangkok",
                    "vi": "Khách sạn 5 sao sang trọng ở trung tâm Bangkok"
                },
                "starRating": 5,
                "countryCode": "TH",
                "city": "Bangkok",
                "addressLine": "123 Sukhumvit Road, Watthana",
                "postalCode": "10110",
                "latitude": 13.7563,
                "longitude": 100.5018,
                "phoneNumber": "+66-2-123-4567",
                "email": "info@grandbangkok.com",
                "website": "https://grandbangkok.com",
                "checkInTime": "15:00",
                "checkOutTime": "11:00",
                "totalRooms": 200,
                "timezone": "Asia/Bangkok"
            }
            """, partnerId);

        // This MUST FAIL until property creation is implemented
        var propertyResult = mockMvc.perform(post("/api/v1/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(propertyRequest))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.partnerId").value(partnerId))
                .andExpect(jsonPath("$.name").value("Grand Hotel Bangkok"))
                .andExpect(jsonPath("$.latitude").value(13.7563))
                .andExpect(jsonPath("$.longitude").value(100.5018))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.description.en").value("Luxury 5-star hotel in downtown Bangkok"))
                .andExpect(jsonPath("$.description.vi").value("Khách sạn 5 sao sang trọng ở trung tâm Bangkok"))
                .andReturn();

        String propertyId = objectMapper.readTree(propertyResult.getResponse().getContentAsString())
                .path("id").asText();

        // Step 3: Verify PostGIS spatial data storage
        mockMvc.perform(get("/api/v1/properties/{propertyId}", propertyId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(13.7563))
                .andExpect(jsonPath("$.longitude").value(100.5018))
                .andExpect(jsonPath("$.countryCode").value("TH"))
                .andExpect(jsonPath("$.city").value("Bangkok"));

        // Step 4: Update property information
        var updateRequest = """
            {
                "name": "Updated Grand Hotel Bangkok",
                "description": {
                    "en": "Updated luxury hotel description",
                    "vi": "Mô tả khách sạn sang trọng đã cập nhật"
                },
                "phoneNumber": "+66-2-999-8888",
                "totalRooms": 250
            }
            """;

        mockMvc.perform(put("/api/v1/properties/{propertyId}", propertyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Grand Hotel Bangkok"))
                .andExpect(jsonPath("$.phoneNumber").value("+66-2-999-8888"))
                .andExpect(jsonPath("$.totalRooms").value(250));

        // Step 5: Verify property appears in partner's property list
        mockMvc.perform(get("/api/v1/properties")
                        .param("partnerId", partnerId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(propertyId))
                .andExpect(jsonPath("$.content[0].partnerId").value(partnerId));
    }

    @Test
    void testPropertyValidationRules() throws Exception {
        String partnerId = createTestPartner();

        // Test 1: Invalid coordinates (latitude > 90)
        var invalidLatRequest = String.format("""
            {
                "partnerId": "%s",
                "propertyTypeId": "123e4567-e89b-12d3-a456-426614174001",
                "name": "Invalid Lat Hotel",
                "description": {"en": "Test hotel"},
                "starRating": 3,
                "countryCode": "TH",
                "city": "Bangkok",
                "addressLine": "123 Test Road",
                "latitude": 95.0,
                "longitude": 100.5018,
                "phoneNumber": "+66-2-123-4567",
                "email": "test@hotel.com",
                "checkInTime": "15:00",
                "checkOutTime": "11:00",
                "totalRooms": 100
            }
            """, partnerId);

        // This MUST FAIL due to coordinate validation
        mockMvc.perform(post("/api/v1/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidLatRequest))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        // Test 2: Invalid star rating
        var invalidRatingRequest = String.format("""
            {
                "partnerId": "%s",
                "propertyTypeId": "123e4567-e89b-12d3-a456-426614174001",
                "name": "Invalid Rating Hotel",
                "description": {"en": "Test hotel"},
                "starRating": 6,
                "countryCode": "TH",
                "city": "Bangkok",
                "addressLine": "123 Test Road",
                "latitude": 13.7563,
                "longitude": 100.5018,
                "phoneNumber": "+66-2-123-4567",
                "email": "test@hotel.com",
                "checkInTime": "15:00",
                "checkOutTime": "11:00",
                "totalRooms": 100
            }
            """, partnerId);

        mockMvc.perform(post("/api/v1/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRatingRequest))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Test 3: Invalid check-in/check-out times
        var invalidTimeRequest = String.format("""
            {
                "partnerId": "%s",
                "propertyTypeId": "123e4567-e89b-12d3-a456-426614174001",
                "name": "Invalid Time Hotel",
                "description": {"en": "Test hotel"},
                "starRating": 3,
                "countryCode": "TH",
                "city": "Bangkok",
                "addressLine": "123 Test Road",
                "latitude": 13.7563,
                "longitude": 100.5018,
                "phoneNumber": "+66-2-123-4567",
                "email": "test@hotel.com",
                "checkInTime": "23:00",
                "checkOutTime": "08:00",
                "totalRooms": 100
            }
            """, partnerId);

        mockMvc.perform(post("/api/v1/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidTimeRequest))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMultiLanguageDescriptionHandling() throws Exception {
        String partnerId = createTestPartner();

        // Test property with multiple language descriptions
        var multiLangRequest = String.format("""
            {
                "partnerId": "%s",
                "propertyTypeId": "123e4567-e89b-12d3-a456-426614174001",
                "name": "Multilingual Hotel",
                "description": {
                    "en": "English description of the hotel with amenities and services",
                    "vi": "Mô tả tiếng Việt về khách sạn với các tiện nghi và dịch vụ",
                    "th": "รายละเอียดโรงแรมเป็นภาษาไทย"
                },
                "starRating": 4,
                "countryCode": "TH",
                "city": "Bangkok",
                "addressLine": "456 Multilingual Road",
                "latitude": 13.7563,
                "longitude": 100.5018,
                "phoneNumber": "+66-2-123-4567",
                "email": "multilang@hotel.com",
                "checkInTime": "15:00",
                "checkOutTime": "11:00",
                "totalRooms": 150
            }
            """, partnerId);

        var result = mockMvc.perform(post("/api/v1/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(multiLangRequest))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description.en").value("English description of the hotel with amenities and services"))
                .andExpect(jsonPath("$.description.vi").value("Mô tả tiếng Việt về khách sạn với các tiện nghi và dịch vụ"))
                .andExpect(jsonPath("$.description.th").value("รายละเอียดโรงแรมเป็นภาษาไทย"))
                .andReturn();

        String propertyId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText();

        // Verify retrieval maintains language data
        mockMvc.perform(get("/api/v1/properties/{propertyId}", propertyId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description.en").exists())
                .andExpect(jsonPath("$.description.vi").exists())
                .andExpect(jsonPath("$.description.th").exists());
    }

    @Test
    void testPropertyStatusWorkflow() throws Exception {
        String partnerId = createTestPartner();
        String propertyId = createTestProperty(partnerId, "Status Test Hotel");

        // Initial status should be DRAFT
        mockMvc.perform(get("/api/v1/properties/{propertyId}", propertyId))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        // Submit for verification (status change to PENDING_VERIFICATION)
        mockMvc.perform(post("/api/v1/properties/{propertyId}/submit-verification", propertyId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"));

        // Approve property (status change to ACTIVE)
        mockMvc.perform(post("/api/v1/properties/{propertyId}/approve", propertyId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Deactivate property (status change to INACTIVE)
        mockMvc.perform(post("/api/v1/properties/{propertyId}/deactivate", propertyId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void testPropertyTimezoneHandling() throws Exception {
        String partnerId = createTestPartner();

        // Create properties in different timezones
        String[] timezones = {"Asia/Bangkok", "Asia/Jakarta", "Asia/Ho_Chi_Minh", "UTC"};

        for (String timezone : timezones) {
            var propertyRequest = String.format("""
                {
                    "partnerId": "%s",
                    "propertyTypeId": "123e4567-e89b-12d3-a456-426614174001",
                    "name": "Hotel in %s",
                    "description": {"en": "Hotel description"},
                    "starRating": 3,
                    "countryCode": "TH",
                    "city": "Bangkok",
                    "addressLine": "123 Timezone Road",
                    "latitude": 13.7563,
                    "longitude": 100.5018,
                    "phoneNumber": "+66-2-123-4567",
                    "email": "timezone@hotel.com",
                    "checkInTime": "15:00",
                    "checkOutTime": "11:00",
                    "totalRooms": 100,
                    "timezone": "%s"
                }
                """, partnerId, timezone, timezone);

            mockMvc.perform(post("/api/v1/properties")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(propertyRequest))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.timezone").value(timezone));
        }
    }

    private String createTestPartner() throws Exception {
        var partnerRequest = """
            {
                "name": "Test Hotel Chain",
                "email": "test@hotelchain.com",
                "phone": "+1-555-0123",
                "businessRegistrationNumber": "TEST-001",
                "contractStartDate": "2024-01-01",
                "contractEndDate": "2026-12-31",
                "commissionRate": 15.00
            }
            """;

        var result = mockMvc.perform(post("/api/v1/partners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partnerRequest))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText();
    }

    private String createTestProperty(String partnerId, String name) throws Exception {
        var propertyRequest = String.format("""
            {
                "partnerId": "%s",
                "propertyTypeId": "123e4567-e89b-12d3-a456-426614174001",
                "name": "%s",
                "description": {"en": "Test hotel description"},
                "starRating": 3,
                "countryCode": "TH",
                "city": "Bangkok",
                "addressLine": "123 Test Road",
                "latitude": 13.7563,
                "longitude": 100.5018,
                "phoneNumber": "+66-2-123-4567",
                "email": "test@hotel.com",
                "checkInTime": "15:00",
                "checkOutTime": "11:00",
                "totalRooms": 100
            }
            """, partnerId, name);

        var result = mockMvc.perform(post("/api/v1/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(propertyRequest))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText();
    }
}