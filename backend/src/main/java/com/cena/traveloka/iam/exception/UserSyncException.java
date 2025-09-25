package com.cena.traveloka.iam.exception;

/**
 * Exception thrown when user data synchronization fails
 *
 * Used when:
 * - Keycloak synchronization fails
 * - External system sync fails
 * - Data consistency issues during sync
 * - Network or service unavailability during sync
 */
public class UserSyncException extends IAMException {

    private final String userId;
    private final String syncOperation;

    public UserSyncException(String message, String userId, String syncOperation) {
        super(message);
        this.userId = userId;
        this.syncOperation = syncOperation;
    }

    public UserSyncException(String message, Throwable cause, String userId, String syncOperation) {
        super(message, cause);
        this.userId = userId;
        this.syncOperation = syncOperation;
    }

    public static UserSyncException keycloakSync(String userId, Throwable cause) {
        return new UserSyncException(
                "Failed to synchronize user with Keycloak: " + userId,
                cause,
                userId,
                "KEYCLOAK_SYNC"
        );
    }

    public static UserSyncException profileSync(String userId, String operation, Throwable cause) {
        return new UserSyncException(
                String.format("Failed to sync user profile during %s: %s", operation, userId),
                cause,
                userId,
                operation
        );
    }

    public static UserSyncException batchSync(String operation, int failedCount, Throwable cause) {
        return new UserSyncException(
                String.format("Batch sync operation failed: %s (%d users affected)", operation, failedCount),
                cause,
                null,
                operation
        );
    }

    public String getUserId() {
        return userId;
    }

    public String getSyncOperation() {
        return syncOperation;
    }
}