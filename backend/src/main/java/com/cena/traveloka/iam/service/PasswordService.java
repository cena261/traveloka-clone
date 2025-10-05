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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PasswordService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final KeycloakSyncService keycloakSyncService;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$"
    );

    public String hashPassword(String plainPassword) {
        log.debug("Hashing password");
        return passwordEncoder.encode(plainPassword);
    }

    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }

    public boolean validatePasswordComplexity(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    public PasswordResetToken createPasswordResetToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

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

    @Transactional(readOnly = true)
    public boolean validateResetToken(String tokenString) {
        Optional<PasswordResetToken> token = tokenRepository
                .findByTokenAndUsedFalseAndExpiresAtAfter(tokenString, OffsetDateTime.now());
        return token.isPresent();
    }

    public void resetPassword(String tokenString, String newPassword) {
        if (!validatePasswordComplexity(newPassword)) {
            throw new RuntimeException("Password does not meet complexity requirements");
        }

        PasswordResetToken token = tokenRepository
                .findByTokenAndUsedFalseAndExpiresAtAfter(tokenString, OffsetDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

        User user = token.getUser();

        if (user.getKeycloakId() != null) {
            keycloakSyncService.resetPasswordInKeycloak(user.getKeycloakId(), newPassword);
        }

        user.setPasswordChangedAt(OffsetDateTime.now());
        userRepository.save(user);

        token.setUsed(true);
        token.setUsedAt(OffsetDateTime.now());
        tokenRepository.save(token);

        log.info("Password reset successful for user: {}", user.getId());
    }

    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        if (!validatePasswordComplexity(newPassword)) {
            throw new RuntimeException("Password does not meet complexity requirements");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (user.getKeycloakId() != null) {
            keycloakSyncService.resetPasswordInKeycloak(user.getKeycloakId(), newPassword);
        } else {
            throw new RuntimeException("User not linked to Keycloak");
        }

        user.setPasswordChangedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("Password changed for user: {}", userId);
    }

    public int cleanupExpiredTokens() {
        var expiredTokens = tokenRepository.findByExpiresAtBefore(OffsetDateTime.now());
        int count = expiredTokens.size();
        tokenRepository.deleteAll(expiredTokens);
        log.info("Cleaned up {} expired password reset tokens", count);
        return count;
    }
}
