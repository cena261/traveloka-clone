package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * T022: SessionRepository interface
 * Repository for Session entity with session management queries.
 * Supports FR-016: 5 concurrent session limit with oldest session eviction.
 *
 * Constitutional Compliance:
 * - Principle III: Layered Architecture - Repository layer for data access
 * - Principle IV: Entity Immutability - Session entity is READ-ONLY
 * - Principle VII: Test-First Development - Implementation follows SessionRepositoryTest
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    /**
     * Find session by session token.
     * @param sessionToken Session token string
     * @return Optional containing session if found
     */
    Optional<Session> findBySessionToken(String sessionToken);

    /**
     * Find all active sessions for a user.
     * Used for session listing (FR-013).
     * @param userId User ID
     * @return List of active sessions
     */
    List<Session> findByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Count active sessions for a user.
     * Used for enforcing 5 concurrent session limit (FR-016).
     * @param userId User ID
     * @return Count of active sessions
     */
    long countByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Find oldest active session for a user.
     * Used for session eviction when limit exceeded (FR-016).
     * @param userId User ID
     * @return Optional containing oldest active session
     */
    Optional<Session> findFirstByUserIdAndIsActiveTrueOrderByCreatedAtAsc(UUID userId);

    /**
     * Find expired active sessions for cleanup.
     * @param now Current timestamp
     * @return List of expired sessions
     */
    List<Session> findByIsActiveTrueAndExpiresAtBefore(OffsetDateTime now);

    /**
     * Find all suspicious sessions.
     * @return List of suspicious sessions
     */
    List<Session> findByIsSuspiciousTrue();

    /**
     * Find sessions requiring 2FA that haven't completed it.
     * @return List of sessions pending 2FA completion
     */
    List<Session> findByRequires2faTrueAndTwoFaCompletedFalse();

    /**
     * Find sessions by IP address.
     * Used for session hijacking detection.
     * @param ipAddress IP address
     * @return List of sessions from the specified IP
     */
    List<Session> findByIpAddress(String ipAddress);

    /**
     * Delete all sessions for a user.
     * @param userId User ID
     */
    void deleteByUserId(UUID userId);

    /**
     * Find sessions by device ID.
     * @param deviceId Device identifier
     * @return List of sessions from the specified device
     */
    List<Session> findByDeviceId(String deviceId);
}
