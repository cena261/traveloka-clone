package com.cena.traveloka.iam.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Standard error response DTO for IAM module
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String error;

    private String errorDescription;

    private String errorCode;

    private Integer status;

    private String path;

    private String method;

    private Instant timestamp;

    private String traceId;

    private List<ValidationError> validationErrors;

    private Map<String, Object> details;

    public ErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ErrorResponse(String error, String errorDescription) {
        this();
        this.error = error;
        this.errorDescription = errorDescription;
    }

    public ErrorResponse(String error, String errorDescription, String errorCode) {
        this(error, errorDescription);
        this.errorCode = errorCode;
    }

    public ErrorResponse(String error, String errorDescription, String errorCode, Integer status) {
        this(error, errorDescription, errorCode);
        this.status = status;
    }

    /**
     * Validation error details
     */
    @Data
    public static class ValidationError {
        private String field;
        private Object rejectedValue;
        private String message;
        private String code;

        public ValidationError() {}

        public ValidationError(String field, Object rejectedValue, String message) {
            this.field = field;
            this.rejectedValue = rejectedValue;
            this.message = message;
        }

        public ValidationError(String field, Object rejectedValue, String message, String code) {
            this(field, rejectedValue, message);
            this.code = code;
        }
    }

    // === Builder Pattern ===

    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }

    public static class ErrorResponseBuilder {
        private final ErrorResponse response = new ErrorResponse();

        public ErrorResponseBuilder error(String error) {
            response.setError(error);
            return this;
        }

        public ErrorResponseBuilder errorDescription(String description) {
            response.setErrorDescription(description);
            return this;
        }

        public ErrorResponseBuilder errorCode(String code) {
            response.setErrorCode(code);
            return this;
        }

        public ErrorResponseBuilder status(Integer status) {
            response.setStatus(status);
            return this;
        }

        public ErrorResponseBuilder path(String path) {
            response.setPath(path);
            return this;
        }

        public ErrorResponseBuilder method(String method) {
            response.setMethod(method);
            return this;
        }

        public ErrorResponseBuilder traceId(String traceId) {
            response.setTraceId(traceId);
            return this;
        }

        public ErrorResponseBuilder validationErrors(List<ValidationError> errors) {
            response.setValidationErrors(errors);
            return this;
        }

        public ErrorResponseBuilder details(Map<String, Object> details) {
            response.setDetails(details);
            return this;
        }

        public ErrorResponse build() {
            return response;
        }
    }

    // === Static Factory Methods ===

    public static ErrorResponse badRequest(String message) {
        return ErrorResponse.builder()
                .error("bad_request")
                .errorDescription(message)
                .errorCode("IAM_BAD_REQUEST")
                .status(400)
                .build();
    }

    public static ErrorResponse unauthorized(String message) {
        return ErrorResponse.builder()
                .error("unauthorized")
                .errorDescription(message)
                .errorCode("IAM_UNAUTHORIZED")
                .status(401)
                .build();
    }

    public static ErrorResponse forbidden(String message) {
        return ErrorResponse.builder()
                .error("forbidden")
                .errorDescription(message)
                .errorCode("IAM_FORBIDDEN")
                .status(403)
                .build();
    }

    public static ErrorResponse notFound(String message) {
        return ErrorResponse.builder()
                .error("not_found")
                .errorDescription(message)
                .errorCode("IAM_NOT_FOUND")
                .status(404)
                .build();
    }

    public static ErrorResponse conflict(String message) {
        return ErrorResponse.builder()
                .error("conflict")
                .errorDescription(message)
                .errorCode("IAM_CONFLICT")
                .status(409)
                .build();
    }

    public static ErrorResponse validationFailed(String message, List<ValidationError> errors) {
        return ErrorResponse.builder()
                .error("validation_failed")
                .errorDescription(message)
                .errorCode("IAM_VALIDATION_FAILED")
                .status(400)
                .validationErrors(errors)
                .build();
    }

    public static ErrorResponse validationFailed(String message, Map<String, String> errors) {
        List<ValidationError> validationErrors = new ArrayList<>();
        if (errors != null) {
            for (Map.Entry<String, String> entry : errors.entrySet()) {
                validationErrors.add(new ValidationError(entry.getKey(), null, entry.getValue()));
            }
        }
        return validationFailed(message, validationErrors);
    }

    public static ErrorResponse internalServerError(String message) {
        return ErrorResponse.builder()
                .error("internal_server_error")
                .errorDescription(message)
                .errorCode("IAM_INTERNAL_ERROR")
                .status(500)
                .build();
    }

    public static ErrorResponse serviceUnavailable(String message) {
        return ErrorResponse.builder()
                .error("service_unavailable")
                .errorDescription(message)
                .errorCode("IAM_SERVICE_UNAVAILABLE")
                .status(503)
                .build();
    }
}