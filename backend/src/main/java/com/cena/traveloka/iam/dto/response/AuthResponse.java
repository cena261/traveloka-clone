package com.cena.traveloka.iam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T035: AuthResponse DTO
 * Response DTO for authentication operations (login, register, refresh).
 *
 * Contains JWT tokens and user basic information.
 *
 * Constitutional Compliance:
 * - NFR-002: JWT 1-hour expiry, refresh token 7-day expiry
 * - Returned by AuthController login, register, refresh endpoints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * JWT access token (1-hour expiry).
     */
    private String accessToken;

    /**
     * Refresh token (7-day expiry).
     */
    private String refreshToken;

    /**
     * Token type (always "Bearer").
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Access token expiry in seconds (3600 = 1 hour).
     */
    @Builder.Default
    private Long expiresIn = 3600L;

    /**
     * Basic user information.
     */
    private UserDto user;
}
