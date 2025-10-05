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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SessionService {

    private final IamSessionRepository sessionRepository;
    private final SessionMapper sessionMapper;

    private static final int MAX_SESSIONS_PER_USER = 5;
    private static final int SESSION_EXPIRY_HOURS = 24;

    public Session createSession(User user, String sessionToken, String refreshToken, String ipAddress, String userAgent) {
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

        enforceSessionLimit(user.getId());

        log.info("Session created for user: {} from IP: {}", user.getId(), ipAddress);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SessionDto> getUserActiveSessions(UUID userId) {
        List<Session> sessions = sessionRepository.findByUserIdAndIsActiveTrue(userId);
        return sessions.stream()
                .map(sessionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countActiveSessions(UUID userId) {
        return sessionRepository.countByUserIdAndIsActiveTrue(userId);
    }

    public void enforceSessionLimit(UUID userId) {
        long activeCount = sessionRepository.countByUserIdAndIsActiveTrue(userId);

        if (activeCount > MAX_SESSIONS_PER_USER) {
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

    public void terminateSession(UUID sessionId, String reason) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with ID: " + sessionId));

        session.setIsActive(false);
        session.setTerminatedAt(OffsetDateTime.now());
        session.setTerminationReason(reason);
        sessionRepository.save(session);

        log.info("Session terminated: {} - Reason: {}", sessionId, reason);
    }

    public void terminateSessionByToken(String sessionToken) {
        Session session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setIsActive(false);
        session.setTerminatedAt(OffsetDateTime.now());
        session.setTerminationReason("User logout");
        sessionRepository.save(session);

        log.info("Session terminated by token");
    }

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

    public void updateLastActivity(String sessionToken) {
        Optional<Session> optionalSession = sessionRepository.findBySessionToken(sessionToken);

        if (optionalSession.isPresent()) {
            Session session = optionalSession.get();
            session.setLastActivity(OffsetDateTime.now());
            session.setUpdatedAt(OffsetDateTime.now());
            sessionRepository.save(session);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Session> findBySessionToken(String sessionToken) {
        return sessionRepository.findBySessionToken(sessionToken);
    }

    @Transactional(readOnly = true)
    public boolean isSessionValid(String sessionToken) {
        Optional<Session> optionalSession = sessionRepository.findBySessionToken(sessionToken);

        if (optionalSession.isEmpty()) {
            return false;
        }

        Session session = optionalSession.get();
        return session.getIsActive() && session.getExpiresAt().isAfter(OffsetDateTime.now());
    }

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

    @Transactional(readOnly = true)
    public boolean detectSuspiciousActivity(String sessionToken, String currentIp, String currentUserAgent) {
        Optional<Session> optionalSession = sessionRepository.findBySessionToken(sessionToken);

        if (optionalSession.isEmpty()) {
            return false;
        }

        Session session = optionalSession.get();

        boolean ipChanged = !session.getIpAddress().equals(currentIp);

        boolean userAgentChanged = !session.getUserAgent().equals(currentUserAgent);

        return ipChanged || userAgentChanged;
    }

    public void markSessionSuspicious(UUID sessionId, int riskScore) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with ID: " + sessionId));

        session.setIsSuspicious(true);
        session.setRiskScore(riskScore);
        sessionRepository.save(session);

        log.warn("Session marked as suspicious: {} - Risk score: {}", sessionId, riskScore);
    }


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

    @Transactional(readOnly = true)
    public List<SessionDto> getActiveSessions(String token) {
        throw new UnsupportedOperationException("getActiveSessions not yet implemented - requires JWT integration");
    }

    public void terminateSessionWithAuth(UUID sessionId, String token) {
        terminateSession(sessionId, "Terminated by user");
    }

    public void terminateAllOtherSessions(String token) {
        throw new UnsupportedOperationException("terminateAllOtherSessions not yet implemented - requires JWT integration");
    }
}
