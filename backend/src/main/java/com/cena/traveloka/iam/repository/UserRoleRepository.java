package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.UserRole;
import com.cena.traveloka.iam.entity.UserRoleId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserRole entity operations
 *
 * Provides comprehensive user-role association management including:
 * - Role assignment and revocation
 * - Temporal role management
 * - User permission resolution
 * - Role assignment analytics
 * - Assignment audit and tracking
 *
 * Key Features:
 * - User-role association queries
 * - Temporal role validation
 * - Permission aggregation
 * - Assignment status management
 * - Performance-optimized queries with proper indexing
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    // === Basic User-Role Associations ===

    /**
     * Find all role assignments for a user
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.userId = :userId ORDER BY ur.createdAt DESC")
    List<UserRole> findByUserId(@Param("userId") String userId);

    /**
     * Find active role assignments for a user
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.userId = :userId AND ur.status = 'ACTIVE' " +
           "AND (ur.validFrom IS NULL OR ur.validFrom <= CURRENT_TIMESTAMP) " +
           "AND (ur.validUntil IS NULL OR ur.validUntil > CURRENT_TIMESTAMP)")
    List<UserRole> findActiveByUserId(@Param("userId") String userId);

    /**
     * Find all user assignments for a role
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.roleId = :roleId ORDER BY ur.createdAt DESC")
    List<UserRole> findByRoleId(@Param("roleId") String roleId);

    /**
     * Find active user assignments for a role
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.roleId = :roleId AND ur.status = 'ACTIVE' " +
           "AND (ur.validFrom IS NULL OR ur.validFrom <= CURRENT_TIMESTAMP) " +
           "AND (ur.validUntil IS NULL OR ur.validUntil > CURRENT_TIMESTAMP)")
    List<UserRole> findActiveByRoleId(@Param("roleId") String roleId);

    /**
     * Find specific user-role assignment
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.userId = :userId AND ur.roleId = :roleId")
    Optional<UserRole> findByUserIdAndRoleId(@Param("userId") String userId, @Param("roleId") String roleId);

    /**
     * Check if user has active role assignment
     */
    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur WHERE ur.userId = :userId AND ur.roleId = :roleId " +
           "AND ur.status = 'ACTIVE' " +
           "AND (ur.validFrom IS NULL OR ur.validFrom <= CURRENT_TIMESTAMP) " +
           "AND (ur.validUntil IS NULL OR ur.validUntil > CURRENT_TIMESTAMP)")
    boolean hasActiveRole(@Param("userId") String userId, @Param("roleId") String roleId);

    // === Role Assignment Status Management ===

    /**
     * Find assignments by status
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.status = :status ORDER BY ur.createdAt DESC")
    Page<UserRole> findByStatus(@Param("status") String status, Pageable pageable);

    /**
     * Count assignments by status for monitoring
     */
    @Query("SELECT ur.status, COUNT(ur) FROM UserRole ur GROUP BY ur.status")
    List<Object[]> countByStatus();

    /**
     * Find expired role assignments that need status update
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.status = 'ACTIVE' AND " +
           "ur.validUntil IS NOT NULL AND ur.validUntil <= CURRENT_TIMESTAMP")
    List<UserRole> findExpiredActiveAssignments();

    /**
     * Find assignments that will expire soon
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.status = 'ACTIVE' AND " +
           "ur.validUntil IS NOT NULL AND ur.validUntil BETWEEN CURRENT_TIMESTAMP AND :expiryThreshold")
    List<UserRole> findAssignmentsExpiringBefore(@Param("expiryThreshold") Instant expiryThreshold);

    /**
     * Find assignments not yet valid (future assignments)
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.status = 'ACTIVE' AND " +
           "ur.validFrom IS NOT NULL AND ur.validFrom > CURRENT_TIMESTAMP")
    List<UserRole> findFutureAssignments();

    // === Temporal Role Management ===

    /**
     * Find roles valid at specific time for user
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.userId = :userId AND ur.status = 'ACTIVE' " +
           "AND (ur.validFrom IS NULL OR ur.validFrom <= :timestamp) " +
           "AND (ur.validUntil IS NULL OR ur.validUntil > :timestamp)")
    List<UserRole> findValidRolesForUserAt(@Param("userId") String userId, @Param("timestamp") Instant timestamp);

    /**
     * Find temporary role assignments (with expiry)
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.validUntil IS NOT NULL AND ur.status = 'ACTIVE'")
    List<UserRole> findTemporaryAssignments();

    /**
     * Find permanent role assignments (no expiry)
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.validUntil IS NULL AND ur.status = 'ACTIVE'")
    List<UserRole> findPermanentAssignments();

    /**
     * Find role assignments within date range
     */
    @Query("SELECT ur FROM UserRole ur WHERE " +
           "(ur.validFrom IS NULL OR ur.validFrom <= :endDate) AND " +
           "(ur.validUntil IS NULL OR ur.validUntil >= :startDate)")
    List<UserRole> findAssignmentsInDateRange(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    // === User Permission Aggregation ===

    /**
     * Get all role names for active user assignments
     */
    @Query("SELECT r.name FROM UserRole ur JOIN Role r ON ur.roleId = r.id " +
           "WHERE ur.userId = :userId AND ur.status = 'ACTIVE' AND r.isActive = true " +
           "AND (ur.validFrom IS NULL OR ur.validFrom <= CURRENT_TIMESTAMP) " +
           "AND (ur.validUntil IS NULL OR ur.validUntil > CURRENT_TIMESTAMP)")
    List<String> findActiveRoleNamesForUser(@Param("userId") String userId);

    /**
     * Get all permissions for user through role assignments
     */
    @Query(value = "SELECT DISTINCT jsonb_array_elements_text(r.permissions) as permission " +
           "FROM iam.user_roles ur " +
           "JOIN iam.roles r ON ur.role_id = r.id " +
           "WHERE ur.user_id = :userId AND ur.status = 'ACTIVE' AND r.is_active = true " +
           "AND (ur.valid_from IS NULL OR ur.valid_from <= CURRENT_TIMESTAMP) " +
           "AND (ur.valid_until IS NULL OR ur.valid_until > CURRENT_TIMESTAMP)",
           nativeQuery = true)
    List<String> findActivePermissionsForUser(@Param("userId") String userId);

    /**
     * Check if user has specific permission through role assignments
     */
    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur " +
           "JOIN Role r ON ur.roleId = r.id " +
           "WHERE ur.userId = :userId AND ur.status = 'ACTIVE' AND r.isActive = true " +
           "AND JSON_SEARCH(r.permissions, 'one', :permission) IS NOT NULL " +
           "AND (ur.validFrom IS NULL OR ur.validFrom <= CURRENT_TIMESTAMP) " +
           "AND (ur.validUntil IS NULL OR ur.validUntil > CURRENT_TIMESTAMP)")
    boolean userHasPermission(@Param("userId") String userId, @Param("permission") String permission);

    /**
     * Find users with specific permission
     */
    @Query("SELECT DISTINCT ur.userId FROM UserRole ur " +
           "JOIN Role r ON ur.roleId = r.id " +
           "WHERE ur.status = 'ACTIVE' AND r.isActive = true " +
           "AND JSON_SEARCH(r.permissions, 'one', :permission) IS NOT NULL " +
           "AND (ur.validFrom IS NULL OR ur.validFrom <= CURRENT_TIMESTAMP) " +
           "AND (ur.validUntil IS NULL OR ur.validUntil > CURRENT_TIMESTAMP)")
    List<String> findUserIdsWithPermission(@Param("permission") String permission);

    // === Assignment Delegation and Approval ===

    /**
     * Find assignments by who assigned them
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.assignedBy = :assignedBy ORDER BY ur.createdAt DESC")
    List<UserRole> findByAssignedBy(@Param("assignedBy") String assignedBy);

    /**
     * Find assignments requiring approval
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.status = 'PENDING_APPROVAL' ORDER BY ur.createdAt ASC")
    List<UserRole> findPendingApprovalAssignments();

    /**
     * Find active assignments created by specific user
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.assignedBy = :assignedBy AND ur.status = 'ACTIVE' ORDER BY ur.createdAt DESC")
    List<UserRole> findActiveAssignmentsByAssignedBy(@Param("assignedBy") String assignedBy);

    /**
     * Find assignments for approval by specific user
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.status = 'PENDING_APPROVAL' " +
           "AND ur.assignedBy = :approverId")
    List<UserRole> findAssignmentsForApproval(@Param("approverId") String approverId);

    // === Bulk Operations ===

    /**
     * Bulk update assignment status
     */
    @Modifying
    @Query("UPDATE UserRole ur SET ur.status = :newStatus, ur.updatedAt = CURRENT_TIMESTAMP, ur.updatedBy = :updatedBy " +
           "WHERE ur.userId IN :userIds AND ur.roleId = :roleId")
    int updateStatusForUserRole(
            @Param("userIds") List<String> userIds,
            @Param("roleId") String roleId,
            @Param("newStatus") String newStatus,
            @Param("updatedBy") String updatedBy);

    /**
     * Bulk revoke role from multiple users
     */
    @Modifying
    @Query("UPDATE UserRole ur SET ur.status = 'REVOKED', ur.validUntil = CURRENT_TIMESTAMP, " +
           "ur.updatedAt = CURRENT_TIMESTAMP, ur.updatedBy = :revokedBy " +
           "WHERE ur.userId IN :userIds AND ur.roleId = :roleId AND ur.status = 'ACTIVE'")
    int revokeRoleFromUsers(
            @Param("userIds") List<String> userIds,
            @Param("roleId") String roleId,
            @Param("revokedBy") String revokedBy);

    /**
     * Bulk revoke all roles from user
     */
    @Modifying
    @Query("UPDATE UserRole ur SET ur.status = 'REVOKED', ur.validUntil = CURRENT_TIMESTAMP, " +
           "ur.updatedAt = CURRENT_TIMESTAMP, ur.updatedBy = :revokedBy " +
           "WHERE ur.userId = :userId AND ur.status = 'ACTIVE'")
    int revokeAllRolesFromUser(@Param("userId") String userId, @Param("revokedBy") String revokedBy);

    /**
     * Bulk expire assignments past their validity
     */
    @Modifying
    @Query("UPDATE UserRole ur SET ur.status = 'EXPIRED', ur.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE ur.status = 'ACTIVE' AND ur.validUntil IS NOT NULL AND ur.validUntil <= CURRENT_TIMESTAMP")
    int expirePassedAssignments();

    /**
     * Extend validity period for assignments
     */
    @Modifying
    @Query("UPDATE UserRole ur SET ur.validUntil = :newValidUntil, ur.updatedAt = CURRENT_TIMESTAMP, ur.updatedBy = :updatedBy " +
           "WHERE ur.userId = :userId AND ur.roleId = :roleId AND ur.status = 'ACTIVE'")
    int extendAssignmentValidity(
            @Param("userId") String userId,
            @Param("roleId") String roleId,
            @Param("newValidUntil") Instant newValidUntil,
            @Param("updatedBy") String updatedBy);

    // === Analytics and Reporting ===

    /**
     * Get user-role assignment statistics
     */
    @Query("SELECT " +
           "COUNT(*) as totalAssignments, " +
           "COUNT(CASE WHEN ur.status = 'ACTIVE' THEN 1 END) as activeAssignments, " +
           "COUNT(CASE WHEN ur.status = 'REVOKED' THEN 1 END) as revokedAssignments, " +
           "COUNT(CASE WHEN ur.status = 'EXPIRED' THEN 1 END) as expiredAssignments, " +
           "COUNT(CASE WHEN ur.validUntil IS NOT NULL THEN 1 END) as temporaryAssignments, " +
           "COUNT(DISTINCT ur.userId) as uniqueUsers, " +
           "COUNT(DISTINCT ur.roleId) as uniqueRoles " +
           "FROM UserRole ur")
    AssignmentStatistics getAssignmentStatistics();

    /**
     * Interface for assignment statistics projection
     */
    interface AssignmentStatistics {
        long getTotalAssignments();
        long getActiveAssignments();
        long getRevokedAssignments();
        long getExpiredAssignments();
        long getTemporaryAssignments();
        long getUniqueUsers();
        long getUniqueRoles();
    }

    /**
     * Get most assigned roles
     */
    @Query("SELECT r.name, COUNT(ur) as assignmentCount FROM UserRole ur " +
           "JOIN Role r ON ur.roleId = r.id " +
           "WHERE ur.status = 'ACTIVE' " +
           "GROUP BY r.name " +
           "ORDER BY assignmentCount DESC")
    List<Object[]> getMostAssignedRoles();

    /**
     * Get users with most role assignments
     */
    @Query("SELECT u.email, COUNT(ur) as roleCount FROM UserRole ur " +
           "JOIN AppUser u ON ur.userId = u.id " +
           "WHERE ur.status = 'ACTIVE' " +
           "GROUP BY u.email " +
           "ORDER BY roleCount DESC")
    List<Object[]> getUsersWithMostRoles();

    /**
     * Find assignments created within date range
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.createdAt BETWEEN :startDate AND :endDate ORDER BY ur.createdAt DESC")
    Page<UserRole> findAssignmentsCreatedBetween(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    // === Search and Filtering ===

    /**
     * Search assignments by multiple criteria
     */
    @Query("SELECT ur FROM UserRole ur WHERE " +
           "(:userId IS NULL OR ur.userId = :userId) AND " +
           "(:roleId IS NULL OR ur.roleId = :roleId) AND " +
           "(:status IS NULL OR ur.status = :status) AND " +
           "(:assignedBy IS NULL OR ur.assignedBy = :assignedBy)")
    Page<UserRole> searchAssignments(
            @Param("userId") String userId,
            @Param("roleId") String roleId,
            @Param("status") String status,
            @Param("assignedBy") String assignedBy,
            Pageable pageable);

    /**
     * Find assignments by assignment reason
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.assignmentReason LIKE CONCAT('%', :reason, '%')")
    List<UserRole> findByAssignmentReason(@Param("reason") String reason);

    // === Data Quality and Validation ===

    /**
     * Find assignments to non-existent users
     */
    @Query("SELECT ur FROM UserRole ur WHERE " +
           "NOT EXISTS (SELECT 1 FROM AppUser u WHERE u.id = ur.userId)")
    List<UserRole> findOrphanedUserAssignments();

    /**
     * Find assignments to non-existent roles
     */
    @Query("SELECT ur FROM UserRole ur WHERE " +
           "NOT EXISTS (SELECT 1 FROM Role r WHERE r.id = ur.roleId)")
    List<UserRole> findOrphanedRoleAssignments();

    /**
     * Find assignments with invalid date ranges
     */
    @Query("SELECT ur FROM UserRole ur WHERE " +
           "ur.validFrom IS NOT NULL AND ur.validUntil IS NOT NULL AND ur.validFrom >= ur.validUntil")
    List<UserRole> findInvalidDateRangeAssignments();

    /**
     * Find duplicate active assignments
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.status = 'ACTIVE' AND " +
           "EXISTS (SELECT 1 FROM UserRole ur2 WHERE ur2.userId = ur.userId AND ur2.roleId = ur.roleId " +
           "AND ur2.status = 'ACTIVE' AND ur2.createdAt != ur.createdAt)")
    List<UserRole> findDuplicateActiveAssignments();

    // === Cleanup and Maintenance ===

    /**
     * Delete old revoked assignments
     */
    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.status = 'REVOKED' AND ur.validUntil < :threshold")
    int deleteOldRevokedAssignments(@Param("threshold") Instant threshold);

    /**
     * Delete old expired assignments
     */
    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.status = 'EXPIRED' AND ur.validUntil < :threshold")
    int deleteOldExpiredAssignments(@Param("threshold") Instant threshold);

    /**
     * Find assignments requiring cleanup
     */
    @Query("SELECT ur FROM UserRole ur WHERE " +
           "(ur.status = 'REVOKED' AND ur.validUntil < :cleanupThreshold) OR " +
           "(ur.status = 'EXPIRED' AND ur.validUntil < :cleanupThreshold)")
    List<UserRole> findAssignmentsForCleanup(@Param("cleanupThreshold") Instant cleanupThreshold);
}