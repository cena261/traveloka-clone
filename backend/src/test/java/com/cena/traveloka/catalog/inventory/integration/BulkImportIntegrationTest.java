package com.cena.traveloka.catalog.inventory.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
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
 * T014: Integration test for bulk CSV property import functionality
 * Tests complete CSV import workflow with validation and error handling
 * These tests MUST FAIL until bulk import implementation is complete
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
class BulkImportIntegrationTest {

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
    void testValidCsvBulkImport() throws Exception {
        // Setup: Create partner
        String partnerId = createTestPartner();

        // Create valid CSV content
        String csvContent = """
            name,description_en,description_vi,starRating,countryCode,city,addressLine,latitude,longitude,phoneNumber,email,checkInTime,checkOutTime,totalRooms
            "Grand Hotel Phuket","Beachfront resort in Phuket","Khu nghỉ dưỡng bãi biển ở Phuket",4,"TH","Phuket","456 Beach Road",7.8804,98.3923,"+66-76-123-456","info@grandphuket.com","14:00","12:00",150
            "Grand Hotel Chiang Mai","Mountain view hotel","Khách sạn view núi",4,"TH","Chiang Mai","789 Mountain Road",18.7883,98.9853,"+66-53-123-456","info@grandchiangmai.com","15:00","11:00",120
            "Grand Hotel Pattaya","Beach resort","Khu nghỉ dưỡng bãi biển",3,"TH","Pattaya","321 Beach Avenue",12.9236,100.8824,"+66-38-123-456","info@grandpattaya.com","15:00","12:00",200
            """;

        MockMultipartFile csvFile = new MockMultipartFile(
                "file", "properties.csv", "text/csv", csvContent.getBytes());

        // This MUST FAIL until bulk import is implemented
        var importResult = mockMvc.perform(multipart("/api/v1/properties/bulk-import")
                        .file(csvFile)
                        .param("partnerId", partnerId))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.status").value("STARTED"))
                .andExpect(jsonPath("$.totalRecords").value(3))
                .andExpect(jsonPath("$.processedRecords").value(0))
                .andExpect(jsonPath("$.successfulRecords").value(0))
                .andExpect(jsonPath("$.failedRecords").value(0))
                .andReturn();

        String jobId = objectMapper.readTree(importResult.getResponse().getContentAsString())
                .path("jobId").asText();

        // Check import job status
        mockMvc.perform(get("/api/v1/properties/bulk-import/{jobId}/status", jobId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.totalRecords").value(3));

        // Wait for processing and verify completion
        Thread.sleep(2000); // Allow time for async processing

        mockMvc.perform(get("/api/v1/properties/bulk-import/{jobId}/status", jobId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.successfulRecords").value(3))
                .andExpect(jsonPath("$.failedRecords").value(0));

        // Verify properties were created
        mockMvc.perform(get("/api/v1/properties")
                        .param("partnerId", partnerId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    void testCsvImportWithValidationErrors() throws Exception {
        String partnerId = createTestPartner();

        // Create CSV with validation errors
        String csvContent = """
            name,description_en,description_vi,starRating,countryCode,city,addressLine,latitude,longitude,phoneNumber,email,checkInTime,checkOutTime,totalRooms
            "Valid Hotel","Valid description","Mô tả hợp lệ",4,"TH","Bangkok","123 Valid Road",13.7563,100.5018,"+66-2-123-456","valid@hotel.com","15:00","11:00",100
            "Invalid Coordinates","Bad coordinates","Tọa độ không hợp lệ",3,"TH","Bangkok","456 Invalid Road",95.0,200.0,"+66-2-123-457","invalid@hotel.com","15:00","11:00",50
            "Invalid Rating","Invalid star rating","Đánh giá sao không hợp lệ",6,"TH","Bangkok","789 Rating Road",13.7563,100.5018,"+66-2-123-458","rating@hotel.com","15:00","11:00",75
            """;

        MockMultipartFile csvFile = new MockMultipartFile(
                "file", "properties-errors.csv", "text/csv", csvContent.getBytes());

        var importResult = mockMvc.perform(multipart("/api/v1/properties/bulk-import")
                        .file(csvFile)
                        .param("partnerId", partnerId))
                .andExpected(status().isAccepted())
                .andReturn();

        String jobId = objectMapper.readTree(importResult.getResponse().getContentAsString())
                .path("jobId").asText();

        // Wait for processing
        Thread.sleep(2000);

        // Check final status - should have partial success
        mockMvc.perform(get("/api/v1/properties/bulk-import/{jobId}/status", jobId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.successfulRecords").value(1))
                .andExpect(jsonPath("$.failedRecords").value(2))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void testCsvImportFormatValidation() throws Exception {
        String partnerId = createTestPartner();

        // Test 1: Invalid CSV format (missing required columns)
        String invalidCsvContent = """
            name,description_en
            "Hotel 1","Description 1"
            "Hotel 2","Description 2"
            """;

        MockMultipartFile invalidCsvFile = new MockMultipartFile(
                "file", "invalid.csv", "text/csv", invalidCsvContent.getBytes());

        mockMvc.perform(multipart("/api/v1/properties/bulk-import")
                        .file(invalidCsvFile)
                        .param("partnerId", partnerId))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        // Test 2: Empty CSV file
        MockMultipartFile emptyCsvFile = new MockMultipartFile(
                "file", "empty.csv", "text/csv", "".getBytes());

        mockMvc.perform(multipart("/api/v1/properties/bulk-import")
                        .file(emptyCsvFile)
                        .param("partnerId", partnerId))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Test 3: Non-CSV file
        MockMultipartFile nonCsvFile = new MockMultipartFile(
                "file", "properties.txt", "text/plain", "not a csv file".getBytes());

        mockMvc.perform(multipart("/api/v1/properties/bulk-import")
                        .file(nonCsvFile)
                        .param("partnerId", partnerId))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCsvImportWithDuplicateData() throws Exception {
        String partnerId = createTestPartner();

        // Create CSV with duplicate property names
        String csvContent = """
            name,description_en,description_vi,starRating,countryCode,city,addressLine,latitude,longitude,phoneNumber,email,checkInTime,checkOutTime,totalRooms
            "Duplicate Hotel","First instance","Lần đầu tiên",4,"TH","Bangkok","123 First Road",13.7563,100.5018,"+66-2-123-456","first@hotel.com","15:00","11:00",100
            "Duplicate Hotel","Second instance","Lần thứ hai",3,"TH","Bangkok","456 Second Road",13.7600,100.5050,"+66-2-123-457","second@hotel.com","15:00","11:00",80
            "Unique Hotel","Unique instance","Duy nhất",5,"TH","Bangkok","789 Unique Road",13.7650,100.5100,"+66-2-123-458","unique@hotel.com","15:00","11:00",120
            """;

        MockMultipartFile csvFile = new MockMultipartFile(
                "file", "duplicates.csv", "text/csv", csvContent.getBytes());

        var importResult = mockMvc.perform(multipart("/api/v1/properties/bulk-import")
                        .file(csvFile)
                        .param("partnerId", partnerId))
                .andExpect(status().isAccepted())
                .andReturn();

        String jobId = objectMapper.readTree(importResult.getResponse().getContentAsString())
                .path("jobId").asText();

        // Wait for processing
        Thread.sleep(2000);

        // Check status - first duplicate should succeed, second should fail
        mockMvc.perform(get("/api/v1/properties/bulk-import/{jobId}/status", jobId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successfulRecords").value(2))
                .andExpect(jsonPath("$.failedRecords").value(1));
    }

    @Test
    void testCsvImportJobTracking() throws Exception {
        String partnerId = createTestPartner();

        // Start multiple import jobs
        String csvContent1 = """
            name,description_en,description_vi,starRating,countryCode,city,addressLine,latitude,longitude,phoneNumber,email,checkInTime,checkOutTime,totalRooms
            "Hotel A","Description A","Mô tả A",4,"TH","Bangkok","123 A Road",13.7563,100.5018,"+66-2-123-456","a@hotel.com","15:00","11:00",100
            """;

        String csvContent2 = """
            name,description_en,description_vi,starRating,countryCode,city,addressLine,latitude,longitude,phoneNumber,email,checkInTime,checkOutTime,totalRooms
            "Hotel B","Description B","Mô tả B",3,"TH","Bangkok","123 B Road",13.7600,100.5050,"+66-2-123-457","b@hotel.com","15:00","11:00",80
            """;

        MockMultipartFile csvFile1 = new MockMultipartFile("file", "batch1.csv", "text/csv", csvContent1.getBytes());
        MockMultipartFile csvFile2 = new MockMultipartFile("file", "batch2.csv", "text/csv", csvContent2.getBytes());

        // Start first job
        var result1 = mockMvc.perform(multipart("/api/v1/properties/bulk-import")
                        .file(csvFile1)
                        .param("partnerId", partnerId))
                .andExpect(status().isAccepted())
                .andReturn();

        // Start second job
        var result2 = mockMvc.perform(multipart("/api/v1/properties/bulk-import")
                        .file(csvFile2)
                        .param("partnerId", partnerId))
                .andExpect(status().isAccepted())
                .andReturn();

        String jobId1 = objectMapper.readTree(result1.getResponse().getContentAsString()).path("jobId").asText();
        String jobId2 = objectMapper.readTree(result2.getResponse().getContentAsString()).path("jobId").asText();

        // Verify both jobs are tracked independently
        mockMvc.perform(get("/api/v1/properties/bulk-import/{jobId}/status", jobId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId1));

        mockMvc.perform(get("/api/v1/properties/bulk-import/{jobId}/status", jobId2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId2));
    }

    private String createTestPartner() throws Exception {
        var partnerRequest = """
            {
                "name": "Bulk Import Test Partner",
                "email": "bulk@partner.com",
                "phone": "+1-555-0123",
                "businessRegistrationNumber": "BULK-001",
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
}