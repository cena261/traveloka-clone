package com.cena.traveloka.iam.repository;

import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * T018: UserRepository interface
 * Repository for User entity with custom query methods.
 *
 * Constitutional Compliance:
 * - Principle III: Layered Architecture - Repository layer for data access
 * - Principle IV: Entity Immutability - User entity is READ-ONLY, not modified
 * - Principle VII: Test-First Development - Implementation follows UserRepositoryTest
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email address.
     * @param email User's email address
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username.
     * @param username User's username
     * @return Optional containing user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by Keycloak ID.
     * @param keycloakId Keycloak user ID
     * @return Optional containing user if found
     */
    Optional<User> findByKeycloakId(UUID keycloakId);

    /**
     * Check if email already exists.
     * @param email Email to check
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Check if username already exists.
     * @param username Username to check
     * @return true if username exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Find all users with specific status.
     * @param status User status (PENDING, ACTIVE, INACTIVE, SUSPENDED)
     * @return List of users with the specified status
     */
    List<User> findByStatus(Status status);

    /**
     * Find all locked user accounts.
     * @return List of locked users
     */
    List<User> findByAccountLockedTrue();

    /**
     * Find users with failed login attempts greater than or equal to threshold.
     * Used for account lockout detection (FR-008: 5 failed attempts).
     * @param threshold Failed login attempts threshold
     * @return List of users with excessive failed login attempts
     */
    List<User> findByFailedLoginAttemptsGreaterThanEqual(int threshold);

    /**
     * Find users with specific status and whose lockout period has expired.
     * Used by AccountLockoutScheduler to automatically unlock accounts.
     * @param status User status
     * @param lockedUntil Timestamp to compare against
     * @return List of users whose lockout has expired
     */
    List<User> findByStatusAndLockedUntilBefore(Status status, java.time.OffsetDateTime lockedUntil);

    /**
     * Count users by status.
     * Used for monitoring and reporting.
     * @param status User status
     * @return Count of users with the specified status
     */
    long countByStatus(Status status);
}
