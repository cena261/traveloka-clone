package com.cena.traveloka.iam.entity;

import com.cena.traveloka.common.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "permissions", schema = "iam",
        uniqueConstraints = @UniqueConstraint(name = "uk_perm_resource_action", columnNames = {"resource", "action"}),
        indexes = {
                @Index(name = "idx_permissions_resource", columnList = "resource"),
                @Index(name = "idx_permissions_action", columnList = "action")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Permission {
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    UUID id;

    @Column(nullable = false, length = 100)
    String resource;

    @Column(nullable = false, length = 50)
    String action;

    @Column(nullable = false, length = 200)
    String name;

    @Column(columnDefinition = "text")
    String description;

    @Enumerated(EnumType.STRING)
    Status status = Status.active;

    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}