package com.cena.traveloka.iam.exception;

/**
 * Base exception class for all IAM module exceptions
 *
 * Provides common functionality for IAM-specific exceptions:
 * - Consistent exception hierarchy
 * - Standard error handling patterns
 * - Contextual error information
 * - Integration with exception handler
 */
public abstract class IAMException extends RuntimeException {

    public IAMException(String message) {
        super(message);
    }

    public IAMException(String message, Throwable cause) {
        super(message, cause);
    }

    public IAMException(Throwable cause) {
        super(cause);
    }
}