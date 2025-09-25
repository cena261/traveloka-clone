package com.cena.traveloka.iam.enums;

/**
 * Event type enum for sync events
 * Based on V9 migration constraints
 */
public enum EventType {
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    ROLE_ASSIGNED,
    ROLE_REMOVED,
    PROFILE_UPDATED
}