package com.cena.traveloka.iam.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when user data validation fails
 *
 * Used when:
 * - User profile data is invalid
 * - Email format is incorrect
 * - Phone number format is invalid
 * - Required fields are missing
 * - Data constraints are violated
 */
public class InvalidUserDataException extends IAMException {

    private final Map<String, String> validationErrors;

    public InvalidUserDataException(String message) {
        super(message);
        this.validationErrors = new HashMap<>();
    }

    public InvalidUserDataException(String message, Map<String, String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors != null ? validationErrors : new HashMap<>();
    }

    public InvalidUserDataException(String message, Throwable cause) {
        super(message, cause);
        this.validationErrors = new HashMap<>();
    }

    public static InvalidUserDataException invalidEmail(String email) {
        Map<String, String> errors = new HashMap<>();
        errors.put("email", "Invalid email format: " + email);
        return new InvalidUserDataException("Invalid email format", errors);
    }

    public static InvalidUserDataException invalidPhoneNumber(String phoneNumber) {
        Map<String, String> errors = new HashMap<>();
        errors.put("phoneNumber", "Invalid phone number format: " + phoneNumber);
        return new InvalidUserDataException("Invalid phone number format", errors);
    }

    public static InvalidUserDataException missingRequiredField(String fieldName) {
        Map<String, String> errors = new HashMap<>();
        errors.put(fieldName, "This field is required");
        return new InvalidUserDataException("Required field missing: " + fieldName, errors);
    }

    public static InvalidUserDataException profileCompletenessOutOfRange(int completeness) {
        Map<String, String> errors = new HashMap<>();
        errors.put("profileCompleteness", "Profile completeness must be between 0 and 100, got: " + completeness);
        return new InvalidUserDataException("Profile completeness out of valid range", errors);
    }

    public static InvalidUserDataException duplicateEmail(String email) {
        Map<String, String> errors = new HashMap<>();
        errors.put("email", "Email address is already in use: " + email);
        return new InvalidUserDataException("Duplicate email address", errors);
    }

    public static InvalidUserDataException duplicateKeycloakId(String keycloakId) {
        Map<String, String> errors = new HashMap<>();
        errors.put("keycloakId", "Keycloak ID is already linked to another user");
        return new InvalidUserDataException("Duplicate Keycloak ID", errors);
    }

    public Map<String, String> getValidationErrors() {
        return new HashMap<>(validationErrors);
    }

    public void addValidationError(String field, String message) {
        this.validationErrors.put(field, message);
    }
}