package com.cena.traveloka.iam.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "oauth_providers", schema = "iam",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_provider_user", columnNames = {"provider", "provider_user_id"}),
                @UniqueConstraint(name = "uk_user_provider", columnNames = {"user_id", "provider"})
        },
        indexes = {
                @Index(name = "idx_oauth_providers_user", columnList = "user_id"),
                @Index(name = "idx_oauth_providers_provider", columnList = "provider")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OAuthProvider {
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false, length = 50)
    String provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    String providerUserId;

    String email;
    String name;
    String avatarUrl;

    @Column(columnDefinition = "text")
    String accessToken;

    @Column(columnDefinition = "text")
    String refreshToken;

    OffsetDateTime tokenExpiresAt;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    JsonNode rawData;

    OffsetDateTime linkedAt;
    OffsetDateTime lastUsedAt;
}