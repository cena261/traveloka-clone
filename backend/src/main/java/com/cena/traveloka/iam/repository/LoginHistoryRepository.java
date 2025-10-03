package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.LoginHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * T023: LoginHistoryRepository interface
 * Repository for LoginHistory entity for audit logging (FR-007, FR-015).
 * Supports account lockout detection (FR-008).
 *
 * Constitutional Compliance:
 * - Principle III: Layered Architecture - Repository layer for data access
 * - Principle IV: Entity Immutability - LoginHistory entity is READ-ONLY
 * - Principle VII: Test-First Development - Implementation follows LoginHistoryRepositoryTest
 */
@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    /**
     * Find all login history for a user.
     * @param userId User ID
     * @return List of login history entries
     */
    List<LoginHistory> findByUserId(UUID userId);

    /**
     * Find login attempts by success status.
     * @param success true for successful logins, false for failed
     * @return List of login attempts
     */
    List<LoginHistory> findBySuccess(boolean success);

    /**
     * Find failed login attempts for a user.
     * Used for account lockout detection (FR-008).
     * @param userId User ID
     * @return List of failed login attempts
     */
    List<LoginHistory> findByUserIdAndSuccessFalse(UUID userId);

    /**
     * Count recent failed login attempts for a user.
     * Used for FR-008: 5 failed attempts → 30 minutes lockout.
     * @param userId User ID
     * @param after Timestamp to count from (e.g., 30 minutes ago)
     * @return Count of recent failed attempts
     */
    long countByUserIdAndSuccessFalseAndAttemptedAtAfter(UUID userId, OffsetDateTime after);

    /**
     * Find login history by email address.
     * @param email Email address
     * @return List of login attempts for the email
     */
    List<LoginHistory> findByEmail(String email);

    /**
     * Find login history by username.
     * @param username Username
     * @return List of login attempts for the username
     */
    List<LoginHistory> findByUsername(String username);

    /**
     * Find login history by IP address.
     * @param ipAddress IP address
     * @return List of login attempts from the IP
     */
    List<LoginHistory> findByIpAddress(String ipAddress);

    /**
     * Find all suspicious login attempts.
     * @return List of suspicious login attempts
     */
    List<LoginHistory> findByIsSuspiciousTrue();

    /**
     * Find login history by OAuth provider.
     * @param provider Provider name (e.g., google, facebook, local)
     * @return List of logins from the specified provider
     */
    List<LoginHistory> findByProvider(String provider);

    /**
     * Find login history within date range.
     * @param start Start timestamp
     * @param end End timestamp
     * @return List of login attempts within the range
     */
    List<LoginHistory> findByAttemptedAtBetween(OffsetDateTime start, OffsetDateTime end);

    /**
     * Find recent login history for a user, ordered by date descending.
     * @param userId User ID
     * @param pageable Pagination parameters
     * @return List of recent login attempts
     */
    List<LoginHistory> findByUserIdOrderByAttemptedAtDesc(UUID userId, Pageable pageable);

    /**
     * Count total login attempts for a user.
     * @param userId User ID
     * @return Total login attempt count
     */
    long countByUserId(UUID userId);

    /**
     * Find logins that required 2FA.
     * @return List of logins requiring 2FA
     */
    List<LoginHistory> findByRequired2faTrue();
}
