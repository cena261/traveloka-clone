package com.cena.traveloka.iam.entity;

import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionId implements Serializable {
    @JdbcTypeCode(SqlTypes.UUID)
    UUID roleId;

    @JdbcTypeCode(SqlTypes.UUID)
    UUID permissionId;
}