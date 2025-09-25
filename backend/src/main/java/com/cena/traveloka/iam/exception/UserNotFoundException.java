package com.cena.traveloka.iam.exception;

/**
 * Exception thrown when a user cannot be found
 *
 * Used when:
 * - User lookup by ID fails
 * - User lookup by email fails
 * - User lookup by Keycloak ID fails
 * - Referenced user does not exist in operations
 */
public class UserNotFoundException extends IAMException {

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public static UserNotFoundException byId(String userId) {
        return new UserNotFoundException("User not found with ID: " + userId);
    }

    public static UserNotFoundException byEmail(String email) {
        return new UserNotFoundException("User not found with email: " + email);
    }

    public static UserNotFoundException byKeycloakId(String keycloakId) {
        return new UserNotFoundException("User not found with Keycloak ID: " + keycloakId);
    }
}