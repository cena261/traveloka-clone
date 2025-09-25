package com.cena.traveloka.iam.dto.response;

import lombok.Data;

import java.time.Instant;

/**
 * Response DTO for logout
 */
@Data
public class LogoutResponse {

    private Boolean success;

    private String message;

    private Instant logoutTime;

    private Integer sessionsTerminated;
}