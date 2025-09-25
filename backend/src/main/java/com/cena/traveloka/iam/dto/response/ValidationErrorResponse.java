package com.cena.traveloka.iam.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Specialized error response for validation failures
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationErrorResponse {

    private String error = "validation_failed";

    private String errorDescription = "Request validation failed";

    private String errorCode = "IAM_VALIDATION_FAILED";

    private Integer status = 400;

    private String path;

    private String method;

    private Instant timestamp;

    private String traceId;

    private List<FieldError> fieldErrors;

    private List<GlobalError> globalErrors;

    private Integer errorCount;

    public ValidationErrorResponse() {
        this.timestamp = Instant.now();
        this.fieldErrors = new ArrayList<>();
        this.globalErrors = new ArrayList<>();
    }

    /**
     * Field-level validation error
     */
    @Data
    public static class FieldError {
        private String field;
        private Object rejectedValue;
        private String message;
        private String code;
        private Map<String, Object> arguments;

        public FieldError() {}

        public FieldError(String field, Object rejectedValue, String message) {
            this.field = field;
            this.rejectedValue = rejectedValue;
            this.message = message;
        }

        public FieldError(String field, Object rejectedValue, String message, String code) {
            this(field, rejectedValue, message);
            this.code = code;
        }
    }

    /**
     * Global validation error (not tied to specific field)
     */
    @Data
    public static class GlobalError {
        private String objectName;
        private String message;
        private String code;
        private Map<String, Object> arguments;

        public GlobalError() {}

        public GlobalError(String objectName, String message) {
            this.objectName = objectName;
            this.message = message;
        }

        public GlobalError(String objectName, String message, String code) {
            this(objectName, message);
            this.code = code;
        }
    }

    // === Builder Methods ===

    public ValidationErrorResponse addFieldError(String field, Object rejectedValue, String message) {
        this.fieldErrors.add(new FieldError(field, rejectedValue, message));
        updateErrorCount();
        return this;
    }

    public ValidationErrorResponse addFieldError(String field, Object rejectedValue, String message, String code) {
        this.fieldErrors.add(new FieldError(field, rejectedValue, message, code));
        updateErrorCount();
        return this;
    }

    public ValidationErrorResponse addGlobalError(String objectName, String message) {
        this.globalErrors.add(new GlobalError(objectName, message));
        updateErrorCount();
        return this;
    }

    public ValidationErrorResponse addGlobalError(String objectName, String message, String code) {
        this.globalErrors.add(new GlobalError(objectName, message, code));
        updateErrorCount();
        return this;
    }

    private void updateErrorCount() {
        this.errorCount = this.fieldErrors.size() + this.globalErrors.size();
    }

    // === Static Factory Methods ===

    public static ValidationErrorResponse create() {
        return new ValidationErrorResponse();
    }

    public static ValidationErrorResponse create(String path, String method) {
        ValidationErrorResponse response = new ValidationErrorResponse();
        response.setPath(path);
        response.setMethod(method);
        return response;
    }

    public static ValidationErrorResponse create(String path, String method, String traceId) {
        ValidationErrorResponse response = create(path, method);
        response.setTraceId(traceId);
        return response;
    }

    public static ValidationErrorResponse fromFieldErrors(List<FieldError> fieldErrors) {
        ValidationErrorResponse response = new ValidationErrorResponse();
        response.setFieldErrors(fieldErrors);
        response.updateErrorCount();
        return response;
    }

    public static ValidationErrorResponse single(String field, Object rejectedValue, String message) {
        ValidationErrorResponse response = new ValidationErrorResponse();
        response.addFieldError(field, rejectedValue, message);
        return response;
    }

    public static ValidationErrorResponse single(String field, Object rejectedValue, String message, String code) {
        ValidationErrorResponse response = new ValidationErrorResponse();
        response.addFieldError(field, rejectedValue, message, code);
        return response;
    }

    // === Convenience Methods ===

    public boolean hasErrors() {
        return (fieldErrors != null && !fieldErrors.isEmpty()) ||
               (globalErrors != null && !globalErrors.isEmpty());
    }

    public boolean hasFieldErrors() {
        return fieldErrors != null && !fieldErrors.isEmpty();
    }

    public boolean hasGlobalErrors() {
        return globalErrors != null && !globalErrors.isEmpty();
    }

    public boolean hasFieldError(String field) {
        return fieldErrors != null &&
               fieldErrors.stream().anyMatch(error -> field.equals(error.getField()));
    }

    public List<FieldError> getFieldErrors(String field) {
        if (fieldErrors == null) {
            return new ArrayList<>();
        }
        return fieldErrors.stream()
                .filter(error -> field.equals(error.getField()))
                .toList();
    }
}