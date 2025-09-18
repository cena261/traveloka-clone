package com.cena.traveloka.catalog.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.cena.traveloka.catalog.inventory.service.PropertyService;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract tests for Property API endpoints
 * These tests verify the API contract matches the OpenAPI specification
 * Tests MUST FAIL initially before implementation exists
 */
@WebMvcTest(PropertyController.class)
class PropertyControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PropertyService propertyService;

    @Test
    void testCreateProperty_ValidRequest_Returns201() throws Exception {
        // Arrange - Property creation request matching API contract
        var createRequest = """
            {
                "partnerId": "123e4567-e89b-12d3-a456-426614174000",
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
            """;

        // Act & Assert - This MUST FAIL until PropertyController is implemented
        mockMvc.perform(post("/api/v1/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.partnerId").value("123e4567-e89b-12d3-a456-426614174000"))
                .andExpect(jsonPath("$.name").value("Grand Hotel Bangkok"))
                .andExpect(jsonPath("$.description.en").value("Luxury 5-star hotel in downtown Bangkok"))
                .andExpect(jsonPath("$.description.vi").value("Khách sạn 5 sao sang trọng ở trung tâm Bangkok"))
                .andExpect(jsonPath("$.starRating").value(5))
                .andExpect(jsonPath("$.countryCode").value("TH"))
                .andExpect(jsonPath("$.city").value("Bangkok"))
                .andExpect(jsonPath("$.latitude").value(13.7563))
                .andExpect(jsonPath("$.longitude").value(100.5018))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalRooms").value(200))
                .andExpect(jsonPath("$.ratingAvg").value(0.00))
                .andExpect(jsonPath("$.ratingCount").value(0))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void testCreateProperty_InvalidCoordinates_Returns400() throws Exception {
        // Arrange - Invalid latitude/longitude values
        var createRequest = """
            {
                "partnerId": "123e4567-e89b-12d3-a456-426614174000",
                "propertyTypeId": "123e4567-e89b-12d3-a456-426614174001",
                "name": "Grand Hotel Bangkok",
                "description": {
                    "en": "Luxury hotel"
                },
                "starRating": 5,
                "countryCode": "TH",
                "city": "Bangkok",
                "addressLine": "123 Sukhumvit Road",
                "latitude": 95.0,
                "longitude": 200.0,
                "phoneNumber": "+66-2-123-4567",
                "email": "info@grandbangkok.com",
                "checkInTime": "15:00",
                "checkOutTime": "11:00",
                "totalRooms": 200
            }
            """;

        // Act & Assert - This MUST FAIL until validation is implemented
        mockMvc.perform(post("/api/v1/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testGetProperty_ValidId_Returns200() throws Exception {
        // Arrange - Valid property ID
        UUID propertyId = UUID.randomUUID();

        // Act & Assert - This MUST FAIL until PropertyController.getProperty is implemented
        mockMvc.perform(get("/api/v1/properties/{propertyId}", propertyId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(propertyId.toString()))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.partnerId").exists())
                .andExpect(jsonPath("$.latitude").exists())
                .andExpect(jsonPath("$.longitude").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void testSearchProperties_WithLocationFilter_Returns200() throws Exception {
        // Act & Assert - This MUST FAIL until spatial search is implemented
        mockMvc.perform(get("/api/v1/properties")
                        .param("lat", "13.7563")
                        .param("lng", "100.5018")
                        .param("radius", "10")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").exists());
    }

    @Test
    void testSearchProperties_WithCityFilter_Returns200() throws Exception {
        // Act & Assert - This MUST FAIL until filtering is implemented
        mockMvc.perform(get("/api/v1/properties")
                        .param("city", "Bangkok")
                        .param("starRating", "5")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testUploadPropertyImages_ValidFiles_Returns201() throws Exception {
        // Arrange - Mock image files
        UUID propertyId = UUID.randomUUID();
        MockMultipartFile image1 = new MockMultipartFile(
                "images", "hotel-exterior.jpg", "image/jpeg", "mock image content".getBytes());
        MockMultipartFile image2 = new MockMultipartFile(
                "images", "hotel-lobby.jpg", "image/jpeg", "mock image content 2".getBytes());

        // Act & Assert - This MUST FAIL until image upload is implemented
        mockMvc.perform(multipart("/api/v1/properties/{propertyId}/images", propertyId)
                        .file(image1)
                        .file(image2))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.images").isArray())
                .andExpect(jsonPath("$.images[0].id").exists())
                .andExpect(jsonPath("$.images[0].url").exists())
                .andExpect(jsonPath("$.images[0].sortOrder").exists());
    }

    @Test
    void testBulkImportProperties_ValidCSV_Returns202() throws Exception {
        // Arrange - Mock CSV file
        UUID partnerId = UUID.randomUUID();
        String csvContent = """
            name,description_en,description_vi,starRating,countryCode,city,addressLine,latitude,longitude,phoneNumber,email,checkInTime,checkOutTime,totalRooms
            "Grand Hotel Phuket","Beachfront resort","Resort bãi biển",4,"TH","Phuket","456 Beach Road",7.8804,98.3923,"+66-76-123-456","info@grandphuket.com","14:00","12:00",150
            """;
        MockMultipartFile csvFile = new MockMultipartFile(
                "file", "properties.csv", "text/csv", csvContent.getBytes());

        // Act & Assert - This MUST FAIL until bulk import is implemented
        mockMvc.perform(multipart("/api/v1/properties/bulk-import")
                        .file(csvFile)
                        .param("partnerId", partnerId.toString()))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.status").value("STARTED"))
                .andExpect(jsonPath("$.totalRecords").exists());
    }

    @Test
    void testUpdateProperty_ValidRequest_Returns200() throws Exception {
        // Arrange - Property update request
        UUID propertyId = UUID.randomUUID();
        var updateRequest = """
            {
                "name": "Updated Grand Hotel Bangkok",
                "description": {
                    "en": "Updated luxury hotel description",
                    "vi": "Mô tả khách sạn sang trọng đã cập nhật"
                },
                "phoneNumber": "+66-2-999-8888",
                "checkInTime": "14:00",
                "checkOutTime": "12:00"
            }
            """;

        // Act & Assert - This MUST FAIL until PropertyController.updateProperty is implemented
        mockMvc.perform(put("/api/v1/properties/{propertyId}", propertyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(propertyId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Grand Hotel Bangkok"))
                .andExpect(jsonPath("$.phoneNumber").value("+66-2-999-8888"))
                .andExpect(jsonPath("$.checkInTime").value("14:00"))
                .andExpect(jsonPath("$.checkOutTime").value("12:00"));
    }

    @Test
    void testGetPropertyDetails_WithAmenities_Returns200() throws Exception {
        // Arrange - Property with full details
        UUID propertyId = UUID.randomUUID();

        // Act & Assert - This MUST FAIL until detailed view is implemented
        mockMvc.perform(get("/api/v1/properties/{propertyId}", propertyId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(propertyId.toString()))
                .andExpect(jsonPath("$.amenities").isArray())
                .andExpect(jsonPath("$.roomTypes").isArray())
                .andExpect(jsonPath("$.policies").isArray())
                .andExpect(jsonPath("$.images").isArray());
    }
}