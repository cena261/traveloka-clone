package com.cena.traveloka.iam.dto.request;

import lombok.Data;

/**
 * Request DTO for user logout
 */
@Data
public class LogoutRequest {

    private Boolean allDevices = false;

    private String reason;
}