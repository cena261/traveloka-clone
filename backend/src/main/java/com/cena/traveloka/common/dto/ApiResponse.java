package com.cena.traveloka.common.dto;

import com.cena.traveloka.common.enums.ResponseStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;

/**
 * Standard wrapper for all API responses in the application.
 * Provides consistent response structure across all endpoints.
 *
 * Features:
 * - Standardized response status (SUCCESS, ERROR, WARNING)
 * - Response code for specific operation identification
 * - Human-readable message
 * - Generic data payload support
 * - UTC timestamp for response generation
 * - Builder pattern for flexible construction
 * - JSON serialization support
 *
 * @param <T> The type of data being returned in the response
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final ResponseStatus status;
    private final String code;
    private final String message;
    private final T data;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private final ZonedDateTime timestamp;

    /**
     * Private constructor for builder pattern
     */
    private ApiResponse(Builder<T> builder) {
        this.status = builder.status;
        this.code = builder.code != null ? builder.code : generateDefaultCode(builder.status);
        this.message = builder.message;
        this.data = builder.data;
        this.timestamp = builder.timestamp != null ? builder.timestamp : ZonedDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Creates a successful response with message and data
     *
     * @param message Success message
     * @param data Response data
     * @param <T> Type of response data
     * @return ApiResponse with SUCCESS status
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .status(ResponseStatus.SUCCESS)
            .message(message)
            .data(data)
            .build();
    }

    /**
     * Creates a successful response with message only (no data)
     *
     * @param message Success message
     * @return ApiResponse with SUCCESS status and null data
     */
    public static ApiResponse<Void> success(String message) {
        return ApiResponse.<Void>builder()
            .status(ResponseStatus.SUCCESS)
            .message(message)
            .build();
    }

    /**
     * Creates an error response with code and message
     *
     * @param code Error code
     * @param message Error message
     * @return ApiResponse with ERROR status
     */
    public static ApiResponse<Void> error(String code, String message) {
        return ApiResponse.<Void>builder()
            .status(ResponseStatus.ERROR)
            .code(code)
            .message(message)
            .build();
    }

    /**
     * Creates a warning response with code, message, and data
     *
     * @param code Warning code
     * @param message Warning message
     * @param data Response data
     * @param <T> Type of response data
     * @return ApiResponse with WARNING status
     */
    public static <T> ApiResponse<T> warning(String code, String message, T data) {
        return ApiResponse.<T>builder()
            .status(ResponseStatus.WARNING)
            .code(code)
            .message(message)
            .data(data)
            .build();
    }

    /**
     * Returns a new builder for constructing ApiResponse instances
     *
     * @param <T> Type of response data
     * @return Builder instance
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Generates a default code based on the response status
     *
     * @param status The response status
     * @return Default code string
     */
    private String generateDefaultCode(ResponseStatus status) {
        return switch (status) {
            case SUCCESS -> "SUCCESS";
            case ERROR -> "ERROR";
            case WARNING -> "WARNING";
        };
    }

    /**
     * Gets the response status
     *
     * @return ResponseStatus enum value
     */
    public ResponseStatus getStatus() {
        return status;
    }

    /**
     * Gets the response code
     *
     * @return Response code string
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets the response message
     *
     * @return Response message string
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the response data
     *
     * @return Response data of type T
     */
    public T getData() {
        return data;
    }

    /**
     * Gets the response timestamp
     *
     * @return ZonedDateTime in UTC
     */
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Builder class for constructing ApiResponse instances
     *
     * @param <T> Type of response data
     */
    public static class Builder<T> {
        private ResponseStatus status;
        private String code;
        private String message;
        private T data;
        private ZonedDateTime timestamp;

        /**
         * Sets the response status
         *
         * @param status ResponseStatus enum value
         * @return Builder instance for method chaining
         */
        public Builder<T> status(ResponseStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the response code
         *
         * @param code Response code string
         * @return Builder instance for method chaining
         */
        public Builder<T> code(String code) {
            this.code = code;
            return this;
        }

        /**
         * Sets the response message
         *
         * @param message Response message string
         * @return Builder instance for method chaining
         */
        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the response data
         *
         * @param data Response data of type T
         * @return Builder instance for method chaining
         */
        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        /**
         * Sets a custom timestamp
         *
         * @param timestamp ZonedDateTime for the response
         * @return Builder instance for method chaining
         */
        public Builder<T> timestamp(ZonedDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Builds the ApiResponse instance
         *
         * @return ApiResponse instance
         * @throws IllegalArgumentException if required fields are missing
         */
        public ApiResponse<T> build() {
            if (status == null) {
                throw new IllegalArgumentException("Response status is required");
            }
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("Response message is required");
            }
            return new ApiResponse<>(this);
        }
    }

    @Override
    public String toString() {
        return String.format("ApiResponse{status=%s, code='%s', message='%s', data=%s, timestamp=%s}",
            status, code, message, data != null ? data.getClass().getSimpleName() : "null", timestamp);
    }
}