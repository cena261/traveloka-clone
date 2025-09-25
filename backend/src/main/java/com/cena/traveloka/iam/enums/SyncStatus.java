package com.cena.traveloka.iam.enums;

/**
 * Sync status enum for sync events
 * Based on V9 migration constraints
 */
public enum SyncStatus {
    SUCCESS,
    FAILED,
    PARTIAL,
    PENDING,
    PROCESSING
}