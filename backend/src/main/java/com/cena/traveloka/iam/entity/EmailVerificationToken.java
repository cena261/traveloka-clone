package com.cena.traveloka.iam.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_verification_tokens", schema = "iam",
        indexes = {
                @Index(name = "idx_email_verification_user", columnList = "user_id"),
                @Index(name = "idx_email_verification_token", columnList = "token")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailVerificationToken {
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false)
    String email;

    @Column(nullable = false, unique = true, length = 500)
    String token;

    @Column(nullable = false)
    OffsetDateTime expiresAt;

    Boolean verified = false;
    OffsetDateTime verifiedAt;
    Integer attempts = 0;
    OffsetDateTime createdAt;
}