package com.cena.traveloka.iam.service;

import com.cena.traveloka.iam.entity.AppUser;
import com.cena.traveloka.iam.entity.UserSession;
import com.cena.traveloka.iam.repository.UserRepository;
import com.cena.traveloka.iam.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for warming up Redis caches with active user and session data
 *
 * Provides intelligent cache warming strategies including:
 * - Application startup warmup for most active users
 * - Periodic warmup of recently active users and sessions
 * - Selective warmup based on user activity patterns
 * - Performance-optimized batch processing
 *
 * Key Features:
 * - Async processing to avoid blocking application startup
 * - Configurable warmup limits and schedules
 * - Activity-based prioritization
 * - Memory-efficient batch processing
 * - Automatic cleanup of stale cache entries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserCacheService userCacheService;
    private final SessionCacheService sessionCacheService;

    @Value("${app.cache.warmup.max-users:1000}")
    private int maxUsersToWarmup;

    @Value("${app.cache.warmup.max-sessions:500}")
    private int maxSessionsToWarmup;

    @Value("${app.cache.warmup.recent-activity-hours:24}")
    private int recentActivityHours;

    @Value("${app.cache.warmup.enabled:true}")
    private boolean warmupEnabled;

    @Value("${app.cache.warmup.startup-enabled:true}")
    private boolean startupWarmupEnabled;

    /**
     * Warm up cache on application startup
     * Triggered when application is fully ready
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmupCacheOnStartup() {
        if (!warmupEnabled || !startupWarmupEnabled) {
            log.info("Cache warmup is disabled, skipping startup warmup");
            return;
        }

        log.info("Starting cache warmup on application startup");

        try {
            CompletableFuture<Void> userWarmup = warmupActiveUsers();
            CompletableFuture<Void> sessionWarmup = warmupActiveSessions();
            CompletableFuture<Void> statsWarmup = warmupStatistics();

            // Wait for all warmup tasks to complete
            CompletableFuture.allOf(userWarmup, sessionWarmup, statsWarmup)
                    .get(); // Wait for completion

            log.info("Cache warmup completed successfully on startup");

        } catch (Exception e) {
            log.error("Error during startup cache warmup", e);
        }
    }

    /**
     * Scheduled cache warmup - runs every 30 minutes
     * Refreshes cache with recently active users and sessions
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    @Async
    public void scheduledCacheWarmup() {
        if (!warmupEnabled) {
            return;
        }

        log.debug("Starting scheduled cache warmup");

        try {
            // Warmup recently active users and sessions
            CompletableFuture<Void> userWarmup = warmupRecentlyActiveUsers();
            CompletableFuture<Void> sessionWarmup = warmupRecentlyActiveSessions();

            CompletableFuture.allOf(userWarmup, sessionWarmup).get();

            log.debug("Scheduled cache warmup completed successfully");

        } catch (Exception e) {
            log.error("Error during scheduled cache warmup", e);
        }
    }

    /**
     * Warm up cache with most active users
     * Prioritizes users with recent activity and complete profiles
     */
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Void> warmupActiveUsers() {
        return CompletableFuture.runAsync(() -> {
            log.info("Starting active users cache warmup (max: {})", maxUsersToWarmup);

            try {
                // Get most active users with recent activity
                List<AppUser> activeUsers = userRepository.findMostActiveUsers(maxUsersToWarmup);

                if (activeUsers.isEmpty()) {
                    log.info("No active users found for cache warmup");
                    return;
                }

                // Batch warmup to avoid overwhelming the cache
                int batchSize = 50;
                int totalBatches = (int) Math.ceil((double) activeUsers.size() / batchSize);

                for (int i = 0; i < totalBatches; i++) {
                    int start = i * batchSize;
                    int end = Math.min(start + batchSize, activeUsers.size());
                    List<AppUser> batch = activeUsers.subList(start, end);

                    // Warm up cache for this batch
                    userCacheService.warmUpUserCache(batch);

                    log.debug("Warmed up batch {}/{} with {} users", i + 1, totalBatches, batch.size());

                    // Small delay between batches to avoid overwhelming the system
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                log.info("Successfully warmed up cache with {} active users", activeUsers.size());

            } catch (Exception e) {
                log.error("Error warming up active users cache", e);
            }
        });
    }

    /**
     * Warm up cache with recently active users
     * Used for scheduled warmup to refresh frequently accessed data
     */
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Void> warmupRecentlyActiveUsers() {
        return CompletableFuture.runAsync(() -> {
            log.debug("Starting recently active users cache warmup");

            try {
                Instant since = Instant.now().minusSeconds(recentActivityHours * 3600L);
                List<AppUser> recentUsers = userRepository.findRecentlyActiveUsers(since, maxUsersToWarmup);

                if (!recentUsers.isEmpty()) {
                    userCacheService.warmUpUserCache(recentUsers);
                    log.debug("Warmed up cache with {} recently active users", recentUsers.size());
                } else {
                    log.debug("No recently active users found for cache warmup");
                }

            } catch (Exception e) {
                log.error("Error warming up recently active users cache", e);
            }
        });
    }

    /**
     * Warm up cache with active sessions
     * Prioritizes sessions with recent activity
     */
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Void> warmupActiveSessions() {
        return CompletableFuture.runAsync(() -> {
            log.info("Starting active sessions cache warmup (max: {})", maxSessionsToWarmup);

            try {
                // Get most recently active sessions
                Instant since = Instant.now().minusSeconds(recentActivityHours * 3600L);
                List<UserSession> activeSessions = userSessionRepository.findRecentlyActiveSessions(since);

                if (activeSessions.isEmpty()) {
                    log.info("No active sessions found for cache warmup");
                    return;
                }

                // Limit to configured maximum
                List<UserSession> sessionsToWarmup = activeSessions.size() > maxSessionsToWarmup ?
                        activeSessions.subList(0, maxSessionsToWarmup) : activeSessions;

                // Warm up session cache
                sessionCacheService.warmUpSessionCache(sessionsToWarmup);

                log.info("Successfully warmed up cache with {} active sessions", sessionsToWarmup.size());

            } catch (Exception e) {
                log.error("Error warming up active sessions cache", e);
            }
        });
    }

    /**
     * Warm up cache with recently active sessions
     * Used for scheduled warmup
     */
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Void> warmupRecentlyActiveSessions() {
        return CompletableFuture.runAsync(() -> {
            log.debug("Starting recently active sessions cache warmup");

            try {
                Instant since = Instant.now().minusSeconds((recentActivityHours / 2) * 3600L); // Last 12 hours for scheduled warmup
                List<UserSession> recentSessions = userSessionRepository.findRecentlyActiveSessions(since);

                if (!recentSessions.isEmpty()) {
                    // Limit to half of max for scheduled warmup
                    List<UserSession> sessionsToWarmup = recentSessions.size() > (maxSessionsToWarmup / 2) ?
                            recentSessions.subList(0, maxSessionsToWarmup / 2) : recentSessions;

                    sessionCacheService.warmUpSessionCache(sessionsToWarmup);
                    log.debug("Warmed up cache with {} recently active sessions", sessionsToWarmup.size());
                } else {
                    log.debug("No recently active sessions found for cache warmup");
                }

            } catch (Exception e) {
                log.error("Error warming up recently active sessions cache", e);
            }
        });
    }

    /**
     * Warm up statistics caches
     * Pre-loads frequently accessed statistics
     */
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Void> warmupStatistics() {
        return CompletableFuture.runAsync(() -> {
            log.debug("Starting statistics cache warmup");

            try {
                // Pre-load user statistics
                Object userStats = userRepository.getUserStatistics();
                userCacheService.cacheUserStatistics(userStats);

                // Pre-load session statistics
                Object sessionStats = userSessionRepository.getSessionStatistics();
                sessionCacheService.cacheSessionStatistics(sessionStats);

                log.debug("Successfully warmed up statistics caches");

            } catch (Exception e) {
                log.error("Error warming up statistics caches", e);
            }
        });
    }

    /**
     * Warm up cache for specific user and their related data
     *
     * @param userId User ID to warm up
     */
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Void> warmupUserData(String userId) {
        return CompletableFuture.runAsync(() -> {
            log.debug("Starting user data cache warmup for user: {}", userId);

            try {
                // Warm up user data
                userRepository.findById(userId).ifPresent(user -> {
                    userCacheService.cacheUser(user);
                    log.debug("Cached user data for: {}", userId);
                });

                // Warm up user's active sessions
                List<UserSession> userSessions = userSessionRepository.findActiveSessionsByUserId(userId);
                if (!userSessions.isEmpty()) {
                    sessionCacheService.warmUpSessionCache(userSessions);
                    log.debug("Cached {} active sessions for user: {}", userSessions.size(), userId);
                }

                // Cache session count
                long sessionCount = userSessionRepository.countActiveSessionsByUserId(userId);
                sessionCacheService.cacheUserSessionCount(userId, sessionCount);

                log.debug("Successfully warmed up cache for user: {}", userId);

            } catch (Exception e) {
                log.error("Error warming up cache for user: {}", userId, e);
            }
        });
    }

    /**
     * Clear and re-warm specific user's cache
     * Useful for when user data has been updated
     *
     * @param userId User ID to refresh
     */
    @Async
    public CompletableFuture<Void> refreshUserCache(String userId) {
        return CompletableFuture.runAsync(() -> {
            log.debug("Refreshing cache for user: {}", userId);

            try {
                // Clear existing cache
                userCacheService.evictAllUserData(userId);
                sessionCacheService.evictUserSessions(userId);

                // Re-warm with fresh data
                warmupUserData(userId).get();

                log.debug("Successfully refreshed cache for user: {}", userId);

            } catch (Exception e) {
                log.error("Error refreshing cache for user: {}", userId, e);
            }
        });
    }

    /**
     * Get cache warmup statistics
     *
     * @return Warmup statistics
     */
    @Transactional(readOnly = true)
    public CacheWarmupStats getCacheWarmupStats() {
        try {
            // Get active user count
            long activeUserCount = userRepository.countActiveUsers();

            // Get active session count
            long activeSessionCount = userSessionRepository.countActiveSessions();

            // Get recently active counts
            Instant since = Instant.now().minusSeconds(recentActivityHours * 3600L);
            long recentlyActiveUsers = userRepository.countRecentlyActiveUsers(since);
            long recentlyActiveSessions = userSessionRepository.countRecentlyActiveSessions(since);

            return new CacheWarmupStats(
                    activeUserCount,
                    activeSessionCount,
                    recentlyActiveUsers,
                    recentlyActiveSessions,
                    maxUsersToWarmup,
                    maxSessionsToWarmup,
                    warmupEnabled
            );

        } catch (Exception e) {
            log.error("Error getting cache warmup statistics", e);
            return new CacheWarmupStats(0L, 0L, 0L, 0L, maxUsersToWarmup, maxSessionsToWarmup, warmupEnabled);
        }
    }

    /**
     * Manual cache warmup trigger
     * Can be called via management endpoint or scheduled task
     */
    @Async
    public CompletableFuture<Void> performManualWarmup() {
        return CompletableFuture.runAsync(() -> {
            log.info("Starting manual cache warmup");

            try {
                CompletableFuture<Void> userWarmup = warmupRecentlyActiveUsers();
                CompletableFuture<Void> sessionWarmup = warmupRecentlyActiveSessions();
                CompletableFuture<Void> statsWarmup = warmupStatistics();

                CompletableFuture.allOf(userWarmup, sessionWarmup, statsWarmup).get();

                log.info("Manual cache warmup completed successfully");

            } catch (Exception e) {
                log.error("Error during manual cache warmup", e);
            }
        });
    }

    /**
     * Cache warmup statistics data class
     */
    public static class CacheWarmupStats {
        public final long activeUsers;
        public final long activeSessions;
        public final long recentlyActiveUsers;
        public final long recentlyActiveSessions;
        public final int maxUsersToWarmup;
        public final int maxSessionsToWarmup;
        public final boolean warmupEnabled;

        public CacheWarmupStats(long activeUsers, long activeSessions, long recentlyActiveUsers,
                              long recentlyActiveSessions, int maxUsersToWarmup, int maxSessionsToWarmup,
                              boolean warmupEnabled) {
            this.activeUsers = activeUsers;
            this.activeSessions = activeSessions;
            this.recentlyActiveUsers = recentlyActiveUsers;
            this.recentlyActiveSessions = recentlyActiveSessions;
            this.maxUsersToWarmup = maxUsersToWarmup;
            this.maxSessionsToWarmup = maxSessionsToWarmup;
            this.warmupEnabled = warmupEnabled;
        }

        @Override
        public String toString() {
            return String.format("CacheWarmupStats{activeUsers=%d, activeSessions=%d, recentlyActiveUsers=%d, " +
                            "recentlyActiveSessions=%d, maxUsersToWarmup=%d, maxSessionsToWarmup=%d, warmupEnabled=%s}",
                    activeUsers, activeSessions, recentlyActiveUsers, recentlyActiveSessions,
                    maxUsersToWarmup, maxSessionsToWarmup, warmupEnabled);
        }
    }
}