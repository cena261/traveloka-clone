package com.cena.traveloka.iam.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Entity
@Table(name = "role_permissions", schema = "iam",
        indexes = {
                @Index(name = "idx_role_permissions_role", columnList = "role_id"),
                @Index(name = "idx_role_permissions_permission", columnList = "permission_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RolePermission {

    @EmbeddedId
    RolePermissionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("permissionId")
    @JoinColumn(name = "permission_id")
    Permission permission;

    OffsetDateTime grantedAt;
    String grantedBy;
}