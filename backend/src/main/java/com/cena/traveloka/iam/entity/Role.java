package com.cena.traveloka.iam.entity;

import com.cena.traveloka.common.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles", schema = "iam",
        indexes = {
                @Index(name = "idx_roles_name", columnList = "name"),
                @Index(name = "idx_roles_status", columnList = "status")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Role {
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    UUID id;

    @Column(name = "keycloak_role_id", unique = true)
    @JdbcTypeCode(SqlTypes.UUID)
    UUID keycloakRoleId;

    @Column(nullable = false, unique = true, length = 100)
    String name;

    @Column(length = 200)
    String displayName;

    @Column(columnDefinition = "text")
    String description;

    @Column(name = "role_type", length = 50)
    String roleType = "custom";

    Boolean isSystem = false;
    Integer priority = 0;

    @Enumerated(EnumType.STRING)
    Status status = Status.active;

    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;

    @ManyToMany
    @JoinTable(
            name = "role_permissions", schema = "iam",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    Set<Permission> permissions = new HashSet<>();
}