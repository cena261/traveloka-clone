package com.cena.traveloka.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;

/**
 * Standard error response structure for all API error responses.
 * Provides consistent error format across the entire application.
 *
 * Features:
 * - Standardized error code and message
 * - Optional field-specific error details
 * - Request path for debugging
 * - UTC timestamp for error occurrence
 * - Builder pattern for flexible construction
 * - JSON serialization support
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String code;
    private final String message;
    private final String field;
    private final String path;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private final ZonedDateTime timestamp;

    /**
     * Private constructor for builder pattern
     */
    private ErrorResponse(Builder builder) {
        this.code = builder.code;
        this.message = builder.message;
        this.field = builder.field;
        this.path = builder.path;
        this.timestamp = builder.timestamp != null ? builder.timestamp : ZonedDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Creates a simple error response with code and message
     *
     * @param code Error code identifying the type of error
     * @param message Human-readable error message
     * @return ErrorResponse instance
     */
    public static ErrorResponse of(String code, String message) {
        return builder()
            .code(code)
            .message(message)
            .build();
    }

    /**
     * Creates a field-specific validation error response
     *
     * @param field Name of the field that failed validation
     * @param message Validation error message
     * @return ErrorResponse instance for field validation
     */
    public static ErrorResponse fieldError(String field, String message) {
        return builder()
            .code("FIELD_VALIDATION_FAILED")
            .message(message)
            .field(field)
            .build();
    }

    /**
     * Returns a new builder for constructing ErrorResponse instances
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the error code
     *
     * @return Error code string
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the error message
     *
     * @return Error message string
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the field name (for validation errors)
     *
     * @return Field name or null if not a field-specific error
     */
    public String getField() {
        return field;
    }

    /**
     * Gets the request path where the error occurred
     *
     * @return Request path string or null if not available
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the timestamp when the error occurred
     *
     * @return ZonedDateTime in UTC
     */
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Builder class for constructing ErrorResponse instances
     */
    public static class Builder {
        private String code;
        private String message;
        private String field;
        private String path;
        private ZonedDateTime timestamp;

        /**
         * Sets the error code
         *
         * @param code Error code string
         * @return Builder instance for method chaining
         */
        public Builder code(String code) {
            this.code = code;
            return this;
        }

        /**
         * Sets the error message
         *
         * @param message Error message string
         * @return Builder instance for method chaining
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the field name for validation errors
         *
         * @param field Field name string
         * @return Builder instance for method chaining
         */
        public Builder field(String field) {
            this.field = field;
            return this;
        }

        /**
         * Sets the request path
         *
         * @param path Request path string
         * @return Builder instance for method chaining
         */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        /**
         * Sets a custom timestamp
         *
         * @param timestamp ZonedDateTime for the error occurrence
         * @return Builder instance for method chaining
         */
        public Builder timestamp(ZonedDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Builds the ErrorResponse instance
         *
         * @return ErrorResponse instance
         * @throws IllegalArgumentException if required fields are missing
         */
        public ErrorResponse build() {
            if (code == null || code.trim().isEmpty()) {
                throw new IllegalArgumentException("Error code is required");
            }
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("Error message is required");
            }
            return new ErrorResponse(this);
        }
    }

    @Override
    public String toString() {
        return String.format("ErrorResponse{code='%s', message='%s', field='%s', path='%s', timestamp=%s}",
            code, message, field, path, timestamp);
    }
}