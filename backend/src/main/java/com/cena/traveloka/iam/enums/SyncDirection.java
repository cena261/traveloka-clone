package com.cena.traveloka.iam.enums;

/**
 * Sync direction enum for IAM module
 * Based on Keycloak synchronization patterns
 */
public enum SyncDirection {
    KEYCLOAK_TO_LOCAL,
    LOCAL_TO_KEYCLOAK,
    BIDIRECTIONAL
}