package com.cena.traveloka.iam.exception;

/**
 * Exception thrown when user exceeds maximum concurrent session limit
 *
 * Used when:
 * - User attempts to create new session but already has maximum allowed
 * - Session limit enforcement is triggered
 * - Security policy prevents additional sessions
 */
public class SessionLimitExceededException extends IAMException {

    private final int currentSessions;
    private final int maxAllowed;

    public SessionLimitExceededException(String message, int currentSessions, int maxAllowed) {
        super(message);
        this.currentSessions = currentSessions;
        this.maxAllowed = maxAllowed;
    }

    public SessionLimitExceededException(String message, Throwable cause, int currentSessions, int maxAllowed) {
        super(message, cause);
        this.currentSessions = currentSessions;
        this.maxAllowed = maxAllowed;
    }

    public static SessionLimitExceededException forUser(String userId, int currentSessions, int maxAllowed) {
        String message = String.format("User %s has %d active sessions, maximum allowed: %d",
                userId, currentSessions, maxAllowed);
        return new SessionLimitExceededException(message, currentSessions, maxAllowed);
    }

    public int getCurrentSessions() {
        return currentSessions;
    }

    public int getMaxAllowed() {
        return maxAllowed;
    }
}