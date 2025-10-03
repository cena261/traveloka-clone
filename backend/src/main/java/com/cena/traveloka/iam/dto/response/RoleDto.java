package com.cena.traveloka.iam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * T040: RoleDto
 * Role information DTO for RBAC (FR-005).
 *
 * Constitutional Compliance:
 * - Principle IV: Entity Immutability - DTO separates from entity
 * - Used in UserDetailDto and role management endpoints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {

    /**
     * Role ID.
     */
    private UUID id;

    /**
     * Role name (ADMIN, CUSTOMER, PARTNER_ADMIN, etc.).
     */
    private String name;

    /**
     * Display name.
     */
    private String displayName;

    /**
     * Role description.
     */
    private String description;

    /**
     * System role indicator.
     */
    private Boolean isSystem;

    /**
     * Role priority (higher = more privileges).
     */
    private Integer priority;
}
