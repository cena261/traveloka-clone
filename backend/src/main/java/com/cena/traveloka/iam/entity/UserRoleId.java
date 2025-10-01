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
@EqualsAndHashCode
public class UserRoleId implements Serializable {
    @JdbcTypeCode(SqlTypes.UUID)
    UUID userId;

    @JdbcTypeCode(SqlTypes.UUID)
    UUID roleId;
}