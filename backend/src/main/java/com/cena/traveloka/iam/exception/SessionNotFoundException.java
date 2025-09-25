package com.cena.traveloka.iam.exception;

/**
 * Exception thrown when a session cannot be found or has expired
 *
 * Used when:
 * - Session lookup by session ID fails
 * - Session has expired and no longer valid
 * - Session was terminated or invalidated
 * - Referenced session does not exist
 */
public class SessionNotFoundException extends IAMException {

    public SessionNotFoundException(String message) {
        super(message);
    }

    public SessionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public static SessionNotFoundException bySessionId(String sessionId) {
        return new SessionNotFoundException("Session not found or expired: " + sessionId);
    }

    public static SessionNotFoundException byUserId(String userId) {
        return new SessionNotFoundException("No active sessions found for user: " + userId);
    }

    public static SessionNotFoundException expired(String sessionId) {
        return new SessionNotFoundException("Session has expired: " + sessionId);
    }

    public static SessionNotFoundException terminated(String sessionId) {
        return new SessionNotFoundException("Session was terminated: " + sessionId);
    }
}