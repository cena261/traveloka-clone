package com.cena.traveloka.iam.service;

import com.cena.traveloka.iam.entity.EmailVerificationToken;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.repository.EmailVerificationTokenRepository;
import com.cena.traveloka.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * T057: EmailVerificationService
 * Service for email verification operations.
 *
 * Constitutional Compliance:
 * - FR-010: Email verification required before booking
 * - Principle III: Layered Architecture - Business logic in service layer
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    private static final int TOKEN_EXPIRY_HOURS = 24;

    /**
     * Create email verification token for user (FR-010).
     *
     * @param userId User ID
     * @return EmailVerificationToken
     */
    public EmailVerificationToken createVerificationToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Delete old unverified tokens
        tokenRepository.deleteByUserIdAndVerifiedFalse(userId);

        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .email(user.getEmail())
                .token(UUID.randomUUID().toString())
                .expiresAt(OffsetDateTime.now().plusHours(TOKEN_EXPIRY_HOURS))
                .verified(false)
                .attempts(0)
                .createdAt(OffsetDateTime.now())
                .build();

        EmailVerificationToken saved = tokenRepository.save(token);

        log.info("Email verification token created for user: {}", userId);
        return saved;
    }

    /**
     * Verify email using token.
     *
     * @param tokenString Token string
     * @return true if verification successful
     */
    public boolean verifyEmail(String tokenString) {
        EmailVerificationToken token = tokenRepository
                .findByTokenAndVerifiedFalseAndExpiresAtAfter(tokenString, OffsetDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired verification token"));

        // Increment attempts
        token.setAttempts(token.getAttempts() + 1);

        // Mark as verified
        token.setVerified(true);
        token.setVerifiedAt(OffsetDateTime.now());
        tokenRepository.save(token);

        // Update user email verification status
        User user = token.getUser();
        user.setEmailVerified(true);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getId());
        return true;
    }

    /**
     * Check if token is valid.
     *
     * @param tokenString Token string
     * @return true if token is valid
     */
    @Transactional(readOnly = true)
    public boolean isTokenValid(String tokenString) {
        return tokenRepository
                .findByTokenAndVerifiedFalseAndExpiresAtAfter(tokenString, OffsetDateTime.now())
                .isPresent();
    }

    /**
     * Get verification token by token string.
     *
     * @param tokenString Token string
     * @return EmailVerificationToken
     */
    @Transactional(readOnly = true)
    public EmailVerificationToken getToken(String tokenString) {
        return tokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new RuntimeException("Token not found"));
    }

    /**
     * Resend verification email.
     *
     * @param userId User ID
     * @return New EmailVerificationToken
     */
    public EmailVerificationToken resendVerificationEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (user.getEmailVerified()) {
            throw new RuntimeException("Email already verified");
        }

        // Create new token (old ones will be deleted)
        EmailVerificationToken token = createVerificationToken(userId);

        log.info("Verification email resent for user: {}", userId);
        return token;
    }

    /**
     * Get user's verification tokens.
     *
     * @param userId User ID
     * @return List of EmailVerificationToken
     */
    @Transactional(readOnly = true)
    public List<EmailVerificationToken> getUserTokens(UUID userId) {
        return tokenRepository.findByUserId(userId);
    }

    /**
     * Check if user has pending verification.
     *
     * @param userId User ID
     * @return true if user has unverified tokens
     */
    @Transactional(readOnly = true)
    public boolean hasPendingVerification(UUID userId) {
        return tokenRepository.existsByUserIdAndVerifiedFalse(userId);
    }

    /**
     * Clean up expired verification tokens.
     *
     * @return Number of tokens deleted
     */
    public int cleanupExpiredTokens() {
        List<EmailVerificationToken> expiredTokens = tokenRepository
                .findByExpiresAtBefore(OffsetDateTime.now());

        int count = expiredTokens.size();
        tokenRepository.deleteByExpiresAtBefore(OffsetDateTime.now());

        log.info("Cleaned up {} expired email verification tokens", count);
        return count;
    }

    /**
     * Delete all tokens for user.
     *
     * @param userId User ID
     */
    public void deleteUserTokens(UUID userId) {
        tokenRepository.deleteByUserId(userId);
        log.info("Deleted all verification tokens for user: {}", userId);
    }

    /**
     * Count unverified tokens for user.
     *
     * @param userId User ID
     * @return Count of unverified tokens
     */
    @Transactional(readOnly = true)
    public long countUnverifiedTokens(UUID userId) {
        return tokenRepository.countByUserIdAndVerifiedFalse(userId);
    }

    /**
     * Get latest unverified token for user.
     *
     * @param userId User ID
     * @return EmailVerificationToken or null
     */
    @Transactional(readOnly = true)
    public EmailVerificationToken getLatestUnverifiedToken(UUID userId) {
        return tokenRepository.findFirstByUserIdAndVerifiedFalseOrderByCreatedAtDesc(userId)
                .orElse(null);
    }
}
