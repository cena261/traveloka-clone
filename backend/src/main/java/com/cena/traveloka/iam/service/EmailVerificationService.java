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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    private static final int TOKEN_EXPIRY_HOURS = 24;

    public EmailVerificationToken createVerificationToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

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

    public boolean verifyEmail(String tokenString) {
        EmailVerificationToken token = tokenRepository
                .findByTokenAndVerifiedFalseAndExpiresAtAfter(tokenString, OffsetDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired verification token"));

        token.setAttempts(token.getAttempts() + 1);

        token.setVerified(true);
        token.setVerifiedAt(OffsetDateTime.now());
        tokenRepository.save(token);

        User user = token.getUser();
        user.setEmailVerified(true);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getId());
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isTokenValid(String tokenString) {
        return tokenRepository
                .findByTokenAndVerifiedFalseAndExpiresAtAfter(tokenString, OffsetDateTime.now())
                .isPresent();
    }

    @Transactional(readOnly = true)
    public EmailVerificationToken getToken(String tokenString) {
        return tokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new RuntimeException("Token not found"));
    }

    public EmailVerificationToken resendVerificationEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (user.getEmailVerified()) {
            throw new RuntimeException("Email already verified");
        }

        EmailVerificationToken token = createVerificationToken(userId);

        log.info("Verification email resent for user: {}", userId);
        return token;
    }

    @Transactional(readOnly = true)
    public List<EmailVerificationToken> getUserTokens(UUID userId) {
        return tokenRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean hasPendingVerification(UUID userId) {
        return tokenRepository.existsByUserIdAndVerifiedFalse(userId);
    }

    public int cleanupExpiredTokens() {
        List<EmailVerificationToken> expiredTokens = tokenRepository
                .findByExpiresAtBefore(OffsetDateTime.now());

        int count = expiredTokens.size();
        tokenRepository.deleteByExpiresAtBefore(OffsetDateTime.now());

        log.info("Cleaned up {} expired email verification tokens", count);
        return count;
    }

    public void deleteUserTokens(UUID userId) {
        tokenRepository.deleteByUserId(userId);
        log.info("Deleted all verification tokens for user: {}", userId);
    }

    @Transactional(readOnly = true)
    public long countUnverifiedTokens(UUID userId) {
        return tokenRepository.countByUserIdAndVerifiedFalse(userId);
    }

    @Transactional(readOnly = true)
    public EmailVerificationToken getLatestUnverifiedToken(UUID userId) {
        return tokenRepository.findFirstByUserIdAndVerifiedFalseOrderByCreatedAtDesc(userId)
                .orElse(null);
    }
}
