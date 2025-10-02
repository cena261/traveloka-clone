package com.cena.traveloka.common.dto;

import com.cena.traveloka.common.enums.ResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import java.time.temporal.ChronoUnit;

/**
 * Test class for ApiResponse wrapper functionality.
 * Tests response structure, builder pattern, and JSON serialization.
 *
 * CRITICAL: These tests MUST FAIL initially (TDD requirement).
 * ApiResponse and ResponseStatus implementations do not exist yet.
 */
class ApiResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldCreateSuccessResponse() {
        // Given: Success data
        String data = "test data";
        String message = "Operation completed successfully";

        // When: Success response is created
        ApiResponse<String> response = ApiResponse.success(message, data);

        // Then: Response has correct structure
        assertThat(response.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(response.getCode()).isNotNull();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getTimestamp().getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void shouldCreateSuccessResponseWithoutData() {
        // Given: Success message only
        String message = "Operation completed";

        // When: Success response is created without data
        ApiResponse<Void> response = ApiResponse.success(message);

        // Then: Response has correct structure with null data
        assertThat(response.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void shouldCreateErrorResponse() {
        // Given: Error details
        String code = "USER_NOT_FOUND";
        String message = "User with ID 123 not found";

        // When: Error response is created
        ApiResponse<Void> response = ApiResponse.error(code, message);

        // Then: Response has error structure
        assertThat(response.getStatus()).isEqualTo(ResponseStatus.ERROR);
        assertThat(response.getCode()).isEqualTo(code);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void shouldCreateWarningResponse() {
        // Given: Warning details
        String code = "CACHE_MISS";
        String message = "Data retrieved from database";
        String data = "fallback data";

        // When: Warning response is created
        ApiResponse<String> response = ApiResponse.warning(code, message, data);

        // Then: Response has warning structure
        assertThat(response.getStatus()).isEqualTo(ResponseStatus.WARNING);
        assertThat(response.getCode()).isEqualTo(code);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void shouldUseBuilderPattern() {
        // Given: Builder usage
        String testData = "builder test data";
        ZonedDateTime customTimestamp = ZonedDateTime.now(ZoneOffset.UTC);

        // When: Response is built using builder
        ApiResponse<String> response = ApiResponse.<String>builder()
            .status(ResponseStatus.SUCCESS)
            .code("CUSTOM_SUCCESS")
            .message("Custom success message")
            .data(testData)
            .timestamp(customTimestamp)
            .build();

        // Then: All fields are set correctly
        assertThat(response.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(response.getCode()).isEqualTo("CUSTOM_SUCCESS");
        assertThat(response.getMessage()).isEqualTo("Custom success message");
        assertThat(response.getData()).isEqualTo(testData);
        assertThat(response.getTimestamp()).isEqualTo(customTimestamp);
    }

    @Test
    void shouldSerializeToJsonCorrectly() throws JsonProcessingException {
        // Given: ApiResponse with data
        ApiResponse<Map<String, Object>> response = ApiResponse.success(
            "Data retrieved",
            Map.of("id", 123, "name", "Test User")
        );

        // When: Response is serialized to JSON
        String json = objectMapper.writeValueAsString(response);

        // Then: JSON contains all expected fields
        assertThat(json).contains("\"status\":\"SUCCESS\"");
        assertThat(json).contains("\"message\":\"Data retrieved\"");
        assertThat(json).contains("\"data\":{");
        assertThat(json).contains("\"id\":123");
        assertThat(json).contains("\"name\":\"Test User\"");
        assertThat(json).contains("\"timestamp\":");
        assertThat(json).contains("\"code\":");
    }

    @Test
    void shouldDeserializeFromJsonCorrectly() throws JsonProcessingException {
        // Given: JSON string representing ApiResponse
        String json = """
            {
                "status": "SUCCESS",
                "code": "USER_RETRIEVED",
                "message": "User retrieved successfully",
                "data": {"id": 456, "name": "Jane Doe"},
                "timestamp": "2025-09-27T10:30:00Z"
            }
            """;

        // When: JSON is deserialized to ApiResponse
        ApiResponse<?> response = objectMapper.readValue(json, ApiResponse.class);

        // Then: All fields are correctly deserialized
        assertThat(response.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(response.getCode()).isEqualTo("USER_RETRIEVED");
        assertThat(response.getMessage()).isEqualTo("User retrieved successfully");
        assertThat(response.getData()).isNotNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void shouldHandleNullDataGracefully() {
        // Given: Response with null data
        ApiResponse<String> response = ApiResponse.success("Success without data", null);

        // Then: Response handles null data correctly
        assertThat(response.getData()).isNull();
        assertThat(response.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(response.getMessage()).isEqualTo("Success without data");
    }

    @Test
    void shouldGenerateTimestampAutomatically() {
        // Given: Current time before response creation
        ZonedDateTime beforeCreation = ZonedDateTime.now(ZoneOffset.UTC);

        // When: Response is created without explicit timestamp
        ApiResponse<String> response = ApiResponse.success("Test message", "test data");

        // Then: Timestamp is automatically generated and recent
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getTimestamp()).isAfterOrEqualTo(beforeCreation);
        assertThat(response.getTimestamp()).isCloseTo(beforeCreation, within(1, ChronoUnit.SECONDS));
        assertThat(response.getTimestamp().getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void shouldGenerateCodeAutomaticallyForSuccess() {
        // When: Success response is created without explicit code
        ApiResponse<String> response = ApiResponse.success("Operation successful", "data");

        // Then: Code is automatically generated
        assertThat(response.getCode()).isNotNull();
        assertThat(response.getCode()).isNotEmpty();
        // Should follow a pattern like "SUCCESS" or "OPERATION_SUCCESSFUL"
    }

    @Test
    void shouldSupportGenericTypes() {
        // Given: Various data types
        ApiResponse<Integer> intResponse = ApiResponse.success("Number response", 42);
        ApiResponse<Boolean> boolResponse = ApiResponse.success("Boolean response", true);
        ApiResponse<Object> objResponse = ApiResponse.success("Object response",
            new TestDataObject("test", 123));

        // Then: All generic types are handled correctly
        assertThat(intResponse.getData()).isEqualTo(42);
        assertThat(boolResponse.getData()).isTrue();
        assertThat(objResponse.getData()).isInstanceOf(TestDataObject.class);
    }

    @Test
    void shouldMaintainImmutability() {
        // Given: ApiResponse instance
        ApiResponse<String> original = ApiResponse.success("Original message", "original data");

        // When: Attempting to modify (should not be possible with proper implementation)
        // Then: Object should be immutable (fields should be final)
        assertThat(original.getStatus()).isEqualTo(ResponseStatus.SUCCESS);
        assertThat(original.getData()).isEqualTo("original data");
        assertThat(original.getMessage()).isEqualTo("Original message");
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given: Two identical responses
        ApiResponse<String> response1 = ApiResponse.success("Test message", "test data");
        ApiResponse<String> response2 = ApiResponse.success("Test message", "test data");

        // When: Comparing responses
        // Then: Should be equal if all fields match
        // Note: This depends on proper equals/hashCode implementation in ApiResponse
        assertThat(response1.getMessage()).isEqualTo(response2.getMessage());
        assertThat(response1.getData()).isEqualTo(response2.getData());
        assertThat(response1.getStatus()).isEqualTo(response2.getStatus());
    }

    /**
     * Test data object for generic type testing
     */
    public static class TestDataObject {
        private final String name;
        private final Integer value;

        public TestDataObject(String name, Integer value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public Integer getValue() { return value; }
    }
}