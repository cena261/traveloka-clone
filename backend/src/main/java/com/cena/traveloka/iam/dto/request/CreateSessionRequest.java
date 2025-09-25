package com.cena.traveloka.iam.dto.request;

import lombok.Data;

import jakarta.validation.constraints.*;

/**
 * Request DTO for creating a new user session
 *
 * Includes validation for session creation parameters:
 * - Device information with type validation
 * - IP address format validation
 * - User agent sanitization
 * - Geographic location validation
 * - Security and tracking parameters
 */
@Data
public class CreateSessionRequest {

    @NotBlank(message = "User ID is required")
    @Size(min = 1, max = 255, message = "User ID must be between 1 and 255 characters")
    private String userId;

    @NotBlank(message = "Device ID is required")
    @Size(min = 1, max = 255, message = "Device ID must be between 1 and 255 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Device ID can only contain alphanumeric characters, underscore, and hyphen")
    private String deviceId;

    @NotBlank(message = "Device type is required")
    @Pattern(regexp = "^(WEB|MOBILE_IOS|MOBILE_ANDROID|TABLET|DESKTOP)$",
             message = "Device type must be WEB, MOBILE_IOS, MOBILE_ANDROID, TABLET, or DESKTOP")
    private String deviceType;

    @NotBlank(message = "IP address is required")
    @Pattern(regexp = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$",
             message = "IP address must be a valid IPv4 or IPv6 address")
    private String ipAddress;

    @Size(max = 1000, message = "User agent must not exceed 1000 characters")
    private String userAgent;

    @Size(max = 200, message = "Location must not exceed 200 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s,.-]*$", message = "Location can only contain alphanumeric characters, spaces, commas, periods, and hyphens")
    private String location;

    @Min(value = 1, message = "Session timeout must be at least 1 hour")
    @Max(value = 168, message = "Session timeout cannot exceed 168 hours (7 days)")
    private Integer timeoutHours;

    @NotNull(message = "Remember me preference is required")
    private Boolean rememberMe;

    @Size(max = 500, message = "Device info must not exceed 500 characters")
    private String deviceInfo;

    @Size(max = 100, message = "App version must not exceed 100 characters")
    @Pattern(regexp = "^[0-9]+\\.[0-9]+\\.[0-9]+(\\.[0-9]+)?(-[a-zA-Z0-9]+)?$",
             message = "App version must follow semantic versioning (e.g., 1.0.0, 1.0.0-beta)")
    private String appVersion;

    @Size(max = 50, message = "Platform must not exceed 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s.-]+$", message = "Platform can only contain alphanumeric characters, spaces, periods, and hyphens")
    private String platform;

    // Security-related fields
    @AssertTrue(message = "Timeout hours is required when remember me is enabled")
    public boolean isValidRememberMeTimeout() {
        if (Boolean.TRUE.equals(rememberMe)) {
            return timeoutHours != null;
        }
        return true;
    }

    @AssertTrue(message = "Extended timeout requires remember me to be enabled")
    public boolean isValidExtendedTimeout() {
        if (timeoutHours != null && timeoutHours > 24) {
            return Boolean.TRUE.equals(rememberMe);
        }
        return true;
    }
}