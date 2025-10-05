package com.cena.traveloka.common.exception;

import java.util.Map;
import java.util.List;

public class ValidationException extends RuntimeException {

    private final String field;
    private final Map<String, String> fieldErrors;
    private final List<String> generalErrors;

    public ValidationException(String message) {
        super(message);
        this.field = null;
        this.fieldErrors = null;
        this.generalErrors = null;
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.field = null;
        this.fieldErrors = null;
        this.generalErrors = null;
    }

    public ValidationException(String field, String message) {
        super(String.format("Validation failed for field '%s': %s", field, message));
        this.field = field;
        this.fieldErrors = null;
        this.generalErrors = null;
    }

    public ValidationException(Map<String, String> fieldErrors) {
        super("Multiple validation errors occurred");
        this.field = null;
        this.fieldErrors = fieldErrors;
        this.generalErrors = null;
    }

    public ValidationException(List<String> generalErrors) {
        super("Multiple validation errors occurred");
        this.field = null;
        this.fieldErrors = null;
        this.generalErrors = generalErrors;
    }

    public ValidationException(Map<String, String> fieldErrors, List<String> generalErrors) {
        super("Multiple validation errors occurred");
        this.field = null;
        this.fieldErrors = fieldErrors;
        this.generalErrors = generalErrors;
    }

    public String getField() {
        return field;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public List<String> getGeneralErrors() {
        return generalErrors;
    }

    public boolean hasFieldErrors() {
        return (field != null) || (fieldErrors != null && !fieldErrors.isEmpty());
    }

    public boolean hasGeneralErrors() {
        return generalErrors != null && !generalErrors.isEmpty();
    }

    public boolean isSingleFieldError() {
        return field != null;
    }

    public boolean hasMultipleErrors() {
        return (fieldErrors != null && fieldErrors.size() > 1) ||
               (generalErrors != null && generalErrors.size() > 1) ||
               (hasFieldErrors() && hasGeneralErrors());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ValidationException{");

        if (field != null) {
            sb.append("field='").append(field).append("', ");
        }

        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            sb.append("fieldErrors=").append(fieldErrors.size()).append(" errors, ");
        }

        if (generalErrors != null && !generalErrors.isEmpty()) {
            sb.append("generalErrors=").append(generalErrors.size()).append(" errors, ");
        }

        sb.append("message='").append(getMessage()).append("'}");
        return sb.toString();
    }
}