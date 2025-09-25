package com.cena.traveloka.iam.exception;

/**
 * Exception thrown when Keycloak integration operations fail
 *
 * Used when:
 * - Keycloak admin client operations fail
 * - Authentication service is unavailable
 * - Token validation fails
 * - User management operations fail in Keycloak
 */
public class KeycloakIntegrationException extends IAMException {

    private final String operation;
    private final int statusCode;

    public KeycloakIntegrationException(String message, String operation) {
        super(message);
        this.operation = operation;
        this.statusCode = -1;
    }

    public KeycloakIntegrationException(String message, Throwable cause, String operation) {
        super(message, cause);
        this.operation = operation;
        this.statusCode = -1;
    }

    public KeycloakIntegrationException(String message, String operation, int statusCode) {
        super(message);
        this.operation = operation;
        this.statusCode = statusCode;
    }

    public KeycloakIntegrationException(String message, Throwable cause, String operation, int statusCode) {
        super(message, cause);
        this.operation = operation;
        this.statusCode = statusCode;
    }

    public static KeycloakIntegrationException serviceUnavailable(String operation) {
        return new KeycloakIntegrationException(
                "Keycloak service is unavailable for operation: " + operation,
                operation,
                503
        );
    }

    public static KeycloakIntegrationException authenticationFailed(String operation, Throwable cause) {
        return new KeycloakIntegrationException(
                "Keycloak authentication failed for operation: " + operation,
                cause,
                operation,
                401
        );
    }

    public static KeycloakIntegrationException userManagementFailed(String operation, String userId, Throwable cause) {
        return new KeycloakIntegrationException(
                String.format("Keycloak user management failed: %s for user %s", operation, userId),
                cause,
                operation
        );
    }

    public static KeycloakIntegrationException tokenValidationFailed(Throwable cause) {
        return new KeycloakIntegrationException(
                "Failed to validate token with Keycloak",
                cause,
                "TOKEN_VALIDATION"
        );
    }

    public static KeycloakIntegrationException configurationError(String message) {
        return new KeycloakIntegrationException(
                "Keycloak configuration error: " + message,
                "CONFIGURATION"
        );
    }

    public String getOperation() {
        return operation;
    }

    public int getStatusCode() {
        return statusCode;
    }
}