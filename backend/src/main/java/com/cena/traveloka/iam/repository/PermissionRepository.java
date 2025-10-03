package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * T021: PermissionRepository interface
 * Repository for Permission entity for RBAC permission management (FR-005, FR-006).
 *
 * Constitutional Compliance:
 * - Principle III: Layered Architecture - Repository layer for data access
 * - Principle IV: Entity Immutability - Permission entity is READ-ONLY
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    /**
     * Find permission by name.
     * @param name Permission name
     * @return Optional containing permission if found
     */
    Optional<Permission> findByName(String name);

    /**
     * Find permissions by resource.
     * @param resource Resource identifier
     * @return List of permissions for the specified resource
     */
    List<Permission> findByResource(String resource);

    /**
     * Find permissions by action.
     * @param action Action identifier (e.g., READ, WRITE, DELETE)
     * @return List of permissions for the specified action
     */
    List<Permission> findByAction(String action);

    /**
     * Find permission by resource and action combination.
     * @param resource Resource identifier
     * @param action Action identifier
     * @return Optional containing permission if found
     */
    Optional<Permission> findByResourceAndAction(String resource, String action);

    /**
     * Check if permission name already exists.
     * @param name Permission name to check
     * @return true if permission exists, false otherwise
     */
    boolean existsByName(String name);
}
