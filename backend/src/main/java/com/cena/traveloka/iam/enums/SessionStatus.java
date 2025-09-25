package com.cena.traveloka.iam.enums;

/**
 * Session status enum for IAM module
 * Based on database constraints and business logic
 */
public enum SessionStatus {
    ACTIVE,
    EXPIRED,
    TERMINATED,
    INVALID
}