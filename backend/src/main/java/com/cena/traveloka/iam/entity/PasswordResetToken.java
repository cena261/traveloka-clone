package com.cena.traveloka.iam.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens", schema = "iam",
        indexes = {
                @Index(name = "idx_password_reset_tokens_user", columnList = "user_id"),
                @Index(name = "idx_password_reset_tokens_token", columnList = "token"),
                @Index(name = "idx_password_reset_tokens_expires", columnList = "expires_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PasswordResetToken {
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false, unique = true, length = 500)
    String token;

    @Column(nullable = false)
    OffsetDateTime expiresAt;

    Boolean used = false;
    OffsetDateTime usedAt;
    String ipAddress;
    @Column(columnDefinition = "text")
    String userAgent;
    OffsetDateTime createdAt;
}