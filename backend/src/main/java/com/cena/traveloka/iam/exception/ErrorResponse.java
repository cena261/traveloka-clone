package com.cena.traveloka.iam.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Standardized error response structure for IAM module
 *
 * Provides consistent error response format across all IAM endpoints:
 * - Timestamp for debugging and audit trails
 * - HTTP status code for client handling
 * - Error type and message for user display
 * - Request path for context
 * - Error code for programmatic handling
 * - Validation errors for detailed feedback
 *
 * Key Features:
 * - JSON serialization optimized
 * - Excludes null fields from response
 * - Builder pattern for easy construction
 * - Security-aware (no sensitive data exposure)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Timestamp when the error occurred
     */
    private Instant timestamp;

    /**
     * HTTP status code
     */
    private int status;

    /**
     * Short error description
     */
    private String error;

    /**
     * Detailed error message for the user
     */
    private String message;

    /**
     * Request path where the error occurred
     */
    private String path;

    /**
     * Application-specific error code for programmatic handling
     */
    private String code;

    /**
     * Field-specific validation errors
     * Only included when validation fails
     */
    private Map<String, String> validationErrors;

    /**
     * Additional error details or context
     * Used for debugging information
     */
    private Map<String, Object> details;

    /**
     * Correlation ID for distributed tracing
     */
    private String correlationId;

    /**
     * Create a simple error response with basic information
     */
    public static ErrorResponse simple(int status, String error, String message) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .message(message)
                .build();
    }

    /**
     * Create a validation error response with field-specific errors
     */
    public static ErrorResponse validation(String message, Map<String, String> validationErrors) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(400)
                .error("Validation Failed")
                .message(message)
                .validationErrors(validationErrors)
                .build();
    }

    /**
     * Create an authentication error response
     */
    public static ErrorResponse authentication(String message) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(401)
                .error("Authentication Failed")
                .message(message)
                .build();
    }

    /**
     * Create an authorization error response
     */
    public static ErrorResponse authorization(String message) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(403)
                .error("Access Denied")
                .message(message)
                .build();
    }

    /**
     * Create a not found error response
     */
    public static ErrorResponse notFound(String message) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(404)
                .error("Not Found")
                .message(message)
                .build();
    }

    /**
     * Create an internal server error response
     */
    public static ErrorResponse internalError(String message) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(500)
                .error("Internal Server Error")
                .message(message)
                .build();
    }

    /**
     * Create a service unavailable error response
     */
    public static ErrorResponse serviceUnavailable(String message) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(503)
                .error("Service Unavailable")
                .message(message)
                .build();
    }

    /**
     * Create a validation failed error response
     */
    public static ErrorResponse validationFailed(String message, Map<String, String> validationErrors) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(400)
                .error("Validation Failed")
                .message(message)
                .validationErrors(validationErrors)
                .build();
    }

    /**
     * Create an unauthorized error response
     */
    public static ErrorResponse unauthorized(String message) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(401)
                .error("Unauthorized")
                .message(message)
                .build();
    }

    /**
     * Create a forbidden error response
     */
    public static ErrorResponse forbidden(String message) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(403)
                .error("Forbidden")
                .message(message)
                .build();
    }

    /**
     * Create a conflict error response
     */
    public static ErrorResponse conflict(String message) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(409)
                .error("Conflict")
                .message(message)
                .build();
    }

    /**
     * Create a bad request error response
     */
    public static ErrorResponse badRequest(String message) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(400)
                .error("Bad Request")
                .message(message)
                .build();
    }

    /**
     * Create a method not allowed error response
     */
    public static ErrorResponse methodNotAllowed(String message) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(405)
                .error("Method Not Allowed")
                .message(message)
                .build();
    }

    /**
     * Create a too many requests error response
     */
    public static ErrorResponse tooManyRequests(String message) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(429)
                .error("Too Many Requests")
                .message(message)
                .build();
    }

    // === Setter methods for additional context ===

    public void setMethod(String method) {
        if (this.details == null) {
            this.details = new HashMap<>();
        }
        this.details.put("method", method);
    }

    public void setTraceId(String traceId) {
        this.correlationId = traceId;
    }
}