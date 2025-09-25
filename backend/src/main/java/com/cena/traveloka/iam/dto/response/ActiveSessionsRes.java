package com.cena.traveloka.iam.dto.response;

import lombok.Data;

import java.util.List;

/**
 * Response DTO for active sessions list
 */
@Data
public class ActiveSessionsRes {

    private String userId;

    private Integer totalActiveSessions;

    private Integer totalAllowedSessions;

    private List<SessionSummaryRes> sessions;

    @Data
    public static class SessionSummaryRes {
        private String sessionId;
        private String deviceInfo;
        private String ipAddress;
        private String lastAccessedAt;
        private Boolean current;
    }
}