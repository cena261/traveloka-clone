package com.cena.traveloka.iam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * T041: TwoFactorSetupDto
 * 2FA setup information DTO (FR-014).
 *
 * Contains QR code and backup codes for TOTP setup.
 *
 * Constitutional Compliance:
 * - Principle IV: Entity Immutability - DTO separates from entity
 * - Used by TwoFactorAuthController POST /api/v1/users/me/2fa/setup
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorSetupDto {

    /**
     * Base64-encoded QR code image for TOTP setup.
     */
    private String qrCode;

    /**
     * TOTP secret (for manual entry).
     */
    private String secret;

    /**
     * Backup recovery codes (8 codes).
     * User should save these securely.
     */
    private List<String> backupCodes;

    /**
     * Issuer name (shown in authenticator app).
     */
    @Builder.Default
    private String issuer = "Traveloka";

    /**
     * Account name (shown in authenticator app).
     */
    private String accountName;
}
