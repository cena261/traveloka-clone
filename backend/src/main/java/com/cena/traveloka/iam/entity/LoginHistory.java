package com.cena.traveloka.iam.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Entity
@Table(name = "login_history", schema = "iam",
        indexes = {
                @Index(name = "idx_login_history_user_id", columnList = "user_id"),
                @Index(name = "idx_login_history_username", columnList = "username"),
                @Index(name = "idx_login_history_email", columnList = "email"),
                @Index(name = "idx_login_history_attempted_at", columnList = "attempted_at"),
                @Index(name = "idx_login_history_success", columnList = "success")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    String username;
    String email;

    String loginType;   // password, oauth, sso, biometric
    String provider;    // local, google, facebook, apple
    Boolean success;
    String failureReason;

    String ipAddress;
    @Column(columnDefinition = "text")
    String userAgent;
    String deviceType;
    String deviceId;
    String browser;
    String os;
    String locationCountry;
    String locationCity;

    Integer riskScore = 0;
    Boolean isSuspicious = false;
    Boolean required2fa = false;
    Boolean completed2fa = false;

    OffsetDateTime attemptedAt;
}