package com.cena.traveloka.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T034: VerifyEmailRequest DTO
 * Request DTO for email verification (FR-010).
 *
 * Constitutional Compliance:
 * - Principle X: Code Quality - Bean Validation for input validation
 * - Used by AuthController POST /api/v1/auth/verify-email
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyEmailRequest {

    /**
     * Email verification token.
     */
    @NotBlank(message = "Verification token is required")
    private String token;
}
