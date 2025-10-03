package com.cena.traveloka.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T032: RefreshTokenRequest DTO
 * Request DTO for refreshing JWT tokens (FR-017).
 *
 * Constitutional Compliance:
 * - Principle X: Code Quality - Bean Validation for input validation
 * - Used by AuthController POST /api/v1/auth/refresh
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    /**
     * Refresh token (7-day expiry).
     */
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
