package com.cena.traveloka.common.exception;

import java.util.UUID;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntityNotFoundException(String entityType, UUID id) {
        super(String.format("%s not found with ID: %s", entityType, id));
    }

    public EntityNotFoundException(String entityType, String identifier) {
        super(String.format("%s not found with identifier: %s", entityType, identifier));
    }

    public EntityNotFoundException(String entityType, Long id) {
        super(String.format("%s not found with ID: %d", entityType, id));
    }
}