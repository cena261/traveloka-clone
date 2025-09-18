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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

/**
 * T013: Integration test for MinIO image upload functionality
 * Tests property image upload to MinIO with database storage of URLs
 * These tests MUST FAIL until MinIO integration is complete
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
class PropertyImageIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgis/postgis:16-3.4")
            .withDatabaseName("traveloka_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Container
    static GenericContainer<?> minio = new GenericContainer<>("minio/minio:latest")
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", "minio")
            .withEnv("MINIO_ROOT_PASSWORD", "minio123")
            .withCommand("server", "/data");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testPropertyImageUploadWorkflow() throws Exception {
        // Setup: Create partner and property
        String partnerId = createTestPartner();
        String propertyId = createTestProperty(partnerId);

        // Test 1: Upload multiple property images
        MockMultipartFile image1 = new MockMultipartFile(
                "images", "hotel-exterior.jpg", "image/jpeg",
                "mock hotel exterior image content".getBytes());

        MockMultipartFile image2 = new MockMultipartFile(
                "images", "hotel-lobby.jpg", "image/jpeg",
                "mock hotel lobby image content".getBytes());

        MockMultipartFile image3 = new MockMultipartFile(
                "images", "hotel-room.jpg", "image/jpeg",
                "mock hotel room image content".getBytes());

        // This MUST FAIL until image upload is implemented
        var uploadResult = mockMvc.perform(multipart("/api/v1/properties/{propertyId}/images", propertyId)
                        .file(image1)
                        .file(image2)
                        .file(image3))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.images").isArray())
                .andExpect(jsonPath("$.images.length()").value(3))
                .andExpect(jsonPath("$.images[0].id").exists())
                .andExpect(jsonPath("$.images[0].url").exists())
                .andExpect(jsonPath("$.images[0].sortOrder").exists())
                .andExpect(jsonPath("$.images[1].sortOrder").exists())
                .andExpect(jsonPath("$.images[2].sortOrder").exists())
                .andReturn();

        // Verify URLs are MinIO URLs
        var uploadResponse = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        var firstImageUrl = uploadResponse.path("images").get(0).path("url").asText();

        // URL should contain MinIO endpoint and bucket
        assert firstImageUrl.contains("localhost") : "URL should contain MinIO endpoint";
        assert firstImageUrl.contains("images") : "URL should contain bucket name";

        // Test 2: Verify images appear in property details
        mockMvc.perform(get("/api/v1/properties/{propertyId}", propertyId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images").isArray())
                .andExpect(jsonPath("$.images.length()").value(3))
                .andExpect(jsonPath("$.images[0].url").exists())
                .andExpect(jsonPath("$.images[1].url").exists())
                .andExpect(jsonPath("$.images[2].url").exists());

        // Test 3: Upload with invalid file type
        MockMultipartFile invalidFile = new MockMultipartFile(
                "images", "document.txt", "text/plain",
                "this is not an image".getBytes());

        mockMvc.perform(multipart("/api/v1/properties/{propertyId}/images", propertyId)
                        .file(invalidFile))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        // Test 4: Upload oversized file (should respect 15MB limit from config)
        byte[] oversizedContent = new byte[16 * 1024 * 1024]; // 16MB
        MockMultipartFile oversizedFile = new MockMultipartFile(
                "images", "oversized.jpg", "image/jpeg", oversizedContent);

        mockMvc.perform(multipart("/api/v1/properties/{propertyId}/images", propertyId)
                        .file(oversizedFile))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testImageUploadErrorHandling() throws Exception {
        String partnerId = createTestPartner();
        String propertyId = createTestProperty(partnerId);

        // Test upload to non-existent property
        MockMultipartFile image = new MockMultipartFile(
                "images", "test.jpg", "image/jpeg", "test content".getBytes());

        mockMvc.perform(multipart("/api/v1/properties/{propertyId}/images", "non-existent-id")
                        .file(image))
                .andDo(print())
                .andExpect(status().isNotFound());

        // Test upload with no files
        mockMvc.perform(multipart("/api/v1/properties/{propertyId}/images", propertyId))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testImageSortOrderManagement() throws Exception {
        String partnerId = createTestPartner();
        String propertyId = createTestProperty(partnerId);

        // Upload images in sequence
        for (int i = 1; i <= 5; i++) {
            MockMultipartFile image = new MockMultipartFile(
                    "images", String.format("image-%d.jpg", i), "image/jpeg",
                    String.format("mock image %d content", i).getBytes());

            mockMvc.perform(multipart("/api/v1/properties/{propertyId}/images", propertyId)
                            .file(image))
                    .andExpect(status().isCreated());
        }

        // Verify sort order is maintained
        mockMvc.perform(get("/api/v1/properties/{propertyId}", propertyId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images").isArray())
                .andExpect(jsonPath("$.images.length()").value(5))
                .andExpect(jsonPath("$.images[0].sortOrder").value(0))
                .andExpect(jsonPath("$.images[1].sortOrder").value(1))
                .andExpect(jsonPath("$.images[2].sortOrder").value(2))
                .andExpect(jsonPath("$.images[3].sortOrder").value(3))
                .andExpect(jsonPath("$.images[4].sortOrder").value(4));
    }

    @Test
    void testImageAccessAndPresignedUrls() throws Exception {
        String partnerId = createTestPartner();
        String propertyId = createTestProperty(partnerId);

        MockMultipartFile image = new MockMultipartFile(
                "images", "access-test.jpg", "image/jpeg", "test image content".getBytes());

        var uploadResult = mockMvc.perform(multipart("/api/v1/properties/{propertyId}/images", propertyId)
                        .file(image))
                .andExpect(status().isCreated())
                .andReturn();

        var uploadResponse = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        var imageUrl = uploadResponse.path("images").get(0).path("url").asText();

        // Test generating presigned URL for secure access
        mockMvc.perform(get("/api/v1/properties/{propertyId}/images/presigned-url", propertyId)
                        .param("imageUrl", imageUrl))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presignedUrl").exists())
                .andExpect(jsonPath("$.expiresIn").exists());
    }

    private String createTestPartner() throws Exception {
        var partnerRequest = """
            {
                "name": "Image Test Partner",
                "email": "image@partner.com",
                "phone": "+1-555-0123",
                "businessRegistrationNumber": "IMAGE-001",
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

    private String createTestProperty(String partnerId) throws Exception {
        var propertyRequest = String.format("""
            {
                "partnerId": "%s",
                "propertyTypeId": "123e4567-e89b-12d3-a456-426614174001",
                "name": "Image Test Hotel",
                "description": {"en": "Hotel for image testing"},
                "starRating": 4,
                "countryCode": "TH",
                "city": "Bangkok",
                "addressLine": "123 Image Test Road",
                "latitude": 13.7563,
                "longitude": 100.5018,
                "phoneNumber": "+66-2-123-4567",
                "email": "image@hotel.com",
                "checkInTime": "15:00",
                "checkOutTime": "11:00",
                "totalRooms": 100
            }
            """, partnerId);

        var result = mockMvc.perform(post("/api/v1/properties")
                        .contentType("application/json")
                        .content(propertyRequest))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("id").asText();
    }
}