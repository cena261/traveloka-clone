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
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "api_keys", schema = "iam",
        indexes = {
                @Index(name = "idx_api_keys_user", columnList = "user_id"),
                @Index(name = "idx_api_keys_hash", columnList = "key_hash"),
                @Index(name = "idx_api_keys_prefix", columnList = "key_prefix")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApiKey {
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false, length = 100)
    String name;

    @Column(name = "key_hash", nullable = false, unique = true, length = 255)
    String keyHash;

    @Column(name = "key_prefix", nullable = false, length = 20)
    String keyPrefix;

    @Column(columnDefinition = "text")
    String description;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    JsonNode permissions;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    List<String> allowedIps;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    List<String> allowedOrigins;

    Integer rateLimit = 1000;

    OffsetDateTime lastUsedAt;
    String lastUsedIp;
    Long usageCount = 0L;

    Boolean isActive = true;
    OffsetDateTime expiresAt;
    OffsetDateTime revokedAt;
    String revokedBy;
    String revokeReason;

    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}