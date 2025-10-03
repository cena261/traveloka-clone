package com.cena.traveloka.iam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * T039: SessionDto
 * Session information DTO for session management (FR-013, FR-016).
 *
 * Constitutional Compliance:
 * - Principle IV: Entity Immutability - DTO separates from entity
 * - Used by SessionController endpoints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDto {

    /**
     * Session ID.
     */
    private UUID id;

    /**
     * User ID.
     */
    private UUID userId;

    /**
     * Device type (desktop, mobile, tablet).
     */
    private String deviceType;

    /**
     * Device ID.
     */
    private String deviceId;

    /**
     * Browser information.
     */
    private String browser;

    /**
     * Operating system.
     */
    private String os;

    /**
     * IP address.
     */
    private String ipAddress;

    /**
     * Location country.
     */
    private String locationCountry;

    /**
     * Location city.
     */
    private String locationCity;

    /**
     * Active session status.
     */
    private Boolean isActive;

    /**
     * Current session indicator.
     */
    private Boolean isCurrent;

    /**
     * Last activity timestamp.
     */
    private OffsetDateTime lastActivity;

    /**
     * Session expiration timestamp (24 hours from creation).
     */
    private OffsetDateTime expiresAt;

    /**
     * Session creation timestamp.
     */
    private OffsetDateTime createdAt;

    /**
     * Suspicious session flag.
     */
    private Boolean isSuspicious;

    /**
     * Risk score (0-100).
     */
    private Integer riskScore;

    /**
     * Requires 2FA flag.
     */
    private Boolean requires2fa;

    /**
     * 2FA completed flag.
     */
    private Boolean twoFaCompleted;
}
