package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.UserSession;
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
 * Repository interface for UserSession entity operations with cleanup capabilities
 *
 * Provides comprehensive session management operations including:
 * - Multi-device session tracking
 * - Automatic session cleanup and expiry
 * - Device fingerprinting and security
 * - Session analytics and monitoring
 * - Geographic and usage patterns
 *
 * Key Features:
 * - Session lifecycle management
 * - Cleanup operations for expired sessions
 * - Device-based session queries
 * - Security and fraud detection
 * - Performance-optimized queries with proper indexing
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    // === Session Lifecycle Management ===

    /**
     * Find session by session ID - primary lookup for authentication
     * Uses unique index on session_id for optimal performance
     */
    @Query("SELECT s FROM UserSession s WHERE s.sessionId = :sessionId")
    Optional<UserSession> findBySessionId(@Param("sessionId") String sessionId);

    /**
     * Find active session by session ID
     */
    @Query("SELECT s FROM UserSession s WHERE s.sessionId = :sessionId AND s.status = 'ACTIVE'")
    Optional<UserSession> findActiveBySessionId(@Param("sessionId") String sessionId);

    /**
     * Check if session exists and is active
     */
    @Query("SELECT COUNT(s) > 0 FROM UserSession s WHERE s.sessionId = :sessionId AND s.status = 'ACTIVE'")
    boolean existsActiveSession(@Param("sessionId") String sessionId);

    // === User Session Management ===

    /**
     * Find all sessions for a user
     * Uses index on user_id for performance
     */
    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId ORDER BY s.lastAccessedAt DESC")
    List<UserSession> findByUserId(@Param("userId") String userId);

    /**
     * Find active sessions for a user
     */
    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.status = 'ACTIVE' ORDER BY s.lastAccessedAt DESC")
    List<UserSession> findActiveSessionsByUserId(@Param("userId") String userId);

    /**
     * Count active sessions for a user
     */
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    long countActiveSessionsByUserId(@Param("userId") String userId);

    /**
     * Find sessions by user and device ID
     */
    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.deviceId = :deviceId ORDER BY s.lastAccessedAt DESC")
    List<UserSession> findByUserIdAndDeviceId(@Param("userId") String userId, @Param("deviceId") String deviceId);

    // === Session Cleanup Operations ===

    /**
     * Find expired sessions for cleanup
     * Critical for session management and security
     */
    @Query("SELECT s FROM UserSession s WHERE s.expiresAt < :currentTime AND s.status = 'ACTIVE'")
    List<UserSession> findExpiredSessions(@Param("currentTime") Instant currentTime);

    /**
     * Find sessions not accessed for specified duration (stale sessions)
     */
    @Query("SELECT s FROM UserSession s WHERE s.lastAccessedAt < :threshold AND s.status = 'ACTIVE'")
    List<UserSession> findStaleActiveSessions(@Param("threshold") Instant threshold);

    /**
     * Find sessions to cleanup based on status and age
     */
    @Query("SELECT s FROM UserSession s WHERE " +
           "(s.status = 'EXPIRED' OR s.status = 'TERMINATED') AND " +
           "s.updatedAt < :cleanupThreshold")
    List<UserSession> findSessionsForCleanup(@Param("cleanupThreshold") Instant cleanupThreshold);

    /**
     * Bulk expire sessions that have passed their expiry time
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.status = 'EXPIRED', s.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE s.expiresAt < :currentTime AND s.status = 'ACTIVE'")
    int expireSessionsPastExpiry(@Param("currentTime") Instant currentTime);

    /**
     * Bulk terminate stale sessions not accessed recently
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.status = 'TERMINATED', s.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE s.lastAccessedAt < :threshold AND s.status = 'ACTIVE'")
    int terminateStaleActiveSessions(@Param("threshold") Instant threshold);

    /**
     * Delete old terminated/expired sessions (hard cleanup)
     */
    @Modifying
    @Query("DELETE FROM UserSession s WHERE " +
           "(s.status = 'EXPIRED' OR s.status = 'TERMINATED') AND " +
           "s.updatedAt < :deleteThreshold")
    int deleteOldTerminatedSessions(@Param("deleteThreshold") Instant deleteThreshold);

    // === Device and Security Management ===

    /**
     * Find sessions by device type for analytics
     */
    @Query("SELECT s FROM UserSession s WHERE s.deviceType = :deviceType")
    Page<UserSession> findByDeviceType(@Param("deviceType") String deviceType, Pageable pageable);

    /**
     * Find sessions by IP address for security monitoring
     */
    @Query("SELECT s FROM UserSession s WHERE s.ipAddress = :ipAddress ORDER BY s.createdAt DESC")
    List<UserSession> findByIpAddress(@Param("ipAddress") String ipAddress);

    /**
     * Find suspicious sessions with multiple IP addresses for same user/device
     */
    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.deviceId = :deviceId " +
           "AND s.ipAddress != :currentIp AND s.status = 'ACTIVE'")
    List<UserSession> findSuspiciousSessionsForUserDevice(
            @Param("userId") String userId,
            @Param("deviceId") String deviceId,
            @Param("currentIp") String currentIp);

    /**
     * Find sessions from specific geographic location
     */
    @Query("SELECT s FROM UserSession s WHERE s.locationCity LIKE CONCAT('%', :location, '%') OR s.locationCountry LIKE CONCAT('%', :location, '%')")
    List<UserSession> findByLocationContaining(@Param("location") String location);

    // === Session Status Management ===

    /**
     * Find sessions by status with pagination
     */
    @Query("SELECT s FROM UserSession s WHERE s.status = :status ORDER BY s.lastAccessedAt DESC")
    Page<UserSession> findByStatus(@Param("status") String status, Pageable pageable);

    /**
     * Count sessions by status for monitoring
     */
    @Query("SELECT s.status, COUNT(s) FROM UserSession s GROUP BY s.status")
    List<Object[]> countByStatus();

    /**
     * Bulk update session status
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.status = :newStatus, s.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE s.id IN :sessionIds")
    int updateSessionStatus(@Param("sessionIds") List<String> sessionIds, @Param("newStatus") String newStatus);

    /**
     * Terminate all sessions for a user (logout from all devices)
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.status = 'TERMINATED', s.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    int terminateAllUserSessions(@Param("userId") String userId);

    /**
     * Terminate sessions except current one
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.status = 'TERMINATED', s.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE s.userId = :userId AND s.sessionId != :currentSessionId AND s.status = 'ACTIVE'")
    int terminateOtherUserSessions(@Param("userId") String userId, @Param("currentSessionId") String currentSessionId);

    // === Session Activity Tracking ===

    /**
     * Update last accessed time for session activity tracking
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.lastAccessedAt = :accessTime, s.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE s.sessionId = :sessionId AND s.status = 'ACTIVE'")
    int updateLastAccessedTime(@Param("sessionId") String sessionId, @Param("accessTime") Instant accessTime);

    /**
     * Extend session expiry time
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.expiresAt = :newExpiryTime, s.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE s.sessionId = :sessionId AND s.status = 'ACTIVE'")
    int extendSessionExpiry(@Param("sessionId") String sessionId, @Param("newExpiryTime") Instant newExpiryTime);

    /**
     * Find recently active sessions for user experience tracking
     */
    @Query("SELECT s FROM UserSession s WHERE s.lastAccessedAt >= :since ORDER BY s.lastAccessedAt DESC")
    List<UserSession> findRecentlyActiveSessions(@Param("since") Instant since);

    // === Analytics and Reporting ===

    /**
     * Find sessions created within date range for analytics
     */
    @Query("SELECT s FROM UserSession s WHERE s.createdAt BETWEEN :startDate AND :endDate ORDER BY s.createdAt DESC")
    Page<UserSession> findSessionsCreatedBetween(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    /**
     * Get session statistics for dashboard
     */
    @Query("SELECT " +
           "COUNT(*) as totalSessions, " +
           "COUNT(CASE WHEN s.status = 'ACTIVE' THEN 1 END) as activeSessions, " +
           "COUNT(CASE WHEN s.status = 'EXPIRED' THEN 1 END) as expiredSessions, " +
           "COUNT(CASE WHEN s.status = 'TERMINATED' THEN 1 END) as terminatedSessions, " +
           "COUNT(DISTINCT s.userId) as uniqueUsers, " +
           "COUNT(DISTINCT s.deviceId) as uniqueDevices " +
           "FROM UserSession s")
    SessionStatistics getSessionStatistics();

    /**
     * Interface for session statistics projection
     */
    interface SessionStatistics {
        long getTotalSessions();
        long getActiveSessions();
        long getExpiredSessions();
        long getTerminatedSessions();
        long getUniqueUsers();
        long getUniqueDevices();
    }

    /**
     * Get device type distribution for analytics
     */
    @Query("SELECT s.deviceType, COUNT(*) FROM UserSession s GROUP BY s.deviceType")
    List<Object[]> getDeviceTypeDistribution();

    /**
     * Find long-running sessions for analysis
     */
    @Query("SELECT s FROM UserSession s WHERE " +
           "s.status = 'ACTIVE' AND " +
           "s.createdAt < :threshold")
    List<UserSession> findLongRunningSessions(@Param("threshold") Instant threshold);

    // === Advanced Queries ===

    /**
     * Find sessions with concurrent logins for security analysis
     */
    @Query("SELECT s FROM UserSession s WHERE s.userId IN (" +
           "SELECT s2.userId FROM UserSession s2 WHERE s2.status = 'ACTIVE' " +
           "GROUP BY s2.userId HAVING COUNT(s2) > :maxConcurrentSessions)")
    List<UserSession> findUsersWithExcessiveConcurrentSessions(@Param("maxConcurrentSessions") long maxConcurrentSessions);

    /**
     * Find sessions by user agent pattern for bot detection
     */
    @Query("SELECT s FROM UserSession s WHERE s.userAgent LIKE %:pattern%")
    List<UserSession> findSessionsByUserAgentPattern(@Param("pattern") String pattern);

    /**
     * Search sessions by multiple criteria for admin interface
     */
    @Query("SELECT s FROM UserSession s WHERE " +
           "(:userId IS NULL OR s.userId = :userId) AND " +
           "(:deviceType IS NULL OR s.deviceType = :deviceType) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:ipAddress IS NULL OR s.ipAddress = :ipAddress) AND " +
           "(:location IS NULL OR s.locationCity LIKE CONCAT('%', :location, '%') OR s.locationCountry LIKE CONCAT('%', :location, '%'))")
    Page<UserSession> searchSessions(
            @Param("userId") String userId,
            @Param("deviceType") String deviceType,
            @Param("status") String status,
            @Param("ipAddress") String ipAddress,
            @Param("location") String location,
            Pageable pageable);

    // === Cleanup Maintenance Operations ===

    /**
     * Get cleanup statistics for maintenance dashboard
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN s.expiresAt < CURRENT_TIMESTAMP AND s.status = 'ACTIVE' THEN 1 END) as sessionsToExpire, " +
           "COUNT(CASE WHEN s.lastAccessedAt < :staleThreshold AND s.status = 'ACTIVE' THEN 1 END) as staleSessions, " +
           "COUNT(CASE WHEN (s.status = 'EXPIRED' OR s.status = 'TERMINATED') AND s.updatedAt < :deleteThreshold THEN 1 END) as sessionsToDelete " +
           "FROM UserSession s")
    CleanupStatistics getCleanupStatistics(@Param("staleThreshold") Instant staleThreshold, @Param("deleteThreshold") Instant deleteThreshold);

    /**
     * Interface for cleanup statistics projection
     */
    interface CleanupStatistics {
        long getSessionsToExpire();
        long getStaleSessions();
        long getSessionsToDelete();
    }

    /**
     * Find sessions requiring immediate cleanup action
     */
    @Query("SELECT s FROM UserSession s WHERE " +
           "(s.expiresAt < CURRENT_TIMESTAMP AND s.status = 'ACTIVE') OR " +
           "(s.lastAccessedAt < :staleThreshold AND s.status = 'ACTIVE') OR " +
           "((s.status = 'EXPIRED' OR s.status = 'TERMINATED') AND s.updatedAt < :deleteThreshold)")
    List<UserSession> findSessionsRequiringCleanup(
            @Param("staleThreshold") Instant staleThreshold,
            @Param("deleteThreshold") Instant deleteThreshold);

    // === Cache Warmup Support Queries ===

    /**
     * Count all active sessions for warmup statistics
     */
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.status = 'ACTIVE'")
    long countActiveSessions();

    /**
     * Count recently active sessions for warmup statistics
     */
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.status = 'ACTIVE' AND s.lastAccessedAt >= :since")
    long countRecentlyActiveSessions(@Param("since") Instant since);
}
