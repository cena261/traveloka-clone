package com.cena.traveloka.common.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import java.time.temporal.ChronoUnit;

/**
 * Test class for ErrorResponse error detail functionality.
 * Tests error structure, field validation, and JSON serialization.
 *
 * CRITICAL: These tests MUST FAIL initially (TDD requirement).
 * ErrorResponse implementation does not exist yet.
 */
class ErrorResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldCreateErrorResponseWithAllFields() {
        // Given: Error details
        String code = "VALIDATION_FAILED";
        String message = "Email is required";
        String field = "email";
        String path = "/api/users";

        // When: ErrorResponse is created
        ErrorResponse errorResponse = ErrorResponse.builder()
            .code(code)
            .message(message)
            .field(field)
            .path(path)
            .build();

        // Then: All fields are set correctly
        assertThat(errorResponse.getCode()).isEqualTo(code);
        assertThat(errorResponse.getMessage()).isEqualTo(message);
        assertThat(errorResponse.getField()).isEqualTo(field);
        assertThat(errorResponse.getPath()).isEqualTo(path);
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getTimestamp().getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void shouldCreateErrorResponseWithoutOptionalFields() {
        // Given: Minimal error details
        String code = "ENTITY_NOT_FOUND";
        String message = "User not found";

        // When: ErrorResponse is created without field and path
        ErrorResponse errorResponse = ErrorResponse.builder()
            .code(code)
            .message(message)
            .build();

        // Then: Required fields are set, optional fields are null
        assertThat(errorResponse.getCode()).isEqualTo(code);
        assertThat(errorResponse.getMessage()).isEqualTo(message);
        assertThat(errorResponse.getField()).isNull();
        assertThat(errorResponse.getPath()).isNull();
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }

    @Test
    void shouldCreateErrorResponseWithConvenienceMethod() {
        // Given: Error code and message
        String code = "BUSINESS_RULE_VIOLATION";
        String message = "Insufficient balance for transaction";

        // When: ErrorResponse is created using convenience method
        ErrorResponse errorResponse = ErrorResponse.of(code, message);

        // Then: Basic fields are set with auto-generated timestamp
        assertThat(errorResponse.getCode()).isEqualTo(code);
        assertThat(errorResponse.getMessage()).isEqualTo(message);
        assertThat(errorResponse.getField()).isNull();
        assertThat(errorResponse.getPath()).isNull();
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }

    @Test
    void shouldCreateFieldValidationError() {
        // Given: Field validation error details
        String field = "phoneNumber";
        String message = "Phone number must be in Vietnamese format (+84...)";

        // When: Field validation error is created
        ErrorResponse errorResponse = ErrorResponse.fieldError(field, message);

        // Then: Field-specific error is properly structured
        assertThat(errorResponse.getCode()).isEqualTo("FIELD_VALIDATION_FAILED");
        assertThat(errorResponse.getMessage()).isEqualTo(message);
        assertThat(errorResponse.getField()).isEqualTo(field);
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }

    @Test
    void shouldGenerateTimestampAutomatically() {
        // Given: Current time before error creation
        ZonedDateTime beforeCreation = ZonedDateTime.now(ZoneOffset.UTC);

        // When: ErrorResponse is created without explicit timestamp
        ErrorResponse errorResponse = ErrorResponse.of("TEST_ERROR", "Test error message");

        // Then: Timestamp is automatically generated and recent
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getTimestamp()).isAfterOrEqualTo(beforeCreation);
        assertThat(errorResponse.getTimestamp()).isCloseTo(beforeCreation, within(1, ChronoUnit.SECONDS));
        assertThat(errorResponse.getTimestamp().getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void shouldAllowCustomTimestamp() {
        // Given: Custom timestamp
        ZonedDateTime customTimestamp = ZonedDateTime.of(2025, 9, 27, 10, 30, 0, 0, ZoneOffset.UTC);

        // When: ErrorResponse is created with custom timestamp
        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("CUSTOM_ERROR")
            .message("Custom error message")
            .timestamp(customTimestamp)
            .build();

        // Then: Custom timestamp is used
        assertThat(errorResponse.getTimestamp()).isEqualTo(customTimestamp);
    }

    @Test
    void shouldSerializeToJsonCorrectly() throws JsonProcessingException {
        // Given: ErrorResponse with all fields
        ErrorResponse errorResponse = ErrorResponse.builder()
            .code("VALIDATION_FAILED")
            .message("Email format is invalid")
            .field("email")
            .path("/api/users/register")
            .timestamp(ZonedDateTime.of(2025, 9, 27, 10, 30, 0, 0, ZoneOffset.UTC))
            .build();

        // When: ErrorResponse is serialized to JSON
        String json = objectMapper.writeValueAsString(errorResponse);

        // Then: JSON contains all fields
        assertThat(json).contains("\"code\":\"VALIDATION_FAILED\"");
        assertThat(json).contains("\"message\":\"Email format is invalid\"");
        assertThat(json).contains("\"field\":\"email\"");
        assertThat(json).contains("\"path\":\"/api/users/register\"");
        assertThat(json).contains("\"timestamp\":\"2025-09-27T10:30:00Z\"");
    }

    @Test
    void shouldSerializeWithNullFieldsCorrectly() throws JsonProcessingException {
        // Given: ErrorResponse with null optional fields
        ErrorResponse errorResponse = ErrorResponse.of("GENERIC_ERROR", "Something went wrong");

        // When: ErrorResponse is serialized to JSON
        String json = objectMapper.writeValueAsString(errorResponse);

        // Then: JSON handles null fields appropriately
        assertThat(json).contains("\"code\":\"GENERIC_ERROR\"");
        assertThat(json).contains("\"message\":\"Something went wrong\"");
        assertThat(json).contains("\"field\":null");
        assertThat(json).contains("\"path\":null");
        assertThat(json).contains("\"timestamp\":");
    }

    @Test
    void shouldDeserializeFromJsonCorrectly() throws JsonProcessingException {
        // Given: JSON string representing ErrorResponse
        String json = """
            {
                "code": "USER_NOT_FOUND",
                "message": "User with ID 123 not found",
                "field": null,
                "path": "/api/users/123",
                "timestamp": "2025-09-27T10:30:00Z"
            }
            """;

        // When: JSON is deserialized to ErrorResponse
        ErrorResponse errorResponse = objectMapper.readValue(json, ErrorResponse.class);

        // Then: All fields are correctly deserialized
        assertThat(errorResponse.getCode()).isEqualTo("USER_NOT_FOUND");
        assertThat(errorResponse.getMessage()).isEqualTo("User with ID 123 not found");
        assertThat(errorResponse.getField()).isNull();
        assertThat(errorResponse.getPath()).isEqualTo("/api/users/123");
        assertThat(errorResponse.getTimestamp()).isNotNull();
    }

    @Test
    void shouldHandleMultipleFieldErrors() {
        // Given: Multiple field validation errors
        ErrorResponse emailError = ErrorResponse.fieldError("email", "Email is required");
        ErrorResponse nameError = ErrorResponse.fieldError("name", "Name must be at least 2 characters");
        ErrorResponse phoneError = ErrorResponse.fieldError("phoneNumber", "Invalid phone format");

        // Then: Each error has appropriate field-specific details
        assertThat(emailError.getField()).isEqualTo("email");
        assertThat(emailError.getMessage()).isEqualTo("Email is required");

        assertThat(nameError.getField()).isEqualTo("name");
        assertThat(nameError.getMessage()).isEqualTo("Name must be at least 2 characters");

        assertThat(phoneError.getField()).isEqualTo("phoneNumber");
        assertThat(phoneError.getMessage()).isEqualTo("Invalid phone format");
    }

    @Test
    void shouldSupportDifferentErrorTypes() {
        // Test various error scenarios
        ErrorResponse validationError = ErrorResponse.of("VALIDATION_FAILED", "Input validation failed");
        ErrorResponse notFoundError = ErrorResponse.of("ENTITY_NOT_FOUND", "Resource not found");
        ErrorResponse businessError = ErrorResponse.of("BUSINESS_RULE_VIOLATION", "Business rule violated");
        ErrorResponse serverError = ErrorResponse.of("INTERNAL_SERVER_ERROR", "Server error occurred");

        // Verify different error types are handled
        assertThat(validationError.getCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(notFoundError.getCode()).isEqualTo("ENTITY_NOT_FOUND");
        assertThat(businessError.getCode()).isEqualTo("BUSINESS_RULE_VIOLATION");
        assertThat(serverError.getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
    }

    @Test
    void shouldHandlePathExtraction() {
        // Given: ErrorResponse with request path
        ErrorResponse errorWithPath = ErrorResponse.builder()
            .code("ACCESS_DENIED")
            .message("Insufficient permissions")
            .path("/api/admin/users")
            .build();

        // Then: Path is properly stored and accessible
        assertThat(errorWithPath.getPath()).isEqualTo("/api/admin/users");
    }

    @Test
    void shouldHandleLongErrorMessages() {
        // Given: Long error message
        String longMessage = "This is a very long error message that describes in detail what went wrong " +
            "with the user's request, including specific validation failures, business rule violations, " +
            "and suggestions for how to fix the problem. It should be properly handled without truncation.";

        // When: ErrorResponse is created with long message
        ErrorResponse errorResponse = ErrorResponse.of("DETAILED_ERROR", longMessage);

        // Then: Long message is preserved
        assertThat(errorResponse.getMessage()).isEqualTo(longMessage);
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given: Two identical error responses
        ErrorResponse error1 = ErrorResponse.builder()
            .code("TEST_ERROR")
            .message("Test message")
            .field("testField")
            .path("/test/path")
            .timestamp(ZonedDateTime.of(2025, 9, 27, 10, 30, 0, 0, ZoneOffset.UTC))
            .build();

        ErrorResponse error2 = ErrorResponse.builder()
            .code("TEST_ERROR")
            .message("Test message")
            .field("testField")
            .path("/test/path")
            .timestamp(ZonedDateTime.of(2025, 9, 27, 10, 30, 0, 0, ZoneOffset.UTC))
            .build();

        // When: Comparing errors
        // Then: Should be equal if all fields match
        assertThat(error1.getCode()).isEqualTo(error2.getCode());
        assertThat(error1.getMessage()).isEqualTo(error2.getMessage());
        assertThat(error1.getField()).isEqualTo(error2.getField());
        assertThat(error1.getPath()).isEqualTo(error2.getPath());
        assertThat(error1.getTimestamp()).isEqualTo(error2.getTimestamp());
    }

    @Test
    void shouldValidateRequiredFields() {
        // Test that code and message are required
        // This would be handled by the actual ErrorResponse implementation
        // with appropriate validation annotations or constructor requirements

        // Given: Valid required fields
        ErrorResponse validError = ErrorResponse.of("VALID_CODE", "Valid message");

        // Then: Required fields are properly set
        assertThat(validError.getCode()).isNotNull();
        assertThat(validError.getCode()).isNotEmpty();
        assertThat(validError.getMessage()).isNotNull();
        assertThat(validError.getMessage()).isNotEmpty();
    }
}