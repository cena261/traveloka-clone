package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * T024: PasswordResetTokenRepository interface
 * Repository for PasswordResetToken entity for password reset flow (FR-009).
 *
 * Constitutional Compliance:
 * - Principle III: Layered Architecture - Repository layer for data access
 * - Principle IV: Entity Immutability - PasswordResetToken entity is READ-ONLY
 * - Principle VII: Test-First Development - Implementation follows PasswordResetTokenRepositoryTest
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Find password reset token by token string.
     * @param token Token string
     * @return Optional containing token if found
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Find valid unused password reset token.
     * @param token Token string
     * @param now Current timestamp
     * @return Optional containing valid token if found
     */
    Optional<PasswordResetToken> findByTokenAndUsedFalseAndExpiresAtAfter(String token, OffsetDateTime now);

    /**
     * Find all password reset tokens for a user.
     * @param userId User ID
     * @return List of password reset tokens
     */
    List<PasswordResetToken> findByUserId(UUID userId);

    /**
     * Find unused password reset tokens for a user.
     * @param userId User ID
     * @return List of unused tokens
     */
    List<PasswordResetToken> findByUserIdAndUsedFalse(UUID userId);

    /**
     * Delete expired password reset tokens (cleanup).
     * @param now Current timestamp
     */
    void deleteByExpiresAtBefore(OffsetDateTime now);

    /**
     * Find expired password reset tokens for cleanup job.
     * @param now Current timestamp
     * @return List of expired tokens
     */
    List<PasswordResetToken> findByExpiresAtBefore(OffsetDateTime now);

    /**
     * Count unused password reset tokens for a user.
     * @param userId User ID
     * @return Count of unused tokens
     */
    long countByUserIdAndUsedFalse(UUID userId);

    /**
     * Delete all password reset tokens for a user.
     * @param userId User ID
     */
    void deleteByUserId(UUID userId);
}
