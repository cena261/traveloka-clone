package com.cena.traveloka.iam.controller;

import com.cena.traveloka.common.dto.ApiResponse;
import com.cena.traveloka.iam.dto.response.TwoFactorSetupDto;
import com.cena.traveloka.iam.service.TwoFactorAuthService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * T078-T080: TwoFactorAuthController
 * REST API controller for two-factor authentication operations.
 *
 * Endpoints:
 * - POST /api/v1/users/me/2fa/setup (T078)
 * - POST /api/v1/users/me/2fa/verify (T079)
 * - POST /api/v1/users/me/2fa/disable (T080)
 *
 * Constitutional Compliance:
 * - Principle III: Layered Architecture - Controller delegates to service layer
 * - Principle IV: Entity Immutability - Uses DTOs for API contracts
 * - FR-014: TOTP-based 2FA support
 * - FR-015: SMS/Email 2FA fallback
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users/me/2fa")
@RequiredArgsConstructor
@Validated
public class TwoFactorAuthController {

    private final TwoFactorAuthService twoFactorAuthService;

    /**
     * T078: Setup TOTP-based 2FA for current user (FR-014).
     * Returns QR code and backup codes for authenticator app setup.
     *
     * @param authHeader Authorization header with JWT token
     * @return ApiResponse with TwoFactorSetupDto containing QR code and backup codes
     */
    @PostMapping("/setup")
    public ApiResponse<TwoFactorSetupDto> setupTwoFactorAuth(
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        log.info("Setup 2FA request");

        TwoFactorSetupDto setupDto = twoFactorAuthService.setupTwoFactorAuth(token);

        return ApiResponse.success(
                "2FA setup initiated. Please scan the QR code with your authenticator app and save the backup codes securely.",
                setupDto
        );
    }

    /**
     * T079: Verify and activate 2FA (FR-014).
     * User must provide a valid TOTP code to complete 2FA setup.
     *
     * @param code TOTP code from authenticator app
     * @param authHeader Authorization header with JWT token
     * @return ApiResponse with success message
     */
    @PostMapping("/verify")
    public ApiResponse<Void> verifyTwoFactorAuth(
            @RequestParam @NotBlank(message = "2FA code is required")
            @Size(min = 6, max = 6, message = "2FA code must be 6 digits") String code,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        log.info("Verify 2FA request");

        twoFactorAuthService.verifyAndActivateTwoFactorAuth(token, code);

        return ApiResponse.success(
                "2FA enabled successfully. Your account is now protected with two-factor authentication.",
                null
        );
    }

    /**
     * T080: Disable 2FA for current user (FR-014).
     * User must provide current password or backup code for verification.
     *
     * @param password Current password for verification (optional if using backup code)
     * @param backupCode Backup code for verification (optional if using password)
     * @param authHeader Authorization header with JWT token
     * @return ApiResponse with success message
     */
    @PostMapping("/disable")
    public ApiResponse<Void> disableTwoFactorAuth(
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String backupCode,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = extractToken(authHeader);
        log.info("Disable 2FA request");

        // Validate that at least one verification method is provided
        if ((password == null || password.isBlank()) && (backupCode == null || backupCode.isBlank())) {
            throw new IllegalArgumentException("Either password or backup code is required to disable 2FA");
        }

        twoFactorAuthService.disableTwoFactorAuth(token, password, backupCode);

        return ApiResponse.success(
                "2FA disabled successfully",
                null
        );
    }

    /**
     * Extract JWT token from Authorization header.
     *
     * @param authHeader Authorization header (Bearer token)
     * @return JWT token string
     */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header format");
    }
}
