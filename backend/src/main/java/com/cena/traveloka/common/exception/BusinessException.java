package com.cena.traveloka.common.exception;

/**
 * Exception for business rule violations and domain-specific errors.
 * This exception is thrown when business logic constraints are violated,
 * such as insufficient account balance, invalid state transitions, etc.
 *
 * Features:
 * - Custom error codes for specific business rules
 * - Detailed error messages for user feedback
 * - Inherits from RuntimeException for easier handling
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;

    /**
     * Creates a business exception with error code and message
     *
     * @param errorCode Specific error code for the business rule violation
     * @param message Detailed error message
     */
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Creates a business exception with error code, message, and cause
     *
     * @param errorCode Specific error code for the business rule violation
     * @param message Detailed error message
     * @param cause The underlying cause of the exception
     */
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Gets the specific error code for this business exception
     *
     * @return The error code identifying the specific business rule violation
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns a string representation of this exception including the error code
     *
     * @return String representation with error code and message
     */
    @Override
    public String toString() {
        return String.format("BusinessException{errorCode='%s', message='%s'}",
            errorCode, getMessage());
    }
}