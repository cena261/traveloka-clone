package com.cena.traveloka.iam.dto.response;

import com.cena.traveloka.iam.entity.UserSession;
import com.cena.traveloka.iam.enums.SessionStatus;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for session information
 */
@Data
public class SessionRes {

    private String id;

    private String userId;

    private String sessionId;

    private String ipAddress;

    private String userAgent;

    private String deviceInfo;

    private SessionStatus status;

    private Map<String, Object> sessionData;

    private Instant createdAt;

    private Instant lastAccessedAt;

    private Instant expiresAt;

    private Instant terminatedAt;

    private String terminationReason;

    private Boolean active;

    private Long durationMinutes;
}