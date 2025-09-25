package com.cena.traveloka.iam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * Request DTO for creating a new session
 */
@Data
public class SessionCreateReq {

    @NotBlank(message = "User ID is required")
    private String userId;

    @Size(max = 255, message = "Device ID must not exceed 255 characters")
    private String deviceId;

    @NotBlank(message = "Device type is required")
    private String deviceType = "WEB";

    @Size(max = 45, message = "IP address must not exceed 45 characters")
    private String ipAddress;

    @Size(max = 500, message = "User agent must not exceed 500 characters")
    private String userAgent;

    @Size(max = 200, message = "Location must not exceed 200 characters")
    private String location;

    // Additional optional fields
    @Size(max = 200, message = "Device info must not exceed 200 characters")
    private String deviceInfo;

    private Map<String, Object> sessionData;

    private Long timeoutMinutes;
}