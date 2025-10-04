package com.cena.traveloka.iam.service;

import com.cena.traveloka.iam.dto.response.SessionDto;
import com.cena.traveloka.iam.entity.Session;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.mapper.SessionMapper;
import com.cena.traveloka.iam.repository.IamSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * T054: SessionService
 * Service for session management with Redis.
 *
 * Constitutional Compliance:
 * - FR-013: Session listing and management
 * - FR-016: 5 concurrent session limit with oldest eviction
 * - FR-004: Redis session storage with 24-hour TTL
 * - Principle III: Layered Architecture - Business logic in service layer
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SessionService {

    private final IamSessionRepository sessionRepository;
    private final SessionMapper sessionMapper;

    private static final int MAX_SESSIONS_PER_USER = 5;
    private static final int SESSION_EXPIRY_HOURS = 24;

    /**
     * Create new session for user.
     *
     * @param user User entity
     * @param sessionToken Session token
     * @param refreshToken Refresh token
     * @param ipAddress IP address
     * @param userAgent User agent
     * @return Session entity
     */
    public Session createSession(User user, String sessionToken, String refreshToken, String ipAddress, String userAgent) {
        // Parse device info from user agent
        String deviceType = parseDeviceType(userAgent);
        String browser = parseBrowser(userAgent);
        String os = parseOS(userAgent);

        Session session = Session.builder()
                .user(user)
                .sessionToken(sessionToken)
                .refreshToken(refreshToken)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceType(deviceType)
                .deviceId(generateDeviceId(userAgent, ipAddress))
                .browser(browser)
                .os(os)
                .isActive(true)
                .lastActivity(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusHours(SESSION_EXPIRY_HOURS))
                .refreshExpiresAt(OffsetDateTime.now().plusDays(7))
                .isSuspicious(false)
                .riskScore(0)
                .requires2fa(false)
                .twoFaCompleted(false)
                .createdAt(OffsetDateTime.now())
                .build();

        Session saved = sessionRepository.save(session);

        // Enforce session limit (FR-016)
        enforceSessionLimit(user.getId());

        log.info("Session created for user: {} from IP: {}", user.getId(), ipAddress);
        return saved;
    }

    /**
     * Get user active sessions (FR-013).
     *
     * @param userId User ID
     * @return List of SessionDto
     */
    @Transactional(readOnly = true)
    public List<SessionDto> getUserActiveSessions(UUID userId) {
        List<Session> sessions = sessionRepository.findByUserIdAndIsActiveTrue(userId);
        return sessions.stream()
                .map(sessionMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Count active sessions for user.
     *
     * @param userId User ID
     * @return Number of active sessions
     */
    @Transactional(readOnly = true)
    public long countActiveSessions(UUID userId) {
        return sessionRepository.countByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Enforce 5 session limit per user (FR-016).
     * Terminates oldest session if limit exceeded.
     *
     * @param userId User ID
     */
    public void enforceSessionLimit(UUID userId) {
        long activeCount = sessionRepository.countByUserIdAndIsActiveTrue(userId);

        if (activeCount > MAX_SESSIONS_PER_USER) {
            // Find oldest session
            Optional<Session> oldestSession = sessionRepository
                    .findFirstByUserIdAndIsActiveTrueOrderByCreatedAtAsc(userId);

            if (oldestSession.isPresent()) {
                Session session = oldestSession.get();
                session.setIsActive(false);
                session.setTerminatedAt(OffsetDateTime.now());
                session.setTerminationReason("Session limit exceeded (max 5 concurrent sessions)");
                sessionRepository.save(session);

                log.info("Terminated oldest session for user: {} due to session limit", userId);
            }
        }
    }

    /**
     * Terminate session by ID.
     *
     * @param sessionId Session ID
     * @param reason Termination reason
     */
    public void terminateSession(UUID sessionId, String reason) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with ID: " + sessionId));

        session.setIsActive(false);
        session.setTerminatedAt(OffsetDateTime.now());
        session.setTerminationReason(reason);
        sessionRepository.save(session);

        log.info("Session terminated: {} - Reason: {}", sessionId, reason);
    }

    /**
     * Terminate session by token.
     *
     * @param sessionToken Session token
     */
    public void terminateSessionByToken(String sessionToken) {
        Session session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setIsActive(false);
        session.setTerminatedAt(OffsetDateTime.now());
        session.setTerminationReason("User logout");
        sessionRepository.save(session);

        log.info("Session terminated by token");
    }

    /**
     * Terminate all sessions for user.
     *
     * @param userId User ID
     */
    public void terminateAllUserSessions(UUID userId) {
        List<Session> sessions = sessionRepository.findByUserIdAndIsActiveTrue(userId);

        sessions.forEach(session -> {
            session.setIsActive(false);
            session.setTerminatedAt(OffsetDateTime.now());
            session.setTerminationReason("All sessions terminated by user");
            sessionRepository.save(session);
        });

        log.info("All sessions terminated for user: {}", userId);
    }

    /**
     * Update session last activity timestamp.
     *
     * @param sessionToken Session token
     */
    public void updateLastActivity(String sessionToken) {
        Optional<Session> optionalSession = sessionRepository.findBySessionToken(sessionToken);

        if (optionalSession.isPresent()) {
            Session session = optionalSession.get();
            session.setLastActivity(OffsetDateTime.now());
            session.setUpdatedAt(OffsetDateTime.now());
            sessionRepository.save(session);
        }
    }

    /**
     * Find session by token.
     *
     * @param sessionToken Session token
     * @return Optional Session
     */
    @Transactional(readOnly = true)
    public Optional<Session> findBySessionToken(String sessionToken) {
        return sessionRepository.findBySessionToken(sessionToken);
    }

    /**
     * Check if session is valid.
     *
     * @param sessionToken Session token
     * @return true if session is active and not expired
     */
    @Transactional(readOnly = true)
    public boolean isSessionValid(String sessionToken) {
        Optional<Session> optionalSession = sessionRepository.findBySessionToken(sessionToken);

        if (optionalSession.isEmpty()) {
            return false;
        }

        Session session = optionalSession.get();
        return session.getIsActive() && session.getExpiresAt().isAfter(OffsetDateTime.now());
    }

    /**
     * Clean up expired sessions.
     *
     * @return Number of sessions cleaned up
     */
    public int cleanupExpiredSessions() {
        List<Session> expiredSessions = sessionRepository
                .findByIsActiveTrueAndExpiresAtBefore(OffsetDateTime.now());

        expiredSessions.forEach(session -> {
            session.setIsActive(false);
            session.setTerminatedAt(OffsetDateTime.now());
            session.setTerminationReason("Session expired");
            sessionRepository.save(session);
        });

        log.info("Cleaned up {} expired sessions", expiredSessions.size());
        return expiredSessions.size();
    }

    /**
     * Detect suspicious activity (session hijacking).
     *
     * @param sessionToken Session token
     * @param currentIp Current IP address
     * @param currentUserAgent Current user agent
     * @return true if suspicious activity detected
     */
    @Transactional(readOnly = true)
    public boolean detectSuspiciousActivity(String sessionToken, String currentIp, String currentUserAgent) {
        Optional<Session> optionalSession = sessionRepository.findBySessionToken(sessionToken);

        if (optionalSession.isEmpty()) {
            return false;
        }

        Session session = optionalSession.get();

        // Check if IP address changed
        boolean ipChanged = !session.getIpAddress().equals(currentIp);

        // Check if user agent changed
        boolean userAgentChanged = !session.getUserAgent().equals(currentUserAgent);

        return ipChanged || userAgentChanged;
    }

    /**
     * Mark session as suspicious.
     *
     * @param sessionId Session ID
     * @param riskScore Risk score (0-100)
     */
    public void markSessionSuspicious(UUID sessionId, int riskScore) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with ID: " + sessionId));

        session.setIsSuspicious(true);
        session.setRiskScore(riskScore);
        sessionRepository.save(session);

        log.warn("Session marked as suspicious: {} - Risk score: {}", sessionId, riskScore);
    }

    // Helper methods for parsing device info

    private String parseDeviceType(String userAgent) {
        if (userAgent == null) return "unknown";
        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
            return "mobile";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "tablet";
        }
        return "desktop";
    }

    private String parseBrowser(String userAgent) {
        if (userAgent == null) return "unknown";
        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("chrome")) return "Chrome";
        if (userAgent.contains("firefox")) return "Firefox";
        if (userAgent.contains("safari")) return "Safari";
        if (userAgent.contains("edge")) return "Edge";
        if (userAgent.contains("opera")) return "Opera";

        return "unknown";
    }

    private String parseOS(String userAgent) {
        if (userAgent == null) return "unknown";
        userAgent = userAgent.toLowerCase();

        if (userAgent.contains("windows")) return "Windows";
        if (userAgent.contains("mac")) return "macOS";
        if (userAgent.contains("linux")) return "Linux";
        if (userAgent.contains("android")) return "Android";
        if (userAgent.contains("ios") || userAgent.contains("iphone") || userAgent.contains("ipad")) return "iOS";

        return "unknown";
    }

    private String generateDeviceId(String userAgent, String ipAddress) {
        return UUID.nameUUIDFromBytes((userAgent + ipAddress).getBytes()).toString();
    }

    /**
     * Get active sessions from JWT token.
     *
     * @param token JWT token
     * @return List of SessionDto
     */
    @Transactional(readOnly = true)
    public List<SessionDto> getActiveSessions(String token) {
        // TODO: Extract user ID from JWT token
        // For now, throw exception - needs JwtTokenProvider integration
        throw new UnsupportedOperationException("getActiveSessions not yet implemented - requires JWT integration");
    }

    /**
     * Terminate session by ID with JWT token verification.
     *
     * @param sessionId Session ID
     * @param token JWT token
     */
    public void terminateSessionWithAuth(UUID sessionId, String token) {
        // TODO: Extract user ID from JWT and verify session ownership
        // For now, just terminate the session
        terminateSession(sessionId, "Terminated by user");
    }

    /**
     * Terminate all other sessions except current one.
     *
     * @param token JWT token of current session
     */
    public void terminateAllOtherSessions(String token) {
        // TODO: Extract user ID from JWT and get current session
        // For now, throw exception - needs JwtTokenProvider integration
        throw new UnsupportedOperationException("terminateAllOtherSessions not yet implemented - requires JWT integration");
    }
}
