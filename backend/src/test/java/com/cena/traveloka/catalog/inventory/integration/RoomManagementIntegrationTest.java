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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

/**
 * T012: Integration test for room management workflow
 * Tests complete room type and unit management functionality
 * These tests MUST FAIL until full implementation is complete
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
class RoomManagementIntegrationTest {

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
    void testCompleteRoomManagementWorkflow() throws Exception {
        // Setup: Create partner and property
        String partnerId = createTestPartner();
        String propertyId = createTestProperty(partnerId);

        // Step 1: Create room types
        String roomTypeId1 = createRoomType(propertyId, "Deluxe King Room", "KING", 2, 35.0, 150.00);
        String roomTypeId2 = createRoomType(propertyId, "Standard Twin Room", "TWIN", 2, 25.0, 100.00);

        // Step 2: Verify room types are listed for property
        mockMvc.perform(get("/api/v1/properties/{propertyId}/room-types", propertyId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        // Step 3: Add room units to first room type
        String[] roomNumbers = {"101", "102", "103", "201", "202", "203"};
        for (String roomNumber : roomNumbers) {
            int floor = Integer.parseInt(roomNumber.substring(0, 1));
            createRoomUnit(roomTypeId1, roomNumber, floor);
        }

        // Step 4: Verify room units are created
        mockMvc.perform(get("/api/v1/room-types/{roomTypeId}/units", roomTypeId1))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(6));

        // Step 5: Update room unit status to maintenance
        String roomUnitId = getRoomUnitId(roomTypeId1, "101");
        var updateRequest = """
            {
                "status": "MAINTENANCE",
                "lastMaintenanceDate": "2024-01-15",
                "notes": "Annual maintenance check"
            }
            """;

        mockMvc.perform(put("/api/v1/room-units/{roomUnitId}", roomUnitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));

        // Step 6: Filter room units by status
        mockMvc.perform(get("/api/v1/room-types/{roomTypeId}/units", roomTypeId1)
                        .param("status", "AVAILABLE"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5)); // 6 total - 1 in maintenance

        // Step 7: Update room type pricing
        var roomTypeUpdate = """
            {
                "basePrice": 175.00,
                "description": "Updated luxury room with enhanced amenities"
            }
            """;

        mockMvc.perform(put("/api/v1/room-types/{roomTypeId}", roomTypeId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomTypeUpdate))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basePrice").value(175.00));

        // Step 8: Get detailed room type information
        mockMvc.perform(get("/api/v1/room-types/{roomTypeId}", roomTypeId1))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUnits").value(6))
                .andExpect(jsonPath("$.availableUnits").value(5))
                .andExpect(jsonPath("$.units").isArray());
    }

    private String createRoomType(String propertyId, String name, String bedType,
                                 int maxOccupancy, double roomSize, double basePrice) throws Exception {
        var createRequest = String.format("""
            {
                "name": "%s",
                "description": "Spacious room with %s bed",
                "maxOccupancy": %d,
                "bedType": "%s",
                "bedCount": 1,
                "roomSize": %.1f,
                "hasBalcony": true,
                "hasCityView": true,
                "hasSeaView": false,
                "smokingAllowed": false,
                "basePrice": %.2f
            }
            """, name, bedType.toLowerCase(), maxOccupancy, bedType, roomSize, basePrice);

        var result = mockMvc.perform(post("/api/v1/properties/{propertyId}/room-types", propertyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText();
    }

    private void createRoomUnit(String roomTypeId, String roomNumber, int floor) throws Exception {
        var createRequest = String.format("""
            {
                "roomNumber": "%s",
                "floorNumber": %d,
                "notes": "Standard room unit"
            }
            """, roomNumber, floor);

        mockMvc.perform(post("/api/v1/room-types/{roomTypeId}/units", roomTypeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated());
    }

    private String getRoomUnitId(String roomTypeId, String roomNumber) throws Exception {
        var result = mockMvc.perform(get("/api/v1/room-types/{roomTypeId}/units", roomTypeId))
                .andExpect(status().isOk())
                .andReturn();

        var units = objectMapper.readTree(result.getResponse().getContentAsString());
        for (var unit : units) {
            if (roomNumber.equals(unit.path("roomNumber").asText())) {
                return unit.path("id").asText();
            }
        }
        throw new RuntimeException("Room unit not found: " + roomNumber);
    }

    private String createTestPartner() throws Exception {
        var partnerRequest = """
            {
                "name": "Room Test Partner",
                "email": "room@partner.com",
                "phone": "+1-555-0123",
                "businessRegistrationNumber": "ROOM-001",
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

    private String createTestProperty(String partnerId) throws Exception {
        var propertyRequest = String.format("""
            {
                "partnerId": "%s",
                "propertyTypeId": "123e4567-e89b-12d3-a456-426614174001",
                "name": "Room Test Hotel",
                "description": {"en": "Hotel for room testing"},
                "starRating": 4,
                "countryCode": "TH",
                "city": "Bangkok",
                "addressLine": "123 Room Test Road",
                "latitude": 13.7563,
                "longitude": 100.5018,
                "phoneNumber": "+66-2-123-4567",
                "email": "room@hotel.com",
                "checkInTime": "15:00",
                "checkOutTime": "11:00",
                "totalRooms": 100
            }
            """, partnerId);

        var result = mockMvc.perform(post("/api/v1/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(propertyRequest))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText();
    }
}