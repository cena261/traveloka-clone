package com.cena.traveloka.iam.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;

/**
 * Generic API response wrapper for consistent response format
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;

    private String message;

    private T data;

    private ErrorResponse error;

    private Instant timestamp;

    private String traceId;

    public ApiResponse() {
        this.timestamp = Instant.now();
    }

    public ApiResponse(boolean success, String message, T data) {
        this();
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public ApiResponse(boolean success, String message, T data, String traceId) {
        this(success, message, data);
        this.traceId = traceId;
    }

    // === Static Factory Methods for Success ===

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> success(String message, T data, String traceId) {
        return new ApiResponse<>(true, message, data, traceId);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null);
    }

    // === Static Factory Methods for Errors ===

    public static <T> ApiResponse<T> error(ErrorResponse error) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage("Request failed");
        response.setError(error);
        return response;
    }

    public static <T> ApiResponse<T> error(String message, ErrorResponse error) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setError(error);
        return response;
    }

    public static <T> ApiResponse<T> error(String message, ErrorResponse error, String traceId) {
        ApiResponse<T> response = error(message, error);
        response.setTraceId(traceId);
        return response;
    }

    // === Convenience Error Methods ===

    public static <T> ApiResponse<T> badRequest(String message) {
        return error(ErrorResponse.badRequest(message));
    }

    public static <T> ApiResponse<T> unauthorized(String message) {
        return error(ErrorResponse.unauthorized(message));
    }

    public static <T> ApiResponse<T> forbidden(String message) {
        return error(ErrorResponse.forbidden(message));
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return error(ErrorResponse.notFound(message));
    }

    public static <T> ApiResponse<T> conflict(String message) {
        return error(ErrorResponse.conflict(message));
    }

    public static <T> ApiResponse<T> internalServerError(String message) {
        return error(ErrorResponse.internalServerError(message));
    }

    public static <T> ApiResponse<T> serviceUnavailable(String message) {
        return error(ErrorResponse.serviceUnavailable(message));
    }

    // === Builder Pattern ===

    public static <T> ApiResponseBuilder<T> builder() {
        return new ApiResponseBuilder<>();
    }

    public static class ApiResponseBuilder<T> {
        private final ApiResponse<T> response = new ApiResponse<>();

        public ApiResponseBuilder<T> success(boolean success) {
            response.setSuccess(success);
            return this;
        }

        public ApiResponseBuilder<T> message(String message) {
            response.setMessage(message);
            return this;
        }

        public ApiResponseBuilder<T> data(T data) {
            response.setData(data);
            return this;
        }

        public ApiResponseBuilder<T> error(ErrorResponse error) {
            response.setError(error);
            response.setSuccess(false);
            return this;
        }

        public ApiResponseBuilder<T> traceId(String traceId) {
            response.setTraceId(traceId);
            return this;
        }

        public ApiResponse<T> build() {
            return response;
        }
    }
}