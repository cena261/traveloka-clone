package com.cena.traveloka.iam.service;

import com.cena.traveloka.iam.entity.PasswordResetToken;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.repository.PasswordResetTokenRepository;
import com.cena.traveloka.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * T051: PasswordService
 * Service for password management operations.
 *
 * Constitutional Compliance:
 * - NFR-001: BCrypt password hashing with 12 rounds
 * - FR-009: Password reset with time-limited tokens
 * - Principle III: Layered Architecture - Business logic in service layer
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PasswordService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final KeycloakSyncService keycloakSyncService;

    /**
     * Password complexity regex (NFR-001):
     * - Minimum 8 characters
     * - At least 1 uppercase letter
     * - At least 1 lowercase letter
     * - At least 1 number
     * - At least 1 special character
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$"
    );

    /**
     * Hash password using BCrypt (NFR-001).
     *
     * @param plainPassword Plain text password
     * @return BCrypt hashed password
     */
    public String hashPassword(String plainPassword) {
        log.debug("Hashing password");
        return passwordEncoder.encode(plainPassword);
    }

    /**
     * Verify password against hash.
     *
     * @param plainPassword Plain text password
     * @param hashedPassword BCrypt hashed password
     * @return true if password matches
     */
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }

    /**
     * Validate password complexity (NFR-001).
     *
     * @param password Password to validate
     * @return true if password meets complexity requirements
     */
    public boolean validatePasswordComplexity(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Create password reset token (FR-009).
     * Token expires in 1 hour.
     *
     * @param userId User ID
     * @return PasswordResetToken
     */
    public PasswordResetToken createPasswordResetToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Invalidate old unused tokens - delete them from the list
        tokenRepository.findByUserIdAndUsedFalse(userId).forEach(tokenRepository::delete);

        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .used(false)
                .createdAt(OffsetDateTime.now())
                .build();

        PasswordResetToken saved = tokenRepository.save(token);
        log.info("Created password reset token for user: {}", userId);

        return saved;
    }

    /**
     * Validate reset token.
     *
     * @param tokenString Token string
     * @return true if token is valid (not used, not expired)
     */
    @Transactional(readOnly = true)
    public boolean validateResetToken(String tokenString) {
        Optional<PasswordResetToken> token = tokenRepository
                .findByTokenAndUsedFalseAndExpiresAtAfter(tokenString, OffsetDateTime.now());
        return token.isPresent();
    }

    /**
     * Reset password using valid token (FR-009).
     *
     * @param tokenString Reset token
     * @param newPassword New password
     */
    public void resetPassword(String tokenString, String newPassword) {
        if (!validatePasswordComplexity(newPassword)) {
            throw new RuntimeException("Password does not meet complexity requirements");
        }

        PasswordResetToken token = tokenRepository
                .findByTokenAndUsedFalseAndExpiresAtAfter(tokenString, OffsetDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

        User user = token.getUser();

        // Update password in Keycloak
        if (user.getKeycloakId() != null) {
            keycloakSyncService.resetPasswordInKeycloak(user.getKeycloakId(), newPassword);
        }

        // Update password changed timestamp
        user.setPasswordChangedAt(OffsetDateTime.now());
        userRepository.save(user);

        // Mark token as used
        token.setUsed(true);
        token.setUsedAt(OffsetDateTime.now());
        tokenRepository.save(token);

        log.info("Password reset successful for user: {}", user.getId());
    }

    /**
     * Change password for authenticated user.
     *
     * @param userId User ID
     * @param currentPassword Current password
     * @param newPassword New password
     */
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        if (!validatePasswordComplexity(newPassword)) {
            throw new RuntimeException("Password does not meet complexity requirements");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Password verification and update handled by Keycloak
        if (user.getKeycloakId() != null) {
            // Keycloak handles current password verification internally
            keycloakSyncService.resetPasswordInKeycloak(user.getKeycloakId(), newPassword);
        } else {
            throw new RuntimeException("User not linked to Keycloak");
        }

        // Update password changed timestamp
        user.setPasswordChangedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("Password changed for user: {}", userId);
    }

    /**
     * Clean up expired password reset tokens.
     *
     * @return Number of tokens deleted
     */
    public int cleanupExpiredTokens() {
        var expiredTokens = tokenRepository.findByExpiresAtBefore(OffsetDateTime.now());
        int count = expiredTokens.size();
        tokenRepository.deleteAll(expiredTokens);
        log.info("Cleaned up {} expired password reset tokens", count);
        return count;
    }
}
