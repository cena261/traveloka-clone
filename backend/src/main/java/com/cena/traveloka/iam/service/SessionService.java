package com.cena.traveloka.iam.service;

import com.cena.traveloka.iam.entity.UserSession;
import com.cena.traveloka.iam.enums.DeviceType;
import com.cena.traveloka.iam.enums.SessionStatus;
import com.cena.traveloka.iam.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for UserSession entity operations and lifecycle management
 *
 * Provides comprehensive session management functionality including:
 * - Session creation, validation, and termination
 * - Multi-device session tracking
 * - Automatic session cleanup and maintenance
 * - Security monitoring and fraud detection
 * - Session analytics and reporting
 * - Device fingerprinting and tracking
 *
 * Key Features:
 * - Transactional operations for data consistency
 * - Redis caching for session performance
 * - Automatic cleanup of expired sessions
 * - Security validation and monitoring
 * - Multi-device session management
 * - Performance optimization for high concurrency
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SessionService {

    private final UserSessionRepository userSessionRepository;
    private final SessionCacheService sessionCacheService;

    @Value("${app.session.default-timeout-hours:24}")
    private int defaultSessionTimeoutHours;

    @Value("${app.session.max-concurrent-sessions:5}")
    private int maxConcurrentSessions;

    @Value("${app.session.cleanup.stale-hours:72}")
    private int staleSessionHours;

    @Value("${app.session.cleanup.delete-after-days:30}")
    private int deleteSessionsAfterDays;

    // === Session Lifecycle Management ===

    /**
     * Create a new user session
     *
     * @param userId User ID
     * @param deviceId Device identifier
     * @param deviceType Device type (WEB, MOBILE_IOS, MOBILE_ANDROID)
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     * @param location Geographic location (optional)
     * @return Created session
     */
    public UserSession createSession(String userId, String deviceId, String deviceType,
                                   String ipAddress, String userAgent, String location) {
        log.info("Creating new session for user: {} on device: {}", userId, deviceId);

        validateSessionCreation(userId, deviceType, ipAddress);

        // Check concurrent session limit
        enforceSessionLimits(userId);

        // Generate unique session ID
        String sessionId = generateSessionId();

        // Calculate expiry time
        Instant expiresAt = Instant.now().plusSeconds(defaultSessionTimeoutHours * 3600L);

        UserSession session = new UserSession();
        session.setUserId(UUID.fromString(userId));
        session.setSessionId(sessionId);
        // deviceId field doesn't exist in entity, storing in metadata instead
        if (deviceId != null) {
            session.getMetadata().put("deviceId", deviceId);
        }
        session.setDeviceType(DeviceType.valueOf(deviceType));
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        if (location != null && !location.trim().isEmpty()) {
            session.setLocation(location.trim());
            // Store parsed location parts in metadata for future reference
            String[] locationParts = location.split(",");
            if (locationParts.length >= 2) {
                session.getMetadata().put("locationCity", locationParts[0].trim());
                session.getMetadata().put("locationCountry", locationParts[1].trim());
            }
        }
        session.setStatus("ACTIVE");
        session.setCreatedAt(OffsetDateTime.now());
        session.setLastActivityAt(OffsetDateTime.now());
        session.setExpiresAt(expiresAt.atOffset(java.time.ZoneOffset.UTC));

        UserSession savedSession = userSessionRepository.save(session);

        sessionCacheService.cacheSession(savedSession);

        log.info("Successfully created session with ID: {}", savedSession.getSessionId());

        return savedSession;
    }

    /**
     * Find session by session ID with caching
     *
     * @param sessionId Session ID
     * @return Session if found and active
     */
    @Transactional(readOnly = true)
    public Optional<UserSession> findActiveSession(String sessionId) {
        log.debug("Finding active session by ID: {}", sessionId);

        // Try cache first
        Optional<UserSession> cachedSession = sessionCacheService.getCachedSession(sessionId);
        if (cachedSession.isPresent()) {
            // Verify session is still active and not expired
            UserSession session = cachedSession.get();
            if ("ACTIVE".equals(session.getStatus()) &&
                (session.getExpiresAt() == null || session.getExpiresAt().isAfter(OffsetDateTime.now()))) {
                return cachedSession;
            } else {
                // Session is expired or inactive, evict from cache
                sessionCacheService.evictSession(sessionId);
            }
        }

        // Fallback to database
        Optional<UserSession> session = userSessionRepository.findActiveBySessionId(sessionId);
        session.ifPresent(sessionCacheService::cacheSession);

        return session;
    }

    /**
     * Validate and refresh session activity
     *
     * @param sessionId Session ID to refresh
     * @return true if session was successfully refreshed
     */
    public boolean refreshSession(String sessionId) {
        log.debug("Refreshing session: {}", sessionId);

        Optional<UserSession> sessionOpt = userSessionRepository.findActiveBySessionId(sessionId);
        if (sessionOpt.isEmpty()) {
            log.warn("Session not found or not active: {}", sessionId);
            sessionCacheService.evictSession(sessionId);
            return false;
        }

        UserSession session = sessionOpt.get();

        // Check if session is expired
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            log.warn("Session expired: {}", sessionId);
            expireSession(sessionId, "SYSTEM");
            return false;
        }

        // Update last accessed time - using session update method instead
        session.setLastActivityAt(OffsetDateTime.now());
        userSessionRepository.save(session);
        int updated = 1;
        if (updated > 0) {
            // Update cache with new last accessed time
            sessionCacheService.updateSessionLastAccess(sessionId);
            log.debug("Successfully refreshed session: {}", sessionId);
            return true;
        }

        return false;
    }

    /**
     * Extend session expiry time
     *
     * @param sessionId Session ID to extend
     * @param additionalHours Additional hours to extend
     * @return true if session was successfully extended
     */
    public boolean extendSession(String sessionId, int additionalHours) {
        log.info("Extending session {} by {} hours", sessionId, additionalHours);

        Instant newExpiryTime = Instant.now().plusSeconds(additionalHours * 3600L);
        int updated = userSessionRepository.extendSessionExpiry(sessionId, newExpiryTime);

        if (updated > 0) {
            // Evict from cache to force fresh data on next access
            sessionCacheService.evictSession(sessionId);
            log.info("Successfully extended session: {}", sessionId);
            return true;
        }

        log.warn("Failed to extend session: {}", sessionId);
        return false;
    }

    /**
     * Terminate session
     *
     * @param sessionId Session ID to terminate
     * @param terminatedBy Who terminated the session
     * @return true if session was successfully terminated
     */
    public boolean terminateSession(String sessionId, String terminatedBy) {
        log.info("Terminating session: {}", sessionId);

        int updated = userSessionRepository.updateSessionStatus(List.of(sessionId), "TERMINATED");
        if (updated > 0) {
            // Evict from cache since session is no longer active
            sessionCacheService.evictSession(sessionId);
            log.info("Successfully terminated session: {}", sessionId);
            return true;
        }

        log.warn("Failed to terminate session: {}", sessionId);
        return false;
    }

    /**
     * Expire session due to timeout
     *
     * @param sessionId Session ID to expire
     * @param expiredBy Who expired the session
     * @return true if session was successfully expired
     */
    public boolean expireSession(String sessionId, String expiredBy) {
        log.info("Expiring session: {}", sessionId);

        int updated = userSessionRepository.updateSessionStatus(List.of(sessionId), "EXPIRED");
        if (updated > 0) {
            // Evict from cache since session is no longer active
            sessionCacheService.evictSession(sessionId);
            log.info("Successfully expired session: {}", sessionId);
            return true;
        }

        log.warn("Failed to expire session: {}", sessionId);
        return false;
    }

    // === Multi-Device Session Management ===

    /**
     * Find all sessions for a user
     *
     * @param userId User ID
     * @return List of user sessions
     */
    @Transactional(readOnly = true)
    public List<UserSession> findUserSessions(String userId) {
        log.debug("Finding all sessions for user: {}", userId);
        return userSessionRepository.findByUserId(userId);
    }

    /**
     * Find active sessions for a user
     *
     * @param userId User ID
     * @return List of active user sessions
     */
    @Transactional(readOnly = true)
    public List<UserSession> findActiveUserSessions(String userId) {
        log.debug("Finding active sessions for user: {}", userId);

        // Try to get session IDs from cache first
        java.util.Set<String> cachedSessionIds = sessionCacheService.getCachedUserSessions(userId);
        if (!cachedSessionIds.isEmpty()) {
            log.debug("Found {} cached session IDs for user: {}", cachedSessionIds.size(), userId);
        }

        // Always fetch from database for consistency
        List<UserSession> sessions = userSessionRepository.findActiveSessionsByUserId(userId);

        // Cache the sessions we found
        for (UserSession session : sessions) {
            sessionCacheService.cacheSession(session);
        }

        return sessions;
    }

    /**
     * Count active sessions for a user
     *
     * @param userId User ID
     * @return Number of active sessions
     */
    @Transactional(readOnly = true)
    public long countActiveUserSessions(String userId) {
        log.debug("Counting active sessions for user: {}", userId);

        // Try cache first
        Optional<Long> cachedCount = sessionCacheService.getCachedUserSessionCount(userId);
        if (cachedCount.isPresent()) {
            return cachedCount.get();
        }

        // Fallback to database
        long count = userSessionRepository.countActiveSessionsByUserId(userId);

        // Cache the count
        sessionCacheService.cacheUserSessionCount(userId, count);

        return count;
    }

    /**
     * Terminate all sessions for a user (logout from all devices)
     *
     * @param userId User ID
     * @param terminatedBy Who terminated the sessions
     * @return Number of sessions terminated
     */
    public int terminateAllUserSessions(String userId, String terminatedBy) {
        log.info("Terminating all sessions for user: {}", userId);

        int terminated = userSessionRepository.terminateAllUserSessions(userId);

        // Evict all sessions for this user from cache
        sessionCacheService.evictUserSessions(userId);

        log.info("Successfully terminated {} sessions for user: {}", terminated, userId);

        return terminated;
    }

    /**
     * Terminate other sessions except current one
     *
     * @param userId User ID
     * @param currentSessionId Current session to keep active
     * @param terminatedBy Who terminated the sessions
     * @return Number of sessions terminated
     */
    public int terminateOtherUserSessions(String userId, String currentSessionId, String terminatedBy) {
        log.info("Terminating other sessions for user: {} except: {}", userId, currentSessionId);

        int terminated = userSessionRepository.terminateOtherUserSessions(userId, currentSessionId);

        // Evict all sessions for this user from cache except current one
        java.util.Set<String> sessionIds = sessionCacheService.getCachedUserSessions(userId);
        for (String sessionId : sessionIds) {
            if (!sessionId.equals(currentSessionId)) {
                sessionCacheService.evictSession(sessionId);
            }
        }

        log.info("Successfully terminated {} other sessions for user: {}", terminated, userId);

        return terminated;
    }

    /**
     * Enforce session limits for user
     *
     * @param userId User ID to check
     */
    private void enforceSessionLimits(String userId) {
        long activeSessionCount = countActiveUserSessions(userId);

        if (activeSessionCount >= maxConcurrentSessions) {
            log.warn("User {} has {} active sessions, exceeding limit of {}",
                    userId, activeSessionCount, maxConcurrentSessions);

            // Terminate oldest sessions to make room
            List<UserSession> activeSessions = findActiveUserSessions(userId);
            if (!activeSessions.isEmpty()) {
                UserSession oldestSession = activeSessions.get(activeSessions.size() - 1);
                terminateSession(oldestSession.getSessionId(), "SYSTEM_LIMIT_ENFORCEMENT");
                log.info("Terminated oldest session {} for user {} due to session limit", oldestSession.getSessionId(), userId);
            }
        }
    }

    // === Security and Monitoring ===

    /**
     * Find sessions by IP address for security monitoring
     *
     * @param ipAddress IP address
     * @return List of sessions from this IP
     */
    @Transactional(readOnly = true)
    public List<UserSession> findSessionsByIpAddress(String ipAddress) {
        log.debug("Finding sessions by IP address: {}", ipAddress);
        return userSessionRepository.findByIpAddress(ipAddress);
    }

    /**
     * Find suspicious sessions for user/device combination
     *
     * @param userId User ID
     * @param deviceId Device ID
     * @param currentIp Current IP address
     * @return List of suspicious sessions
     */
    @Transactional(readOnly = true)
    public List<UserSession> findSuspiciousSessions(String userId, String deviceId, String currentIp) {
        log.debug("Finding suspicious sessions for user: {} device: {} from IP: {}", userId, deviceId, currentIp);
        return userSessionRepository.findSuspiciousSessionsForUserDevice(userId, deviceId, currentIp);
    }

    /**
     * Find users with excessive concurrent sessions
     *
     * @return List of sessions from users with too many concurrent sessions
     */
    @Transactional(readOnly = true)
    public List<UserSession> findUsersWithExcessiveSessions() {
        log.debug("Finding users with excessive concurrent sessions");
        return userSessionRepository.findUsersWithExcessiveConcurrentSessions((long) maxConcurrentSessions);
    }

    // === Session Cleanup and Maintenance ===

    /**
     * Scheduled cleanup of expired and stale sessions
     * Runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Async
    public void performScheduledCleanup() {
        log.info("Starting scheduled session cleanup");

        try {
            // Expire sessions past their expiry time
            int expiredCount = userSessionRepository.expireSessionsPastExpiry(Instant.now());
            log.info("Expired {} sessions past their expiry time", expiredCount);

            // Terminate stale active sessions
            Instant staleThreshold = Instant.now().minusSeconds(staleSessionHours * 3600L);
            int staleCount = userSessionRepository.terminateStaleActiveSessions(staleThreshold);
            log.info("Terminated {} stale active sessions", staleCount);

            // Delete old terminated/expired sessions
            Instant deleteThreshold = Instant.now().minusSeconds(deleteSessionsAfterDays * 24 * 3600L);
            int deletedCount = userSessionRepository.deleteOldTerminatedSessions(deleteThreshold);
            log.info("Deleted {} old terminated/expired sessions", deletedCount);

            log.info("Completed scheduled session cleanup - expired: {}, stale: {}, deleted: {}",
                    expiredCount, staleCount, deletedCount);

        } catch (Exception e) {
            log.error("Error during scheduled session cleanup", e);
        }
    }

    /**
     * Manual cleanup of sessions
     *
     * @return Cleanup statistics
     */
    public UserSessionRepository.CleanupStatistics performManualCleanup() {
        log.info("Starting manual session cleanup");

        Instant currentTime = Instant.now();
        Instant staleThreshold = currentTime.minusSeconds(staleSessionHours * 3600L);
        Instant deleteThreshold = currentTime.minusSeconds(deleteSessionsAfterDays * 24 * 3600L);

        // Get statistics before cleanup
        UserSessionRepository.CleanupStatistics stats = userSessionRepository.getCleanupStatistics(staleThreshold, deleteThreshold);

        // Perform cleanup
        performScheduledCleanup();

        log.info("Completed manual session cleanup");
        return stats;
    }

    /**
     * Find sessions requiring cleanup
     *
     * @return List of sessions that need cleanup
     */
    @Transactional(readOnly = true)
    public List<UserSession> findSessionsRequiringCleanup() {
        Instant staleThreshold = Instant.now().minusSeconds(staleSessionHours * 3600L);
        Instant deleteThreshold = Instant.now().minusSeconds(deleteSessionsAfterDays * 24 * 3600L);

        return userSessionRepository.findSessionsRequiringCleanup(staleThreshold, deleteThreshold);
    }

    // === Analytics and Reporting ===

    /**
     * Get session statistics for dashboard
     *
     * @return Session statistics
     */
    @Transactional(readOnly = true)
    public UserSessionRepository.SessionStatistics getSessionStatistics() {
        log.debug("Retrieving session statistics");

        // Try cache first
        Optional<Object> cachedStats = sessionCacheService.getCachedSessionStatistics();
        if (cachedStats.isPresent()) {
            return (UserSessionRepository.SessionStatistics) cachedStats.get();
        }

        // Fallback to database
        UserSessionRepository.SessionStatistics stats = userSessionRepository.getSessionStatistics();

        // Cache the statistics
        sessionCacheService.cacheSessionStatistics(stats);

        return stats;
    }

    /**
     * Get device type distribution
     *
     * @return List of device type counts
     */
    @Transactional(readOnly = true)
    public List<Object[]> getDeviceTypeDistribution() {
        log.debug("Getting device type distribution");
        return userSessionRepository.getDeviceTypeDistribution();
    }

    /**
     * Find long-running sessions
     *
     * @param thresholdHours Hours to consider long-running
     * @return List of long-running sessions
     */
    @Transactional(readOnly = true)
    public List<UserSession> findLongRunningSessions(int thresholdHours) {
        log.debug("Finding sessions running longer than {} hours", thresholdHours);

        Instant threshold = Instant.now().minusSeconds(thresholdHours * 3600L);
        return userSessionRepository.findLongRunningSessions(threshold);
    }

    /**
     * Find recently active sessions
     *
     * @param hours Number of hours to look back
     * @return List of recently active sessions
     */
    @Transactional(readOnly = true)
    public List<UserSession> findRecentlyActiveSessions(int hours) {
        log.debug("Finding sessions active in the last {} hours", hours);

        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return userSessionRepository.findRecentlyActiveSessions(since);
    }

    // === Search and Query Operations ===

    /**
     * Search sessions by multiple criteria
     *
     * @param userId User ID filter (optional)
     * @param deviceType Device type filter (optional)
     * @param status Status filter (optional)
     * @param ipAddress IP address filter (optional)
     * @param location Location filter (optional)
     * @param pageable Pagination parameters
     * @return Page of matching sessions
     */
    @Transactional(readOnly = true)
    public Page<UserSession> searchSessions(String userId, String deviceType, String status,
                                          String ipAddress, String location, Pageable pageable) {
        log.debug("Searching sessions with filters - user: {}, device: {}, status: {}, IP: {}, location: {}",
                 userId, deviceType, status, ipAddress, location);
        return userSessionRepository.searchSessions(userId, deviceType, status, ipAddress, location, pageable);
    }

    // === Validation and Helper Methods ===

    /**
     * Generate unique session ID
     *
     * @return Unique session identifier
     */
    private String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Validate session creation parameters
     *
     * @param userId User ID
     * @param deviceType Device type
     * @param ipAddress IP address
     * @throws IllegalArgumentException if validation fails
     */
    private void validateSessionCreation(String userId, String deviceType, String ipAddress) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("User ID is required");
        }

        if (!StringUtils.hasText(deviceType)) {
            throw new IllegalArgumentException("Device type is required");
        }

        try {
            DeviceType.valueOf(deviceType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid device type: " + deviceType + ". Valid types are: " +
                java.util.Arrays.toString(DeviceType.values()));
        }

        if (!StringUtils.hasText(ipAddress)) {
            throw new IllegalArgumentException("IP address is required");
        }

        // Basic IP address format validation
        if (!ipAddress.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$") &&
            !ipAddress.matches("^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$")) {
            throw new IllegalArgumentException("Invalid IP address format: " + ipAddress);
        }
    }

    /**
     * Check if session exists and is active
     *
     * @param sessionId Session ID to check
     * @return true if session exists and is active
     */
    @Transactional(readOnly = true)
    public boolean isSessionActive(String sessionId) {
        return userSessionRepository.existsActiveSession(sessionId);
    }

    /**
     * Check if a session belongs to a specific user (for security authorization)
     *
     * @param sessionId Session ID to check
     * @param userId User ID to verify ownership
     * @return true if the session belongs to the user
     */
    @Transactional(readOnly = true)
    public boolean isSessionOwner(String sessionId, String userId) {
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(userId)) {
            return false;
        }

        Optional<UserSession> session = findActiveSession(sessionId);
        return session.isPresent() && userId.equals(session.get().getUserId());
    }
}
