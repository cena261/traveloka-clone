package com.cena.traveloka.iam.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T031: PasswordResetRequest DTO
 * Request DTO for password reset flow (FR-009).
 *
 * Constitutional Compliance:
 * - Principle X: Code Quality - Bean Validation for input validation
 * - Used by AuthController POST /api/v1/auth/forgot-password
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {

    /**
     * Email address to send reset link to.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
}
