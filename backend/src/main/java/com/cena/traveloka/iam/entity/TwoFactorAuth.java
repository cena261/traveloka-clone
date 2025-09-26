package com.cena.traveloka.iam.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "two_factor_auth", schema = "iam",
        uniqueConstraints = @UniqueConstraint(name = "uk_2fa_user_method", columnNames = {"user_id", "method"}),
        indexes = {
                @Index(name = "idx_two_factor_auth_user", columnList = "user_id"),
                @Index(name = "idx_two_factor_auth_method", columnList = "method")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TwoFactorAuth {
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false, length = 50)
    String method; // totp, sms, email, backup_codes

    @Column(columnDefinition = "text")
    String secret;

    @ElementCollection
    @CollectionTable(name = "two_factor_backup_codes", schema = "iam", joinColumns = @JoinColumn(name = "two_factor_id"))
    @Column(name = "code")
    List<String> backupCodes;

    String phoneNumber;
    String email;

    Boolean isPrimary = false;
    Boolean isActive = true;
    Boolean verified = false;
    OffsetDateTime verifiedAt;
    OffsetDateTime lastUsedAt;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}