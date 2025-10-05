package com.cena.traveloka.iam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDto {

    private UUID id;

    private UUID userId;

    private String deviceType;

    private String deviceId;

    private String browser;

    private String os;

    private String ipAddress;

    private String locationCountry;

    private String locationCity;

    private Boolean isActive;

    private Boolean isCurrent;

    private OffsetDateTime lastActivity;

    private OffsetDateTime expiresAt;

    private OffsetDateTime createdAt;

    private Boolean isSuspicious;

    private Integer riskScore;

    private Boolean requires2fa;

    private Boolean twoFaCompleted;
}
