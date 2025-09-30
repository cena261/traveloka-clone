package com.cena.traveloka.common.exception;

import java.util.UUID;

/**
 * Exception thrown when a requested entity cannot be found in the database.
 * This is typically used for 404 NOT FOUND HTTP responses.
 *
 * Features:
 * - Specific exception for entity not found scenarios
 * - Supports both UUID and string-based entity identifiers
 * - Provides clear error messages for debugging and user feedback
 * - Inherits from RuntimeException for easier handling
 */
public class EntityNotFoundException extends RuntimeException {

    /**
     * Creates an entity not found exception with a custom message
     *
     * @param message Detailed error message describing what entity was not found
     */
    public EntityNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates an entity not found exception with message and cause
     *
     * @param message Detailed error message
     * @param cause The underlying cause of the exception
     */
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an entity not found exception for a specific entity type and UUID
     *
     * @param entityType The type of entity that was not found (e.g., "User", "Hotel")
     * @param id The UUID identifier that was searched for
     */
    public EntityNotFoundException(String entityType, UUID id) {
        super(String.format("%s not found with ID: %s", entityType, id));
    }

    /**
     * Creates an entity not found exception for a specific entity type and string identifier
     *
     * @param entityType The type of entity that was not found (e.g., "User", "Hotel")
     * @param identifier The string identifier that was searched for
     */
    public EntityNotFoundException(String entityType, String identifier) {
        super(String.format("%s not found with identifier: %s", entityType, identifier));
    }

    /**
     * Creates an entity not found exception for a specific entity type and long identifier
     *
     * @param entityType The type of entity that was not found (e.g., "User", "Hotel")
     * @param id The long identifier that was searched for
     */
    public EntityNotFoundException(String entityType, Long id) {
        super(String.format("%s not found with ID: %d", entityType, id));
    }
}