package com.cena.traveloka.common.dto;

/**
 * Enumeration defining the possible status values for API responses.
 * Used to indicate the overall result of an API operation.
 *
 * Values:
 * - SUCCESS: Operation completed successfully
 * - ERROR: Operation failed due to an error
 * - WARNING: Operation completed but with warnings
 */
public enum ResponseStatus {
    /**
     * Indicates that the operation completed successfully without any issues
     */
    SUCCESS,

    /**
     * Indicates that the operation failed due to an error condition
     */
    ERROR,

    /**
     * Indicates that the operation completed but there were warnings or
     * non-critical issues that the client should be aware of
     */
    WARNING
}