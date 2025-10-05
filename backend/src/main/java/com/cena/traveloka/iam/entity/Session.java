package com.cena.traveloka.iam.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sessions", schema = "iam",
        indexes = {
                @Index(name = "idx_sessions_user_id", columnList = "user_id"),
                @Index(name = "idx_sessions_token", columnList = "session_token"),
                @Index(name = "idx_sessions_expires", columnList = "expires_at"),
                @Index(name = "idx_sessions_last_activity", columnList = "last_activity")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Session {
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "session_token", nullable = false, unique = true, length = 500)
    String sessionToken;

    @Column(name = "refresh_token", length = 500)
    String refreshToken;

    String ipAddress;
    @Column(columnDefinition = "text")
    String userAgent;
    String deviceType;
    String deviceId;
    String browser;
    String os;
    String locationCountry;
    String locationCity;

    Boolean isActive = true;
    OffsetDateTime lastActivity;
    OffsetDateTime expiresAt;
    OffsetDateTime refreshExpiresAt;

    Boolean isSuspicious = false;
    Integer riskScore = 0;
    Boolean requires2fa = false;
    Boolean twoFaCompleted = false;

    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
    OffsetDateTime terminatedAt;
    String terminationReason;
}
