package com.cena.traveloka.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for user login
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Access token is required")
    private String accessToken;

    @Size(max = 45, message = "IP address must not exceed 45 characters")
    private String ipAddress;

    @Size(max = 500, message = "User agent must not exceed 500 characters")
    private String userAgent;

    @Size(max = 200, message = "Device info must not exceed 200 characters")
    private String deviceInfo;

    private Boolean rememberMe = false;
}