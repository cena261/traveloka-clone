package com.cena.traveloka.catalog.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.cena.traveloka.catalog.inventory.service.RoomService;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract tests for Room Management API endpoints
 * These tests verify the API contract matches the OpenAPI specification
 * Tests MUST FAIL initially before implementation exists
 */
@WebMvcTest(RoomController.class)
class RoomControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoomService roomService;

    @Test
    void testCreateRoomType_ValidRequest_Returns201() throws Exception {
        // Arrange - Room type creation request
        UUID propertyId = UUID.randomUUID();
        var createRequest = """
            {
                "name": "Deluxe King Room",
                "description": "Spacious room with king bed and city view",
                "maxOccupancy": 2,
                "bedType": "KING",
                "bedCount": 1,
                "roomSize": 35.0,
                "hasBalcony": true,
                "hasCityView": true,
                "hasSeaView": false,
                "smokingAllowed": false,
                "basePrice": 150.00
            }
            """;

        // Act & Assert - This MUST FAIL until RoomController is implemented
        mockMvc.perform(post("/api/v1/properties/{propertyId}/room-types", propertyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.propertyId").value(propertyId.toString()))
                .andExpect(jsonPath("$.name").value("Deluxe King Room"))
                .andExpect(jsonPath("$.description").value("Spacious room with king bed and city view"))
                .andExpect(jsonPath("$.maxOccupancy").value(2))
                .andExpect(jsonPath("$.bedType").value("KING"))
                .andExpect(jsonPath("$.bedCount").value(1))
                .andExpect(jsonPath("$.roomSize").value(35.0))
                .andExpect(jsonPath("$.hasBalcony").value(true))
                .andExpect(jsonPath("$.hasCityView").value(true))
                .andExpect(jsonPath("$.hasSeaView").value(false))
                .andExpect(jsonPath("$.smokingAllowed").value(false))
                .andExpect(jsonPath("$.basePrice").value(150.00))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.totalUnits").value(0))
                .andExpect(jsonPath("$.availableUnits").value(0))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void testCreateRoomType_InvalidOccupancy_Returns400() throws Exception {
        // Arrange - Invalid max occupancy
        UUID propertyId = UUID.randomUUID();
        var createRequest = """
            {
                "name": "Invalid Room",
                "description": "Room with invalid occupancy",
                "maxOccupancy": 25,
                "bedType": "KING",
                "bedCount": 1,
                "roomSize": 35.0,
                "basePrice": 150.00
            }
            """;

        // Act & Assert - This MUST FAIL until validation is implemented
        mockMvc.perform(post("/api/v1/properties/{propertyId}/room-types", propertyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testListRoomTypes_ForProperty_Returns200() throws Exception {
        // Arrange - Valid property ID
        UUID propertyId = UUID.randomUUID();

        // Act & Assert - This MUST FAIL until RoomController.listRoomTypes is implemented
        mockMvc.perform(get("/api/v1/properties/{propertyId}/room-types", propertyId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetRoomType_ValidId_Returns200() throws Exception {
        // Arrange - Valid room type ID
        UUID roomTypeId = UUID.randomUUID();

        // Act & Assert - This MUST FAIL until RoomController.getRoomType is implemented
        mockMvc.perform(get("/api/v1/room-types/{roomTypeId}", roomTypeId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(roomTypeId.toString()))
                .andExpect(jsonPath("$.propertyId").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.maxOccupancy").exists())
                .andExpect(jsonPath("$.bedType").exists())
                .andExpect(jsonPath("$.units").isArray());
    }

    @Test
    void testCreateRoomUnit_ValidRequest_Returns201() throws Exception {
        // Arrange - Room unit creation request
        UUID roomTypeId = UUID.randomUUID();
        var createRequest = """
            {
                "roomNumber": "101",
                "floorNumber": 1,
                "notes": "Standard room unit"
            }
            """;

        // Act & Assert - This MUST FAIL until room unit creation is implemented
        mockMvc.perform(post("/api/v1/room-types/{roomTypeId}/units", roomTypeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.roomTypeId").value(roomTypeId.toString()))
                .andExpect(jsonPath("$.roomNumber").value("101"))
                .andExpect(jsonPath("$.floorNumber").value(1))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.notes").value("Standard room unit"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void testCreateRoomUnit_DuplicateRoomNumber_Returns409() throws Exception {
        // Arrange - Duplicate room number
        UUID roomTypeId = UUID.randomUUID();
        var createRequest = """
            {
                "roomNumber": "101",
                "floorNumber": 1
            }
            """;

        // Act & Assert - This MUST FAIL until duplicate validation is implemented
        mockMvc.perform(post("/api/v1/room-types/{roomTypeId}/units", roomTypeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpected(jsonPath("$.status").value(409));
    }

    @Test
    void testListRoomUnits_WithStatusFilter_Returns200() throws Exception {
        // Arrange - Room type with status filter
        UUID roomTypeId = UUID.randomUUID();

        // Act & Assert - This MUST FAIL until room unit listing is implemented
        mockMvc.perform(get("/api/v1/room-types/{roomTypeId}/units", roomTypeId)
                        .param("status", "AVAILABLE")
                        .param("floorNumber", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testUpdateRoomUnit_ValidRequest_Returns200() throws Exception {
        // Arrange - Room unit update request
        UUID roomUnitId = UUID.randomUUID();
        var updateRequest = """
            {
                "status": "MAINTENANCE",
                "lastMaintenanceDate": "2024-01-15",
                "notes": "Annual maintenance check"
            }
            """;

        // Act & Assert - This MUST FAIL until room unit update is implemented
        mockMvc.perform(put("/api/v1/room-units/{roomUnitId}", roomUnitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.id").value(roomUnitId.toString()))
                .andExpected(jsonPath("$.status").value("MAINTENANCE"))
                .andExpect(jsonPath("$.lastMaintenanceDate").value("2024-01-15"))
                .andExpect(jsonPath("$.notes").value("Annual maintenance check"));
    }

    @Test
    void testUpdateRoomType_ValidRequest_Returns200() throws Exception {
        // Arrange - Room type update request
        UUID roomTypeId = UUID.randomUUID();
        var updateRequest = """
            {
                "name": "Updated Deluxe King Room",
                "description": "Updated room description",
                "basePrice": 175.00,
                "isActive": false
            }
            """;

        // Act & Assert - This MUST FAIL until room type update is implemented
        mockMvc.perform(put("/api/v1/room-types/{roomTypeId}", roomTypeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(roomTypeId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Deluxe King Room"))
                .andExpect(jsonPath("$.basePrice").value(175.00))
                .andExpect(jsonPath("$.isActive").value(false));
    }
}