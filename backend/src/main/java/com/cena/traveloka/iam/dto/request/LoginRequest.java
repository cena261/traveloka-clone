package com.cena.traveloka.iam.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T028: LoginRequest DTO
 * Request DTO for user login (FR-001).
 *
 * Constitutional Compliance:
 * - Principle X: Code Quality - Bean Validation for input validation
 * - Used by AuthController POST /api/v1/auth/login
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * User's email address.
     * Must be valid email format.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /**
     * User's password.
     * Must be at least 8 characters (FR-001).
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /**
     * Optional device information for session tracking.
     */
    private String deviceType;

    /**
     * Optional device ID for multi-device tracking.
     */
    private String deviceId;

    /**
     * Optional 2FA code if 2FA is enabled.
     */
    @Size(min = 6, max = 6, message = "2FA code must be 6 digits")
    private String twoFactorCode;
}
