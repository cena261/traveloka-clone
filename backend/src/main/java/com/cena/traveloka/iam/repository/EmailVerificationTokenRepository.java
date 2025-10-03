package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * T025: EmailVerificationTokenRepository interface
 * Repository for EmailVerificationToken entity for email verification flow (FR-010).
 *
 * Constitutional Compliance:
 * - Principle III: Layered Architecture - Repository layer for data access
 * - Principle IV: Entity Immutability - EmailVerificationToken entity is READ-ONLY
 * - Principle VII: Test-First Development - Implementation follows EmailVerificationTokenRepositoryTest
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    /**
     * Find email verification token by token string.
     * @param token Token string
     * @return Optional containing token if found
     */
    Optional<EmailVerificationToken> findByToken(String token);

    /**
     * Find valid unverified email verification token.
     * @param token Token string
     * @param now Current timestamp
     * @return Optional containing valid token if found
     */
    Optional<EmailVerificationToken> findByTokenAndVerifiedFalseAndExpiresAtAfter(String token, OffsetDateTime now);

    /**
     * Find all email verification tokens for a user.
     * @param userId User ID
     * @return List of email verification tokens
     */
    List<EmailVerificationToken> findByUserId(UUID userId);

    /**
     * Find unverified email verification tokens for a user.
     * @param userId User ID
     * @return List of unverified tokens
     */
    List<EmailVerificationToken> findByUserIdAndVerifiedFalse(UUID userId);

    /**
     * Find email verification tokens by email address.
     * @param email Email address
     * @return List of tokens for the email
     */
    List<EmailVerificationToken> findByEmail(String email);

    /**
     * Find latest unverified token for a user.
     * @param userId User ID
     * @return Optional containing latest unverified token
     */
    Optional<EmailVerificationToken> findFirstByUserIdAndVerifiedFalseOrderByCreatedAtDesc(UUID userId);

    /**
     * Delete expired email verification tokens (cleanup).
     * @param now Current timestamp
     */
    void deleteByExpiresAtBefore(OffsetDateTime now);

    /**
     * Find expired email verification tokens for cleanup job.
     * @param now Current timestamp
     * @return List of expired tokens
     */
    List<EmailVerificationToken> findByExpiresAtBefore(OffsetDateTime now);

    /**
     * Count unverified email verification tokens for a user.
     * @param userId User ID
     * @return Count of unverified tokens
     */
    long countByUserIdAndVerifiedFalse(UUID userId);

    /**
     * Delete all email verification tokens for a user.
     * @param userId User ID
     */
    void deleteByUserId(UUID userId);

    /**
     * Check if user has pending email verification.
     * @param userId User ID
     * @return true if user has unverified tokens, false otherwise
     */
    boolean existsByUserIdAndVerifiedFalse(UUID userId);

    /**
     * Delete unverified email verification tokens for a user.
     * Used when generating new verification token.
     * @param userId User ID
     */
    void deleteByUserIdAndVerifiedFalse(UUID userId);
}
