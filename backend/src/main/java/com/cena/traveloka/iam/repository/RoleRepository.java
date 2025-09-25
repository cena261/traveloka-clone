package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Role entity operations
 * Simplified to match actual PostgreSQL schema
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // === Basic CRUD Operations ===

    /**
     * Find role by name
     */
    Optional<Role> findByName(String name);

    /**
     * Find all roles ordered by name
     */
    @Query("SELECT r FROM Role r ORDER BY r.name ASC")
    List<Role> findAllOrderByName();

    /**
     * Find default roles
     */
    @Query("SELECT r FROM Role r WHERE r.isDefault = true ORDER BY r.name ASC")
    List<Role> findDefaultRoles();

    /**
     * Find non-default (custom) roles
     */
    @Query("SELECT r FROM Role r WHERE r.isDefault = false ORDER BY r.name ASC")
    List<Role> findCustomRoles();

    // === Permission-based Queries ===

    /**
     * Find roles that have a specific permission
     */
    @Query("SELECT r FROM Role r WHERE :permission MEMBER OF r.permissions")
    List<Role> findRolesWithPermission(@Param("permission") String permission);

    /**
     * Check if role has specific permission
     */
    @Query("SELECT COUNT(r) > 0 FROM Role r WHERE r.id = :roleId AND :permission MEMBER OF r.permissions")
    boolean hasPermission(@Param("roleId") Long roleId, @Param("permission") String permission);

    // === Search and Filtering ===

    /**
     * Search roles by name pattern
     */
    @Query("SELECT r FROM Role r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :pattern, '%')) ORDER BY r.name ASC")
    Page<Role> findByNameContainingIgnoreCase(@Param("pattern") String pattern, Pageable pageable);

    /**
     * Search roles by description pattern
     */
    @Query("SELECT r FROM Role r WHERE LOWER(r.description) LIKE LOWER(CONCAT('%', :pattern, '%')) ORDER BY r.name ASC")
    Page<Role> findByDescriptionContainingIgnoreCase(@Param("pattern") String pattern, Pageable pageable);

    /**
     * Search roles by name
     */
    @Query("SELECT r FROM Role r WHERE (:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<Role> searchRolesByName(@Param("name") String name, Pageable pageable);
}