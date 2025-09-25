package com.cena.traveloka.iam.enums;

/**
 * User status enum for IAM module
 * Based on database constraints and business logic
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    PENDING_VERIFICATION,
    DELETED
}