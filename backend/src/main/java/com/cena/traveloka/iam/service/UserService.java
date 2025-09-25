package com.cena.traveloka.iam.service;

import com.cena.traveloka.iam.entity.AppUser;
import com.cena.traveloka.iam.enums.UserStatus;
import com.cena.traveloka.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service class for AppUser entity operations
 *
 * Provides comprehensive user management functionality including:
 * - CRUD operations with validation
 * - Keycloak integration and synchronization
 * - User status lifecycle management
 * - Profile completeness tracking
 * - Cache management for performance
 * - Analytics and reporting
 *
 * Key Features:
 * - Transactional operations for data consistency
 * - Redis caching for frequently accessed users
 * - Keycloak ID mapping for OAuth2 integration
 * - Business logic for user lifecycle
 * - Audit logging for compliance
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserCacheService userCacheService;

    // === Core CRUD Operations ===

    /**
     * Create a new user with validation
     *
     * @param user User entity to create
     * @return Created user with generated ID
     * @throws IllegalArgumentException if user data is invalid
     */
    public AppUser createUser(AppUser user) {
        log.info("Creating new user with email: {}", user.getEmail());

        validateUser(user);
        validateUniqueConstraints(user);

        // Set default values
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.ACTIVE);
        }

        // Calculate profile completeness
        user.updateProfileCompleteness();
        user.setCreatedBy("SYSTEM");
        user.setUpdatedBy("SYSTEM");

        AppUser savedUser = userRepository.save(user);

        // Cache the newly created user
        userCacheService.cacheUser(savedUser);

        log.info("Successfully created user with ID: {}", savedUser.getId());

        return savedUser;
    }

    /**
     * Find user by ID with caching
     *
     * @param userId User ID
     * @return User if found
     */
    @Transactional(readOnly = true)
    public Optional<AppUser> findById(String userId) {
        log.debug("Finding user by ID: {}", userId);

        // Try cache first
        Optional<AppUser> cachedUser = userCacheService.getCachedUser(userId);
        if (cachedUser.isPresent()) {
            return cachedUser;
        }

        // Fallback to database
        Optional<AppUser> user = userRepository.findById(userId);
        user.ifPresent(userCacheService::cacheUser);

        return user;
    }

    /**
     * Find user by email
     *
     * @param email User email
     * @return User if found
     */
    @Cacheable(value = "users", key = "'email:' + #email")
    @Transactional(readOnly = true)
    public Optional<AppUser> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Find user by Keycloak ID for OAuth2 integration
     *
     * @param keycloakId Keycloak user ID
     * @return User if found
     */
    @Cacheable(value = "users", key = "'keycloak:' + #keycloakId")
    @Transactional(readOnly = true)
    public Optional<AppUser> findByKeycloakId(String keycloakId) {
        log.debug("Finding user by Keycloak ID: {}", keycloakId);
        return userRepository.findByKeycloakId(keycloakId);
    }

    /**
     * Update user with cache eviction
     *
     * @param user User to update
     * @return Updated user
     */
    public AppUser updateUser(AppUser user) {
        log.info("Updating user with ID: {}", user.getId());

        validateUser(user);

        // Update profile completeness
        user.updateProfileCompleteness();
        user.setUpdatedBy("SYSTEM");

        AppUser updatedUser = userRepository.save(user);

        // Update cache
        userCacheService.cacheUser(updatedUser);

        log.info("Successfully updated user with ID: {}", updatedUser.getId());

        return updatedUser;
    }

    /**
     * Soft delete user by changing status
     *
     * @param userId User ID to delete
     * @param deletedBy Who performed the deletion
     */
    public void deleteUser(String userId, String deletedBy) {
        log.info("Soft deleting user with ID: {}", userId);

        AppUser user = findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.softDelete();
        user.setUpdatedBy(deletedBy);
        userRepository.save(user);

        // Evict from cache
        userCacheService.evictUser(userId);

        log.info("Successfully soft deleted user with ID: {}", userId);
    }

    // === User Status Management ===

    /**
     * Activate user account
     *
     * @param userId User ID to activate
     * @param activatedBy Who performed the activation
     */
    @CacheEvict(value = "users", key = "#userId")
    public void activateUser(String userId, String activatedBy) {
        log.info("Activating user with ID: {}", userId);

        AppUser user = findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.activate();
        user.setUpdatedBy(activatedBy);
        userRepository.save(user);

        log.info("Successfully activated user with ID: {}", userId);
    }

    /**
     * Suspend user account
     *
     * @param userId User ID to suspend
     * @param suspendedBy Who performed the suspension
     */
    @CacheEvict(value = "users", key = "#userId")
    public void suspendUser(String userId, String suspendedBy) {
        log.info("Suspending user with ID: {}", userId);

        AppUser user = findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.suspend();
        user.setUpdatedBy(suspendedBy);
        userRepository.save(user);

        log.info("Successfully suspended user with ID: {}", userId);
    }

    /**
     * Update user status in bulk
     *
     * @param userIds List of user IDs
     * @param status New status
     * @param updatedBy Who performed the update
     * @return Number of users updated
     */
    @CacheEvict(value = "users", allEntries = true)
    public int updateUserStatus(List<String> userIds, UserStatus status, String updatedBy) {
        log.info("Bulk updating status for {} users to: {}", userIds.size(), status);

        int updatedCount = userRepository.updateStatusForUsers(userIds, status);
        log.info("Successfully updated status for {} users", updatedCount);

        return updatedCount;
    }

    // === Keycloak Integration ===

    /**
     * Link user with Keycloak ID
     *
     * @param userId User ID
     * @param keycloakId Keycloak user ID
     * @param syncedBy Who performed the sync
     */
    @CacheEvict(value = "users", key = "#userId")
    public void linkKeycloakId(String userId, String keycloakId, String syncedBy) {
        log.info("Linking user {} with Keycloak ID: {}", userId, keycloakId);

        AppUser user = findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Validate Keycloak ID is not already used
        if (userRepository.existsByKeycloakId(keycloakId)) {
            throw new IllegalArgumentException("Keycloak ID already linked to another user: " + keycloakId);
        }

        user.setKeycloakId(keycloakId);
        user.markSynced();
        user.setUpdatedBy(syncedBy);
        user.updateProfileCompleteness();

        userRepository.save(user);
        log.info("Successfully linked user {} with Keycloak ID: {}", userId, keycloakId);
    }

    /**
     * Mark user as synced with Keycloak
     *
     * @param keycloakIds List of Keycloak IDs to mark as synced
     * @param syncedBy Who performed the sync
     * @return Number of users updated
     */
    @CacheEvict(value = "users", allEntries = true)
    public int markUsersSynced(List<String> keycloakIds, String syncedBy) {
        log.info("Marking {} users as synced with Keycloak", keycloakIds.size());

        int updatedCount = userRepository.updateLastSyncForKeycloakUsers(keycloakIds, Instant.now());
        log.info("Successfully marked {} users as synced", updatedCount);

        return updatedCount;
    }

    /**
     * Find users that need Keycloak synchronization
     *
     * @param thresholdHours Hours since last sync to consider stale
     * @return List of users needing sync
     */
    @Transactional(readOnly = true)
    public List<AppUser> findUsersNeedingSync(int thresholdHours) {
        log.debug("Finding users needing Keycloak sync (threshold: {} hours)", thresholdHours);

        Instant threshold = Instant.now().minusSeconds(thresholdHours * 3600L);
        return userRepository.findUsersNeedingSync(threshold);
    }

    // === Search and Query Operations ===

    /**
     * Find all active users
     *
     * @return List of active users
     */
    @Transactional(readOnly = true)
    public List<AppUser> findActiveUsers() {
        log.debug("Finding all active users");
        return userRepository.findActiveUsers();
    }

    /**
     * Find users by status with pagination
     *
     * @param status User status
     * @param pageable Pagination parameters
     * @return Page of users
     */
    @Transactional(readOnly = true)
    public Page<AppUser> findUsersByStatus(UserStatus status, Pageable pageable) {
        log.debug("Finding users by status: {} with pagination", status);
        return userRepository.findByStatus(status, pageable);
    }

    /**
     * Search users by name pattern
     *
     * @param name Name pattern to search
     * @param pageable Pagination parameters
     * @return Page of matching users
     */
    @Transactional(readOnly = true)
    public Page<AppUser> searchUsersByName(String name, Pageable pageable) {
        log.debug("Searching users by name pattern: {}", name);
        return userRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    /**
     * Search users by email pattern
     *
     * @param emailPattern Email pattern to search
     * @param pageable Pagination parameters
     * @return Page of matching users
     */
    @Transactional(readOnly = true)
    public Page<AppUser> searchUsersByEmail(String emailPattern, Pageable pageable) {
        log.debug("Searching users by email pattern: {}", emailPattern);
        return userRepository.findByEmailContainingIgnoreCase(emailPattern, pageable);
    }

    /**
     * Advanced user search with multiple criteria
     *
     * @param email Email filter (optional)
     * @param firstName First name filter (optional)
     * @param lastName Last name filter (optional)
     * @param status Status filter (optional)
     * @param minCompleteness Minimum profile completeness (optional)
     * @param pageable Pagination parameters
     * @return Page of matching users
     */
    @Transactional(readOnly = true)
    public Page<AppUser> searchUsers(String email, String firstName, String lastName,
                                   UserStatus status, Integer minCompleteness, Pageable pageable) {
        log.debug("Advanced user search with filters - email: {}, firstName: {}, lastName: {}, status: {}, minCompleteness: {}",
                 email, firstName, lastName, status, minCompleteness);
        return userRepository.searchUsers(email, firstName, lastName, status, minCompleteness, pageable);
    }

    // === Profile Management ===

    /**
     * Find users with incomplete profiles
     *
     * @param threshold Profile completeness threshold (0-100)
     * @return List of users with incomplete profiles
     */
    @Transactional(readOnly = true)
    public List<AppUser> findUsersWithIncompleteProfiles(int threshold) {
        log.debug("Finding users with profile completeness below: {}%", threshold);
        return userRepository.findUsersWithIncompleteProfiles(threshold);
    }

    /**
     * Update profile completeness for multiple users
     *
     * @param userIds List of user IDs
     * @param completeness New completeness percentage
     * @param updatedBy Who performed the update
     * @return Number of users updated
     */
    @CacheEvict(value = "users", allEntries = true)
    public int updateProfileCompleteness(List<String> userIds, int completeness, String updatedBy) {
        log.info("Updating profile completeness for {} users to: {}%", userIds.size(), completeness);

        if (completeness < 0 || completeness > 100) {
            throw new IllegalArgumentException("Profile completeness must be between 0 and 100");
        }

        int updatedCount = userRepository.updateProfileCompletenessForUsers(userIds, completeness);
        log.info("Successfully updated profile completeness for {} users", updatedCount);

        return updatedCount;
    }

    // === Analytics and Reporting ===

    /**
     * Get user statistics for dashboard
     *
     * @return User statistics
     */
    @Transactional(readOnly = true)
    public Object getUserStatistics() {
        log.debug("Retrieving user statistics");

        // Try cache first
        Optional<Object> cachedStats = userCacheService.getCachedUserStatistics();
        if (cachedStats.isPresent()) {
            return cachedStats.get();
        }

        // Fallback to database
        Object stats = userRepository.getUserStatistics();
        userCacheService.cacheUserStatistics(stats);

        return stats;
    }

    /**
     * Count users by status
     *
     * @param status User status
     * @return Number of users with the status
     */
    @Transactional(readOnly = true)
    public long countUsersByStatus(UserStatus status) {
        log.debug("Counting users by status: {}", status);
        return userRepository.countByStatus(status);
    }

    /**
     * Find recently created users
     *
     * @param hours Number of hours to look back
     * @return List of recently created users
     */
    @Transactional(readOnly = true)
    public List<AppUser> findRecentlyCreatedUsers(int hours) {
        log.debug("Finding users created in the last {} hours", hours);

        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return userRepository.findRecentlyCreatedUsers(since);
    }

    // === Validation and Business Logic ===

    /**
     * Validate user data
     *
     * @param user User to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateUser(AppUser user) {
        if (!StringUtils.hasText(user.getEmail())) {
            throw new IllegalArgumentException("Email is required");
        }

        if (!user.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (user.getPhoneNumber() != null && !user.getPhoneNumber().matches("^\\+?[1-9]\\d{1,14}$")) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        if (user.getProfileCompleteness() != null &&
            (user.getProfileCompleteness() < 0 || user.getProfileCompleteness() > 100)) {
            throw new IllegalArgumentException("Profile completeness must be between 0 and 100");
        }
    }

    /**
     * Validate unique constraints
     *
     * @param user User to validate
     * @throws IllegalArgumentException if constraint violation
     */
    private void validateUniqueConstraints(AppUser user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }

        if (user.getKeycloakId() != null && userRepository.existsByKeycloakId(user.getKeycloakId())) {
            throw new IllegalArgumentException("Keycloak ID already exists: " + user.getKeycloakId());
        }
    }

    /**
     * Check if user exists by email
     *
     * @param email Email to check
     * @return true if user exists
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Check if user exists by Keycloak ID
     *
     * @param keycloakId Keycloak ID to check
     * @return true if user exists
     */
    @Transactional(readOnly = true)
    public boolean existsByKeycloakId(String keycloakId) {
        return userRepository.existsByKeycloakId(keycloakId);
    }

    /**
     * Refresh user context and cache
     *
     * @param userId User ID to refresh
     * @return Refreshed user
     */
    public AppUser refreshUserContext(String userId) {
        log.debug("Refreshing user context for: {}", userId);

        // Evict from cache to force fresh data
        userCacheService.evictAllUserData(userId);

        // Get fresh data from database
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Cache the fresh data
        userCacheService.cacheUser(user);

        return user;
    }
}
