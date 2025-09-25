package com.cena.traveloka.iam.service;

import com.cena.traveloka.iam.entity.AppUser;
import com.cena.traveloka.iam.entity.UserProfile;
import com.cena.traveloka.iam.entity.UserPreference;
import com.cena.traveloka.iam.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String USER_CACHE_PREFIX = "user:";
    private static final String USER_PROFILE_CACHE_PREFIX = "user:profile:";
    private static final String USER_PREFERENCE_CACHE_PREFIX = "user:preference:";
    private static final String USER_ACTIVE_SET = "users:active";
    private static final String USER_STATS_KEY = "users:stats";

    private static final Duration USER_CACHE_TTL = Duration.ofHours(1);
    private static final Duration PROFILE_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration PREFERENCE_CACHE_TTL = Duration.ofMinutes(15);
    private static final Duration STATS_CACHE_TTL = Duration.ofMinutes(5);

    public void cacheUser(AppUser user) {
        if (user == null || user.getId() == null) {
            return;
        }

        try {
            String key = USER_CACHE_PREFIX + user.getId();
            redisTemplate.opsForValue().set(key, user, USER_CACHE_TTL);

            // Add to active users set if user is active
            if (user.getStatus() == UserStatus.ACTIVE) {
                redisTemplate.opsForSet().add(USER_ACTIVE_SET, user.getId());
                redisTemplate.expire(USER_ACTIVE_SET, Duration.ofHours(2));
            }

            log.debug("Cached user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to cache user {}: {}", user.getId(), e.getMessage());
        }
    }

    public Optional<AppUser> getCachedUser(String userId) {
        if (userId == null) {
            return Optional.empty();
        }

        try {
            String key = USER_CACHE_PREFIX + userId;
            AppUser user = (AppUser) redisTemplate.opsForValue().get(key);

            if (user != null) {
                log.debug("Cache hit for user: {}", userId);
                return Optional.of(user);
            }

            log.debug("Cache miss for user: {}", userId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get cached user {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    public void evictUser(String userId) {
        if (userId == null) {
            return;
        }

        try {
            String key = USER_CACHE_PREFIX + userId;
            redisTemplate.delete(key);
            redisTemplate.opsForSet().remove(USER_ACTIVE_SET, userId);

            log.debug("Evicted user from cache: {}", userId);
        } catch (Exception e) {
            log.error("Failed to evict user {}: {}", userId, e.getMessage());
        }
    }

    public void cacheUserProfile(UserProfile profile) {
        if (profile == null || profile.getUserId() == null) {
            return;
        }

        try {
            String key = USER_PROFILE_CACHE_PREFIX + profile.getUserId();
            redisTemplate.opsForValue().set(key, profile, PROFILE_CACHE_TTL);

            log.debug("Cached user profile: {}", profile.getUserId());
        } catch (Exception e) {
            log.error("Failed to cache user profile {}: {}", profile.getUserId(), e.getMessage());
        }
    }

    public Optional<UserProfile> getCachedUserProfile(String userId) {
        if (userId == null) {
            return Optional.empty();
        }

        try {
            String key = USER_PROFILE_CACHE_PREFIX + userId;
            UserProfile profile = (UserProfile) redisTemplate.opsForValue().get(key);

            if (profile != null) {
                log.debug("Cache hit for user profile: {}", userId);
                return Optional.of(profile);
            }

            log.debug("Cache miss for user profile: {}", userId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get cached user profile {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    public void evictUserProfile(String userId) {
        if (userId == null) {
            return;
        }

        try {
            String key = USER_PROFILE_CACHE_PREFIX + userId;
            redisTemplate.delete(key);

            log.debug("Evicted user profile from cache: {}", userId);
        } catch (Exception e) {
            log.error("Failed to evict user profile {}: {}", userId, e.getMessage());
        }
    }

    public void cacheUserPreference(UserPreference preference) {
        if (preference == null || preference.getUserId() == null) {
            return;
        }

        try {
            String key = USER_PREFERENCE_CACHE_PREFIX + preference.getUserId();
            redisTemplate.opsForValue().set(key, preference, PREFERENCE_CACHE_TTL);

            log.debug("Cached user preference: {}", preference.getUserId());
        } catch (Exception e) {
            log.error("Failed to cache user preference {}: {}", preference.getUserId(), e.getMessage());
        }
    }

    public Optional<UserPreference> getCachedUserPreference(String userId) {
        if (userId == null) {
            return Optional.empty();
        }

        try {
            String key = USER_PREFERENCE_CACHE_PREFIX + userId;
            UserPreference preference = (UserPreference) redisTemplate.opsForValue().get(key);

            if (preference != null) {
                log.debug("Cache hit for user preference: {}", userId);
                return Optional.of(preference);
            }

            log.debug("Cache miss for user preference: {}", userId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get cached user preference {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    public void evictUserPreference(String userId) {
        if (userId == null) {
            return;
        }

        try {
            String key = USER_PREFERENCE_CACHE_PREFIX + userId;
            redisTemplate.delete(key);

            log.debug("Evicted user preference from cache: {}", userId);
        } catch (Exception e) {
            log.error("Failed to evict user preference {}: {}", userId, e.getMessage());
        }
    }

    public void evictAllUserData(String userId) {
        if (userId == null) {
            return;
        }

        try {
            evictUser(userId);
            evictUserProfile(userId);
            evictUserPreference(userId);

            log.debug("Evicted all user data from cache: {}", userId);
        } catch (Exception e) {
            log.error("Failed to evict all user data {}: {}", userId, e.getMessage());
        }
    }

    public Set<String> getActiveUsers() {
        try {
            Set<Object> members = redisTemplate.opsForSet().members(USER_ACTIVE_SET);
            return members != null ?
                   members.stream().map(Object::toString).collect(java.util.stream.Collectors.toSet()) :
                   Set.of();
        } catch (Exception e) {
            log.error("Failed to get active users: {}", e.getMessage());
            return Set.of();
        }
    }

    public void cacheUserStatistics(Object statistics) {
        try {
            redisTemplate.opsForValue().set(USER_STATS_KEY, statistics, STATS_CACHE_TTL);
            log.debug("Cached user statistics");
        } catch (Exception e) {
            log.error("Failed to cache user statistics: {}", e.getMessage());
        }
    }

    public Optional<Object> getCachedUserStatistics() {
        try {
            Object stats = redisTemplate.opsForValue().get(USER_STATS_KEY);
            if (stats != null) {
                log.debug("Cache hit for user statistics");
                return Optional.of(stats);
            }

            log.debug("Cache miss for user statistics");
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get cached user statistics: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void evictUserStatistics() {
        try {
            redisTemplate.delete(USER_STATS_KEY);
            log.debug("Evicted user statistics from cache");
        } catch (Exception e) {
            log.error("Failed to evict user statistics: {}", e.getMessage());
        }
    }

    public void warmUpUserCache(List<AppUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        try {
            for (AppUser user : users) {
                cacheUser(user);
            }
            log.info("Warmed up cache with {} users", users.size());
        } catch (Exception e) {
            log.error("Failed to warm up user cache: {}", e.getMessage());
        }
    }

    public void clearAllUserCaches() {
        try {
            Set<String> userKeys = redisTemplate.keys(USER_CACHE_PREFIX + "*");
            Set<String> profileKeys = redisTemplate.keys(USER_PROFILE_CACHE_PREFIX + "*");
            Set<String> preferenceKeys = redisTemplate.keys(USER_PREFERENCE_CACHE_PREFIX + "*");

            if (userKeys != null && !userKeys.isEmpty()) {
                redisTemplate.delete(userKeys);
            }
            if (profileKeys != null && !profileKeys.isEmpty()) {
                redisTemplate.delete(profileKeys);
            }
            if (preferenceKeys != null && !preferenceKeys.isEmpty()) {
                redisTemplate.delete(preferenceKeys);
            }

            redisTemplate.delete(USER_ACTIVE_SET);
            redisTemplate.delete(USER_STATS_KEY);

            log.info("Cleared all user caches");
        } catch (Exception e) {
            log.error("Failed to clear all user caches: {}", e.getMessage());
        }
    }
}