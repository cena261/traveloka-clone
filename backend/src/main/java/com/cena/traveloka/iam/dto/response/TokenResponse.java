package com.cena.traveloka.iam.dto.response;

import lombok.Data;

import java.time.Instant;

/**
 * Response DTO for token operations
 */
@Data
public class TokenResponse {

    private String accessToken;

    private String refreshToken;

    private String tokenType = "Bearer";

    private Long expiresIn;

    private Instant issuedAt;

    private Instant expiresAt;

    private String scope;
}