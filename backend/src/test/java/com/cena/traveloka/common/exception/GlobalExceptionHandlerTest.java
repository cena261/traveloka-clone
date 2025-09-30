package com.cena.traveloka.common.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import com.cena.traveloka.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolationException;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Test class for GlobalExceptionHandler functionality.
 * Tests standardized error response handling for various exception types.
 *
 * CRITICAL: These tests MUST FAIL initially (TDD requirement).
 * GlobalExceptionHandler and ErrorResponse implementations do not exist yet.
 */
@WebMvcTest(controllers = {TestController.class})
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private GlobalExceptionHandler globalExceptionHandler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/test/endpoint");
    }

    @Test
    void shouldHandleValidationException() {
        // Given: MethodArgumentNotValidException with field errors
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("user", "email", "Email is required");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
            null, bindingResult);

        // When: Exception is handled
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleValidationException(exception, webRequest);

        // Then: Error response is properly formatted
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().getMessage()).contains("Email is required");
        assertThat(response.getBody().getField()).isEqualTo("email");
        assertThat(response.getBody().getPath()).isEqualTo("/test/endpoint");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void shouldHandleEntityNotFoundException() {
        // Given: EntityNotFoundException
        EntityNotFoundException exception = new EntityNotFoundException("User not found with ID: 123");

        // When: Exception is handled
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleEntityNotFound(exception, webRequest);

        // Then: 404 response with error details
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("ENTITY_NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("User not found with ID: 123");
        assertThat(response.getBody().getField()).isNull();
        assertThat(response.getBody().getPath()).isEqualTo("/test/endpoint");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void shouldHandleBusinessException() {
        // Given: BusinessException with custom error code
        BusinessException exception = new BusinessException("INSUFFICIENT_BALANCE",
            "Account balance is insufficient for this transaction");

        // When: Exception is handled
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleBusinessException(exception, webRequest);

        // Then: 400 response with business error details
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INSUFFICIENT_BALANCE");
        assertThat(response.getBody().getMessage()).isEqualTo("Account balance is insufficient for this transaction");
        assertThat(response.getBody().getPath()).isEqualTo("/test/endpoint");
    }

    @Test
    void shouldHandleConstraintViolationException() {
        // Given: ConstraintViolationException
        ConstraintViolationException exception = new ConstraintViolationException(
            "Validation failed", new HashSet<>());

        // When: Exception is handled
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleConstraintViolation(exception, webRequest);

        // Then: 400 response with constraint violation details
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("CONSTRAINT_VIOLATION");
        assertThat(response.getBody().getMessage()).contains("Validation failed");
    }

    @Test
    void shouldHandleGenericException() {
        // Given: Generic runtime exception
        RuntimeException exception = new RuntimeException("Unexpected error occurred");

        // When: Exception is handled
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleGenericException(exception, webRequest);

        // Then: 500 response with generic error
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getPath()).isEqualTo("/test/endpoint");
    }

    @Test
    void shouldHandleValidationExceptionViaController() throws Exception {
        // When: Making request that triggers validation error
        mockMvc.perform(post("/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\",\"name\":\"\"}"))
                // Then: Returns validation error response
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/test/validate"));
    }

    @Test
    void shouldHandleEntityNotFoundViaController() throws Exception {
        // When: Making request that triggers entity not found
        mockMvc.perform(post("/test/not-found")
                .contentType(MediaType.APPLICATION_JSON))
                // Then: Returns 404 error response
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENTITY_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldHandleBusinessRuleViaController() throws Exception {
        // When: Making request that triggers business rule violation
        mockMvc.perform(post("/test/business-error")
                .contentType(MediaType.APPLICATION_JSON))
                // Then: Returns business error response
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldIncludeCorrelationIdInResponse() {
        // Given: Exception with correlation context
        when(webRequest.getHeader("X-Correlation-ID")).thenReturn("test-correlation-123");

        BusinessException exception = new BusinessException("TEST_ERROR", "Test error message");

        // When: Exception is handled
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleBusinessException(exception, webRequest);

        // Then: Response includes correlation ID
        assertThat(response.getHeaders().get("X-Correlation-ID"))
            .contains("test-correlation-123");
    }

    @Test
    void shouldSanitizeErrorMessageForProduction() {
        // Given: Exception with sensitive information
        RuntimeException exception = new RuntimeException("Database connection failed: jdbc://user:password@localhost:5432/db");

        // When: Exception is handled in production mode
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleGenericException(exception, webRequest);

        // Then: Error message is sanitized (not revealing sensitive details)
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getMessage()).doesNotContain("password");
        assertThat(response.getBody().getMessage()).doesNotContain("jdbc");
    }

    @Test
    void shouldReturnTimestampInUtc() {
        // Given: Any exception
        BusinessException exception = new BusinessException("TEST_ERROR", "Test message");

        // When: Exception is handled
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleBusinessException(exception, webRequest);

        // Then: Timestamp is in UTC
        ZonedDateTime timestamp = response.getBody().getTimestamp();
        assertThat(timestamp).isNotNull();
        assertThat(timestamp.getZone().getId()).isEqualTo("Z"); // UTC
    }
}