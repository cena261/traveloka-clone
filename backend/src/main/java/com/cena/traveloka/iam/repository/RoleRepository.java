package com.cena.traveloka.iam.repository;

import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * T020: RoleRepository interface
 * Repository for Role entity with query methods for RBAC (FR-005).
 *
 * Constitutional Compliance:
 * - Principle III: Layered Architecture - Repository layer for data access
 * - Principle IV: Entity Immutability - Role entity is READ-ONLY
 * - Principle VII: Test-First Development - Implementation follows RoleRepositoryTest
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Find role by name (e.g., ADMIN, CUSTOMER, PARTNER_ADMIN).
     * @param name Role name
     * @return Optional containing role if found
     */
    Optional<Role> findByName(String name);

    /**
     * Find role by Keycloak role ID.
     * @param keycloakRoleId Keycloak role ID
     * @return Optional containing role if found
     */
    Optional<Role> findByKeycloakRoleId(UUID keycloakRoleId);

    /**
     * Check if role name already exists.
     * @param name Role name to check
     * @return true if role exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Find all system roles.
     * @return List of system roles
     */
    List<Role> findByIsSystemTrue();

    /**
     * Find roles by status.
     * @param status Role status (ACTIVE, INACTIVE)
     * @return List of roles with the specified status
     */
    List<Role> findByStatus(Status status);

    /**
     * Find roles by type (e.g., system, partner, custom).
     * @param roleType Role type
     * @return List of roles with the specified type
     */
    List<Role> findByRoleType(String roleType);

    /**
     * Find all roles ordered by priority descending.
     * @return List of roles sorted by priority (highest first)
     */
    List<Role> findAllByOrderByPriorityDesc();

    /**
     * Find active system roles.
     * @param status Role status (typically ACTIVE)
     * @return List of active system roles
     */
    List<Role> findByIsSystemTrueAndStatus(Status status);

    /**
     * Count roles by status.
     * @param status Role status
     * @return Count of roles with the specified status
     */
    long countByStatus(Status status);
}
