package com.cena.traveloka.iam.service;

import com.cena.traveloka.iam.entity.UserSession;
import com.cena.traveloka.iam.enums.SessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_CACHE_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user:sessions:";
    private static final String ACTIVE_SESSIONS_SET = "sessions:active";
    private static final String SESSION_STATS_KEY = "sessions:stats";
    private static final String DEVICE_SESSIONS_PREFIX = "device:sessions:";

    private static final Duration SESSION_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration USER_SESSIONS_CACHE_TTL = Duration.ofMinutes(15);
    private static final Duration STATS_CACHE_TTL = Duration.ofMinutes(5);

    public void cacheSession(UserSession session) {
        if (session == null || session.getId() == null) {
            return;
        }

        try {
            String sessionKey = SESSION_CACHE_PREFIX + session.getId();
            redisTemplate.opsForValue().set(sessionKey, session, SESSION_CACHE_TTL);

            // Add to active sessions set if session is active
            if ("ACTIVE".equals(session.getStatus())) {
                redisTemplate.opsForSet().add(ACTIVE_SESSIONS_SET, session.getId());
                redisTemplate.expire(ACTIVE_SESSIONS_SET, Duration.ofHours(1));
            }

            // Cache user sessions mapping
            if (session.getUserId() != null) {
                String userSessionsKey = USER_SESSIONS_PREFIX + session.getUserId();
                redisTemplate.opsForSet().add(userSessionsKey, session.getId());
                redisTemplate.expire(userSessionsKey, USER_SESSIONS_CACHE_TTL);
            }

            // Cache device sessions mapping
            if (session.getSessionId() != null) {
                String deviceSessionsKey = DEVICE_SESSIONS_PREFIX + session.getSessionId();
                redisTemplate.opsForSet().add(deviceSessionsKey, session.getId());
                redisTemplate.expire(deviceSessionsKey, USER_SESSIONS_CACHE_TTL);
            }

            log.debug("Cached session: {}", session.getId());
        } catch (Exception e) {
            log.error("Failed to cache session {}: {}", session.getId(), e.getMessage());
        }
    }

    public Optional<UserSession> getCachedSession(String sessionId) {
        if (sessionId == null) {
            return Optional.empty();
        }

        try {
            String key = SESSION_CACHE_PREFIX + sessionId;
            UserSession session = (UserSession) redisTemplate.opsForValue().get(key);

            if (session != null) {
                log.debug("Cache hit for session: {}", sessionId);
                return Optional.of(session);
            }

            log.debug("Cache miss for session: {}", sessionId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get cached session {}: {}", sessionId, e.getMessage());
            return Optional.empty();
        }
    }

    public void evictSession(String sessionId) {
        if (sessionId == null) {
            return;
        }

        try {
            // Get session first to clean up related caches
            Optional<UserSession> sessionOpt = getCachedSession(sessionId);

            String sessionKey = SESSION_CACHE_PREFIX + sessionId;
            redisTemplate.delete(sessionKey);
            redisTemplate.opsForSet().remove(ACTIVE_SESSIONS_SET, sessionId);

            // Clean up user sessions mapping
            if (sessionOpt.isPresent() && sessionOpt.get().getUserId() != null) {
                String userSessionsKey = USER_SESSIONS_PREFIX + sessionOpt.get().getUserId();
                redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
            }

            // Clean up device sessions mapping
            if (sessionOpt.isPresent() && sessionOpt.get().getSessionId() != null) {
                String deviceSessionsKey = DEVICE_SESSIONS_PREFIX + sessionOpt.get().getSessionId();
                redisTemplate.opsForSet().remove(deviceSessionsKey, sessionId);
            }

            log.debug("Evicted session from cache: {}", sessionId);
        } catch (Exception e) {
            log.error("Failed to evict session {}: {}", sessionId, e.getMessage());
        }
    }

    public Set<String> getCachedUserSessions(String userId) {
        if (userId == null) {
            return Set.of();
        }

        try {
            String key = USER_SESSIONS_PREFIX + userId;
            Set<Object> members = redisTemplate.opsForSet().members(key);
            return members != null ?
                   members.stream().map(Object::toString).collect(java.util.stream.Collectors.toSet()) :
                   Set.of();
        } catch (Exception e) {
            log.error("Failed to get cached user sessions {}: {}", userId, e.getMessage());
            return Set.of();
        }
    }

    public Set<String> getCachedDeviceSessions(String deviceId) {
        if (deviceId == null) {
            return Set.of();
        }

        try {
            String key = DEVICE_SESSIONS_PREFIX + deviceId;
            Set<Object> members = redisTemplate.opsForSet().members(key);
            return members != null ?
                   members.stream().map(Object::toString).collect(java.util.stream.Collectors.toSet()) :
                   Set.of();
        } catch (Exception e) {
            log.error("Failed to get cached device sessions {}: {}", deviceId, e.getMessage());
            return Set.of();
        }
    }

    public void evictUserSessions(String userId) {
        if (userId == null) {
            return;
        }

        try {
            // Get all session IDs for the user
            Set<String> sessionIds = getCachedUserSessions(userId);

            // Evict each session
            for (String sessionId : sessionIds) {
                evictSession(sessionId);
            }

            // Clean up user sessions mapping
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            redisTemplate.delete(userSessionsKey);

            log.debug("Evicted all sessions for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to evict user sessions {}: {}", userId, e.getMessage());
        }
    }

    public void updateSessionLastAccess(String sessionId) {
        if (sessionId == null) {
            return;
        }

        try {
            Optional<UserSession> sessionOpt = getCachedSession(sessionId);
            if (sessionOpt.isPresent()) {
                UserSession session = sessionOpt.get();
                session.setLastActivityAt(java.time.OffsetDateTime.now());
                cacheSession(session);
                log.debug("Updated last access for session: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("Failed to update session last access {}: {}", sessionId, e.getMessage());
        }
    }

    public Set<String> getActiveSessions() {
        try {
            Set<Object> members = redisTemplate.opsForSet().members(ACTIVE_SESSIONS_SET);
            return members != null ?
                   members.stream().map(Object::toString).collect(java.util.stream.Collectors.toSet()) :
                   Set.of();
        } catch (Exception e) {
            log.error("Failed to get active sessions: {}", e.getMessage());
            return Set.of();
        }
    }

    public void cacheSessionStatistics(Object statistics) {
        try {
            redisTemplate.opsForValue().set(SESSION_STATS_KEY, statistics, STATS_CACHE_TTL);
            log.debug("Cached session statistics");
        } catch (Exception e) {
            log.error("Failed to cache session statistics: {}", e.getMessage());
        }
    }

    public Optional<Object> getCachedSessionStatistics() {
        try {
            Object stats = redisTemplate.opsForValue().get(SESSION_STATS_KEY);
            if (stats != null) {
                log.debug("Cache hit for session statistics");
                return Optional.of(stats);
            }

            log.debug("Cache miss for session statistics");
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get cached session statistics: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void evictSessionStatistics() {
        try {
            redisTemplate.delete(SESSION_STATS_KEY);
            log.debug("Evicted session statistics from cache");
        } catch (Exception e) {
            log.error("Failed to evict session statistics: {}", e.getMessage());
        }
    }

    public void warmUpSessionCache(List<UserSession> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            for (UserSession session : sessions) {
                cacheSession(session);
            }
            log.info("Warmed up session cache with {} sessions", sessions.size());
        } catch (Exception e) {
            log.error("Failed to warm up session cache: {}", e.getMessage());
        }
    }

    public void cacheUserSessionCount(String userId, long count) {
        if (userId == null) {
            return;
        }

        try {
            String key = "user:session:count:" + userId;
            redisTemplate.opsForValue().set(key, count, Duration.ofMinutes(10));
            log.debug("Cached session count for user {}: {}", userId, count);
        } catch (Exception e) {
            log.error("Failed to cache session count for user {}: {}", userId, e.getMessage());
        }
    }

    public Optional<Long> getCachedUserSessionCount(String userId) {
        if (userId == null) {
            return Optional.empty();
        }

        try {
            String key = "user:session:count:" + userId;
            Object count = redisTemplate.opsForValue().get(key);
            if (count != null) {
                return Optional.of(((Number) count).longValue());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get cached session count for user {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    public void clearAllSessionCaches() {
        try {
            Set<String> sessionKeys = redisTemplate.keys(SESSION_CACHE_PREFIX + "*");
            Set<String> userSessionKeys = redisTemplate.keys(USER_SESSIONS_PREFIX + "*");
            Set<String> deviceSessionKeys = redisTemplate.keys(DEVICE_SESSIONS_PREFIX + "*");
            Set<String> countKeys = redisTemplate.keys("user:session:count:*");

            if (sessionKeys != null && !sessionKeys.isEmpty()) {
                redisTemplate.delete(sessionKeys);
            }
            if (userSessionKeys != null && !userSessionKeys.isEmpty()) {
                redisTemplate.delete(userSessionKeys);
            }
            if (deviceSessionKeys != null && !deviceSessionKeys.isEmpty()) {
                redisTemplate.delete(deviceSessionKeys);
            }
            if (countKeys != null && !countKeys.isEmpty()) {
                redisTemplate.delete(countKeys);
            }

            redisTemplate.delete(ACTIVE_SESSIONS_SET);
            redisTemplate.delete(SESSION_STATS_KEY);

            log.info("Cleared all session caches");
        } catch (Exception e) {
            log.error("Failed to clear all session caches: {}", e.getMessage());
        }
    }
}