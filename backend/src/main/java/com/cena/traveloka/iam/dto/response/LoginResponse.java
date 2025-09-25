package com.cena.traveloka.iam.dto.response;

import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for successful login
 */
@Data
public class LoginResponse {

    private String accessToken;

    private String refreshToken;

    private String tokenType = "Bearer";

    private Long expiresIn;

    private UserRes user;

    private SessionRes session;

    private List<String> roles;

    private List<String> permissions;

    private Instant loginTime;

    private Boolean firstLogin;

    private Boolean passwordChangeRequired;

    private Boolean mfaRequired;
}