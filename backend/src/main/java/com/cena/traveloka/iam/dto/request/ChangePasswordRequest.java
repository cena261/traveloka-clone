package com.cena.traveloka.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T033: ChangePasswordRequest DTO
 * Request DTO for changing user password.
 *
 * Constitutional Compliance:
 * - Principle X: Code Quality - Bean Validation for input validation
 * - NFR-001: Password complexity validation
 * - Used by AuthController POST /api/v1/auth/change-password
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    /**
     * Current password for verification.
     */
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    /**
     * New password (must meet complexity requirements).
     */
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$",
        message = "Password must contain at least 1 uppercase, 1 lowercase, 1 number, and 1 special character"
    )
    private String newPassword;

    /**
     * New password confirmation.
     */
    @NotBlank(message = "Password confirmation is required")
    private String newPasswordConfirmation;
}
