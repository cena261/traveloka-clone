package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.AppUser;
import com.cena.traveloka.iam.enums.UserStatus;
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
 * Repository interface for AppUser entity operations
 *
 * Provides standard CRUD operations plus custom queries optimized for IAM operations:
 * - Keycloak integration lookups
 * - User status management
 * - Profile completeness tracking
 * - Synchronization status queries
 * - Performance-optimized queries with proper indexing
 *
 * Key Features:
 * - Custom queries for Keycloak ID mapping
 * - Bulk operations for user management
 * - Optimized queries for active user lookups
 * - Profile completeness filtering
 * - Sync status tracking for data consistency
 */
@Repository
public interface UserRepository extends JpaRepository<AppUser, String> {

    // === Keycloak Integration Queries ===

    /**
     * Find user by Keycloak ID for OAuth2 integration
     * Uses unique index on keycloak_id for optimal performance
     */
    @Query("SELECT u FROM AppUser u WHERE u.keycloakId = :keycloakId")
    Optional<AppUser> findByKeycloakId(@Param("keycloakId") String keycloakId);

    /**
     * Check if user exists by Keycloak ID
     */
    boolean existsByKeycloakId(String keycloakId);

    /**
     * Find users that need Keycloak synchronization
     * Returns users without keycloakId or not synced recently
     */
    @Query("SELECT u FROM AppUser u WHERE u.keycloakId IS NULL OR u.lastSyncAt IS NULL OR u.lastSyncAt < :threshold")
    List<AppUser> findUsersNeedingSync(@Param("threshold") Instant threshold);

    // === Email and Authentication Queries ===

    /**
     * Find user by email address (unique constraint)
     * Uses unique index on email for optimal performance
     */
    Optional<AppUser> findByEmail(String email);

    /**
     * Check if email is already in use
     */
    boolean existsByEmail(String email);

    /**
     * Find users by email pattern for admin searches
     */
    @Query("SELECT u FROM AppUser u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :pattern, '%'))")
    Page<AppUser> findByEmailContainingIgnoreCase(@Param("pattern") String pattern, Pageable pageable);

    // === Status and State Management ===

    /**
     * Find all active users
     * Uses partial index on status for optimal performance
     */
    @Query("SELECT u FROM AppUser u WHERE u.status = 'ACTIVE'")
    List<AppUser> findActiveUsers();

    /**
     * Find users by status with pagination
     */
    @Query("SELECT u FROM AppUser u WHERE u.status = :status")
    Page<AppUser> findByStatus(@Param("status") UserStatus status, Pageable pageable);

    /**
     * Count users by status for dashboard metrics
     */
    @Query("SELECT COUNT(u) FROM AppUser u WHERE u.status = :status")
    long countByStatus(@Param("status") UserStatus status);

    /**
     * Find users with incomplete profiles
     */
    @Query("SELECT u FROM AppUser u WHERE u.profileCompleteness < :threshold")
    List<AppUser> findUsersWithIncompleteProfiles(@Param("threshold") Integer threshold);

    // === Profile and Completeness Queries ===

    /**
     * Find users by profile completeness range
     */
    @Query("SELECT u FROM AppUser u WHERE u.profileCompleteness BETWEEN :minCompleteness AND :maxCompleteness")
    Page<AppUser> findByProfileCompletenessRange(
            @Param("minCompleteness") Integer minCompleteness,
            @Param("maxCompleteness") Integer maxCompleteness,
            Pageable pageable);

    /**
     * Find users by name pattern for search functionality
     */
    @Query("SELECT u FROM AppUser u WHERE " +
           "LOWER(CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, ''))) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<AppUser> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

    // === Bulk Operations ===

    /**
     * Update profile completeness for multiple users
     */
    @Modifying
    @Query("UPDATE AppUser u SET u.profileCompleteness = :completeness, u.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE u.id IN :userIds")
    int updateProfileCompletenessForUsers(
            @Param("userIds") List<String> userIds,
            @Param("completeness") Integer completeness);

    /**
     * Update last sync timestamp for users
     */
    @Modifying
    @Query("UPDATE AppUser u SET u.lastSyncAt = :syncTime, u.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE u.keycloakId IN :keycloakIds")
    int updateLastSyncForKeycloakUsers(
            @Param("keycloakIds") List<String> keycloakIds,
            @Param("syncTime") Instant syncTime);

    /**
     * Bulk status update for user management
     */
    @Modifying
    @Query("UPDATE AppUser u SET u.status = :status, u.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE u.id IN :userIds")
    int updateStatusForUsers(
            @Param("userIds") List<String> userIds,
            @Param("status") UserStatus status);

    // === Time-based Queries ===

    /**
     * Find recently created users for monitoring
     */
    @Query("SELECT u FROM AppUser u WHERE u.createdAt >= :since ORDER BY u.createdAt DESC")
    List<AppUser> findRecentlyCreatedUsers(@Param("since") Instant since);

    /**
     * Find users created within date range
     */
    @Query("SELECT u FROM AppUser u WHERE u.createdAt BETWEEN :startDate AND :endDate ORDER BY u.createdAt DESC")
    Page<AppUser> findUsersCreatedBetween(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    /**
     * Find users last updated before threshold (for cleanup operations)
     */
    @Query("SELECT u FROM AppUser u WHERE u.updatedAt < :threshold AND u.status != 'DELETED'")
    List<AppUser> findStaleUsers(@Param("threshold") Instant threshold);

    // === Administrative Queries ===

    /**
     * Find users without phone numbers for data completion campaigns
     */
    @Query("SELECT u FROM AppUser u WHERE u.phoneNumber IS NULL AND u.status = 'ACTIVE'")
    List<AppUser> findActiveUsersWithoutPhoneNumber();

    /**
     * Search users by multiple criteria for admin interface
     */
    @Query("SELECT u FROM AppUser u WHERE " +
           "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:firstName IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))) AND " +
           "(:lastName IS NULL OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:minCompleteness IS NULL OR u.profileCompleteness >= :minCompleteness)")
    Page<AppUser> searchUsers(
            @Param("email") String email,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("status") UserStatus status,
            @Param("minCompleteness") Integer minCompleteness,
            Pageable pageable);

    // === Statistics and Reporting ===

    /**
     * Get user statistics for dashboard
     */
    @Query("SELECT " +
           "COUNT(*) as totalUsers, " +
           "COUNT(CASE WHEN u.status = 'ACTIVE' THEN 1 END) as activeUsers, " +
           "COUNT(CASE WHEN u.keycloakId IS NOT NULL THEN 1 END) as syncedUsers, " +
           "AVG(u.profileCompleteness) as avgCompleteness " +
           "FROM AppUser u")
    UserStatistics getUserStatistics();

    /**
     * Interface for user statistics projection
     */
    interface UserStatistics {
        long getTotalUsers();
        long getActiveUsers();
        long getSyncedUsers();
        double getAvgCompleteness();
    }

    // === Advanced Queries ===

    /**
     * Find users for targeted campaigns based on profile status
     */
    @Query("SELECT u FROM AppUser u WHERE " +
           "u.status = 'ACTIVE' AND " +
           "u.profileCompleteness BETWEEN :minCompleteness AND :maxCompleteness AND " +
           "u.lastSyncAt >= :recentThreshold")
    List<AppUser> findUsersForTargetedCampaigns(
            @Param("minCompleteness") Integer minCompleteness,
            @Param("maxCompleteness") Integer maxCompleteness,
            @Param("recentThreshold") Instant recentThreshold);

    /**
     * Find potential duplicate users by email pattern
     */
    @Query("SELECT u FROM AppUser u WHERE " +
           "EXISTS (SELECT u2 FROM AppUser u2 WHERE " +
           "u2.id != u.id AND " +
           "LOWER(REPLACE(u2.email, '.', '')) = LOWER(REPLACE(u.email, '.', '')))")
    List<AppUser> findPotentialDuplicateUsers();

    // === Cache Warmup Support Queries ===

    /**
     * Find most active users for cache warmup
     * Orders by last sync time and profile completeness
     */
    @Query("SELECT u FROM AppUser u WHERE u.status = 'ACTIVE' " +
           "ORDER BY u.lastSyncAt DESC NULLS LAST, u.profileCompleteness DESC, u.updatedAt DESC")
    List<AppUser> findMostActiveUsers(@Param("limit") int limit);

    /**
     * Find recently active users since given timestamp
     */
    @Query("SELECT u FROM AppUser u WHERE u.status = 'ACTIVE' AND " +
           "(u.lastSyncAt >= :since OR u.updatedAt >= :since) " +
           "ORDER BY GREATEST(COALESCE(u.lastSyncAt, u.updatedAt), u.updatedAt) DESC")
    List<AppUser> findRecentlyActiveUsers(@Param("since") Instant since, @Param("limit") int limit);

    /**
     * Count active users for warmup statistics
     */
    @Query("SELECT COUNT(u) FROM AppUser u WHERE u.status = 'ACTIVE'")
    long countActiveUsers();

    /**
     * Count recently active users for warmup statistics
     */
    @Query("SELECT COUNT(u) FROM AppUser u WHERE u.status = 'ACTIVE' AND " +
           "(u.lastSyncAt >= :since OR u.updatedAt >= :since)")
    long countRecentlyActiveUsers(@Param("since") Instant since);
}
