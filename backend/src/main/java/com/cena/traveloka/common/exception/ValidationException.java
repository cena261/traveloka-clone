package com.cena.traveloka.common.exception;

import java.util.Map;
import java.util.List;

/**
 * Exception thrown when validation fails for request data or business rules.
 * This exception is typically used for 400 BAD REQUEST HTTP responses.
 *
 * Features:
 * - Specific exception for validation failure scenarios
 * - Support for single field validation errors
 * - Support for multiple field validation errors
 * - Support for general validation messages
 * - Field-specific error details for client-side handling
 * - Inherits from RuntimeException for easier handling
 */
public class ValidationException extends RuntimeException {

    private final String field;
    private final Map<String, String> fieldErrors;
    private final List<String> generalErrors;

    /**
     * Creates a validation exception with a general validation message
     *
     * @param message General validation error message
     */
    public ValidationException(String message) {
        super(message);
        this.field = null;
        this.fieldErrors = null;
        this.generalErrors = null;
    }

    /**
     * Creates a validation exception with message and cause
     *
     * @param message Validation error message
     * @param cause The underlying cause of the exception
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.field = null;
        this.fieldErrors = null;
        this.generalErrors = null;
    }

    /**
     * Creates a validation exception for a specific field
     *
     * @param field Name of the field that failed validation
     * @param message Field-specific validation error message
     */
    public ValidationException(String field, String message) {
        super(String.format("Validation failed for field '%s': %s", field, message));
        this.field = field;
        this.fieldErrors = null;
        this.generalErrors = null;
    }

    /**
     * Creates a validation exception with multiple field errors
     *
     * @param fieldErrors Map of field names to their respective error messages
     */
    public ValidationException(Map<String, String> fieldErrors) {
        super("Multiple validation errors occurred");
        this.field = null;
        this.fieldErrors = fieldErrors;
        this.generalErrors = null;
    }

    /**
     * Creates a validation exception with multiple general errors
     *
     * @param generalErrors List of general validation error messages
     */
    public ValidationException(List<String> generalErrors) {
        super("Multiple validation errors occurred");
        this.field = null;
        this.fieldErrors = null;
        this.generalErrors = generalErrors;
    }

    /**
     * Creates a validation exception with both field-specific and general errors
     *
     * @param fieldErrors Map of field names to their respective error messages
     * @param generalErrors List of general validation error messages
     */
    public ValidationException(Map<String, String> fieldErrors, List<String> generalErrors) {
        super("Multiple validation errors occurred");
        this.field = null;
        this.fieldErrors = fieldErrors;
        this.generalErrors = generalErrors;
    }

    /**
     * Gets the specific field name that failed validation (for single field errors)
     *
     * @return Field name or null if not a single field error
     */
    public String getField() {
        return field;
    }

    /**
     * Gets all field-specific validation errors
     *
     * @return Map of field names to error messages, or null if no field errors
     */
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    /**
     * Gets all general validation errors
     *
     * @return List of general error messages, or null if no general errors
     */
    public List<String> getGeneralErrors() {
        return generalErrors;
    }

    /**
     * Checks if this exception contains field-specific errors
     *
     * @return true if there are field-specific errors
     */
    public boolean hasFieldErrors() {
        return (field != null) || (fieldErrors != null && !fieldErrors.isEmpty());
    }

    /**
     * Checks if this exception contains general validation errors
     *
     * @return true if there are general validation errors
     */
    public boolean hasGeneralErrors() {
        return generalErrors != null && !generalErrors.isEmpty();
    }

    /**
     * Checks if this exception is for a single field validation error
     *
     * @return true if this is a single field validation error
     */
    public boolean isSingleFieldError() {
        return field != null;
    }

    /**
     * Checks if this exception contains multiple validation errors
     *
     * @return true if there are multiple validation errors
     */
    public boolean hasMultipleErrors() {
        return (fieldErrors != null && fieldErrors.size() > 1) ||
               (generalErrors != null && generalErrors.size() > 1) ||
               (hasFieldErrors() && hasGeneralErrors());
    }

    /**
     * Returns a string representation of this exception including error details
     *
     * @return String representation with error details
     */
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