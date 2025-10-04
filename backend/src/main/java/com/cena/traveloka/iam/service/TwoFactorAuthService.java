package com.cena.traveloka.iam.service;

import com.cena.traveloka.iam.dto.response.TwoFactorSetupDto;
import com.cena.traveloka.iam.entity.TwoFactorAuth;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.repository.TwoFactorAuthRepository;
import com.cena.traveloka.iam.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * T059: TwoFactorAuthService
 * Service for two-factor authentication operations.
 *
 * Constitutional Compliance:
 * - FR-014: TOTP-based 2FA support
 * - FR-015: SMS/Email 2FA fallback
 * - Principle III: Layered Architecture - Business logic in service layer
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TwoFactorAuthService {

    private final TwoFactorAuthRepository twoFactorRepository;
    private final UserRepository userRepository;
    private final GoogleAuthenticator googleAuthenticator;

    private static final int BACKUP_CODES_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 8;

    /**
     * Setup TOTP 2FA for user (FR-014).
     *
     * @param userId User ID
     * @return TwoFactorSetupDto with secret and QR code URL
     */
    public TwoFactorSetupDto setupTotpAuth(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Check if TOTP already exists
        if (twoFactorRepository.findByUserIdAndMethod(userId, "totp").isPresent()) {
            throw new RuntimeException("TOTP 2FA already enabled for user");
        }

        // Generate secret key
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        String secret = key.getKey();

        // Generate backup codes
        List<String> backupCodes = generateBackupCodes();

        // Create 2FA record (not verified yet)
        TwoFactorAuth twoFactorAuth = TwoFactorAuth.builder()
                .user(user)
                .method("totp")
                .secret(secret)
                .backupCodes(backupCodes)
                .isPrimary(false)
                .isActive(false)
                .verified(false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        twoFactorRepository.save(twoFactorAuth);

        // Generate QR code URL for Google Authenticator
        String qrCodeUrl = generateQrCodeUrl(user.getEmail(), secret);

        log.info("TOTP 2FA setup initiated for user: {}", userId);

        return TwoFactorSetupDto.builder()
                .secret(secret)
                .qrCode(qrCodeUrl) // Using qrCode field instead of qrCodeUrl
                .backupCodes(backupCodes)
                .accountName(user.getEmail())
                .build();
    }

    /**
     * Verify and activate TOTP 2FA.
     *
     * @param userId User ID
     * @param code TOTP code from user
     * @return true if verification successful
     */
    public boolean verifyTotpSetup(UUID userId, String code) {
        TwoFactorAuth twoFactorAuth = twoFactorRepository.findByUserIdAndMethod(userId, "totp")
                .orElseThrow(() -> new RuntimeException("TOTP 2FA not setup for user"));

        // Verify code
        boolean isValid = googleAuthenticator.authorize(twoFactorAuth.getSecret(), Integer.parseInt(code));

        if (!isValid) {
            log.warn("Invalid TOTP code provided for user: {}", userId);
            return false;
        }

        // Activate TOTP
        twoFactorAuth.setVerified(true);
        twoFactorAuth.setVerifiedAt(OffsetDateTime.now());
        twoFactorAuth.setIsActive(true);
        twoFactorAuth.setIsPrimary(true);
        twoFactorAuth.setUpdatedAt(OffsetDateTime.now());
        twoFactorRepository.save(twoFactorAuth);

        // Update user 2FA status
        User user = twoFactorAuth.getUser();
        user.setTwoFactorEnabled(true);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("TOTP 2FA verified and activated for user: {}", userId);
        return true;
    }

    /**
     * Verify TOTP code during login.
     *
     * @param userId User ID
     * @param code TOTP code
     * @return true if code is valid
     */
    @Transactional(readOnly = true)
    public boolean verifyTotpCode(UUID userId, String code) {
        TwoFactorAuth twoFactorAuth = twoFactorRepository.findByUserIdAndMethod(userId, "totp")
                .orElseThrow(() -> new RuntimeException("TOTP 2FA not enabled for user"));

        if (!twoFactorAuth.getIsActive() || !twoFactorAuth.getVerified()) {
            throw new RuntimeException("TOTP 2FA not active for user");
        }

        boolean isValid = googleAuthenticator.authorize(twoFactorAuth.getSecret(), Integer.parseInt(code));

        if (isValid) {
            // Update last used timestamp (requires new transaction)
            updateLastUsed(twoFactorAuth.getId());
        }

        return isValid;
    }

    /**
     * Setup SMS 2FA (FR-015).
     *
     * @param userId User ID
     * @param phoneNumber Phone number for SMS
     */
    public void setupSmsAuth(UUID userId, String phoneNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Check if SMS auth already exists
        twoFactorRepository.findByUserIdAndMethod(userId, "sms")
                .ifPresent(auth -> {
                    throw new RuntimeException("SMS 2FA already enabled for user");
                });

        TwoFactorAuth twoFactorAuth = TwoFactorAuth.builder()
                .user(user)
                .method("sms")
                .phoneNumber(phoneNumber)
                .isPrimary(false)
                .isActive(true)
                .verified(false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        twoFactorRepository.save(twoFactorAuth);

        log.info("SMS 2FA setup for user: {}", userId);
    }

    /**
     * Setup Email 2FA (FR-015).
     *
     * @param userId User ID
     * @param email Email for 2FA codes
     */
    public void setupEmailAuth(UUID userId, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Check if Email auth already exists
        twoFactorRepository.findByUserIdAndMethod(userId, "email")
                .ifPresent(auth -> {
                    throw new RuntimeException("Email 2FA already enabled for user");
                });

        TwoFactorAuth twoFactorAuth = TwoFactorAuth.builder()
                .user(user)
                .method("email")
                .email(email)
                .isPrimary(false)
                .isActive(true)
                .verified(false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        twoFactorRepository.save(twoFactorAuth);

        log.info("Email 2FA setup for user: {}", userId);
    }

    /**
     * Verify backup code.
     *
     * @param userId User ID
     * @param code Backup code
     * @return true if code is valid
     */
    public boolean verifyBackupCode(UUID userId, String code) {
        TwoFactorAuth twoFactorAuth = twoFactorRepository.findByUserIdAndMethod(userId, "totp")
                .orElseThrow(() -> new RuntimeException("TOTP 2FA not enabled for user"));

        if (twoFactorAuth.getBackupCodes() == null || twoFactorAuth.getBackupCodes().isEmpty()) {
            return false;
        }

        // Check if code exists in backup codes
        if (!twoFactorAuth.getBackupCodes().contains(code)) {
            return false;
        }

        // Remove used backup code
        List<String> updatedCodes = twoFactorAuth.getBackupCodes().stream()
                .filter(c -> !c.equals(code))
                .collect(Collectors.toList());

        twoFactorAuth.setBackupCodes(updatedCodes);
        twoFactorAuth.setLastUsedAt(OffsetDateTime.now());
        twoFactorAuth.setUpdatedAt(OffsetDateTime.now());
        twoFactorRepository.save(twoFactorAuth);

        log.info("Backup code used for user: {}", userId);
        return true;
    }

    /**
     * Disable 2FA for user.
     *
     * @param userId User ID
     */
    public void disable2FA(UUID userId) {
        List<TwoFactorAuth> twoFactorAuths = twoFactorRepository.findByUserId(userId);

        twoFactorAuths.forEach(auth -> {
            auth.setIsActive(false);
            auth.setUpdatedAt(OffsetDateTime.now());
            twoFactorRepository.save(auth);
        });

        // Update user 2FA status
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        user.setTwoFactorEnabled(false);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("2FA disabled for user: {}", userId);
    }

    /**
     * Get user's 2FA methods.
     *
     * @param userId User ID
     * @return List of TwoFactorAuth
     */
    @Transactional(readOnly = true)
    public List<TwoFactorAuth> getUserTwoFactorMethods(UUID userId) {
        return twoFactorRepository.findByUserId(userId);
    }

    /**
     * Check if user has active 2FA.
     *
     * @param userId User ID
     * @return true if user has active 2FA
     */
    @Transactional(readOnly = true)
    public boolean hasActive2FA(UUID userId) {
        return twoFactorRepository.existsByUserIdAndIsActiveTrueAndVerifiedTrue(userId);
    }

    /**
     * Get primary 2FA method.
     *
     * @param userId User ID
     * @return TwoFactorAuth or null
     */
    @Transactional(readOnly = true)
    public TwoFactorAuth getPrimary2FAMethod(UUID userId) {
        return twoFactorRepository.findByUserIdAndIsPrimaryTrue(userId).orElse(null);
    }

    /**
     * Regenerate backup codes.
     *
     * @param userId User ID
     * @return New list of backup codes
     */
    public List<String> regenerateBackupCodes(UUID userId) {
        TwoFactorAuth twoFactorAuth = twoFactorRepository.findByUserIdAndMethod(userId, "totp")
                .orElseThrow(() -> new RuntimeException("TOTP 2FA not enabled for user"));

        List<String> newBackupCodes = generateBackupCodes();
        twoFactorAuth.setBackupCodes(newBackupCodes);
        twoFactorAuth.setUpdatedAt(OffsetDateTime.now());
        twoFactorRepository.save(twoFactorAuth);

        log.info("Backup codes regenerated for user: {}", userId);
        return newBackupCodes;
    }

    // Private helper methods

    /**
     * Generate backup codes.
     *
     * @return List of backup codes
     */
    private List<String> generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        List<String> codes = new ArrayList<>();

        for (int i = 0; i < BACKUP_CODES_COUNT; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < BACKUP_CODE_LENGTH; j++) {
                code.append(random.nextInt(10));
            }
            codes.add(code.toString());
        }

        return codes;
    }

    /**
     * Generate QR code URL for Google Authenticator.
     *
     * @param email User email
     * @param secret TOTP secret
     * @return QR code URL
     */
    private String generateQrCodeUrl(String email, String secret) {
        String issuer = "Traveloka";
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                issuer,
                email,
                secret,
                issuer
        );
    }

    /**
     * Update last used timestamp (requires new transaction).
     *
     * @param twoFactorId TwoFactorAuth ID
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void updateLastUsed(UUID twoFactorId) {
        twoFactorRepository.findById(twoFactorId).ifPresent(auth -> {
            auth.setLastUsedAt(OffsetDateTime.now());
            auth.setUpdatedAt(OffsetDateTime.now());
            twoFactorRepository.save(auth);
        });
    }

    /**
     * Setup two-factor auth from JWT token.
     *
     * @param token JWT token
     * @return TwoFactorSetupDto
     */
    public TwoFactorSetupDto setupTwoFactorAuth(String token) {
        // TODO: Extract user ID from JWT token
        // For now, throw exception - needs JwtTokenProvider integration
        throw new UnsupportedOperationException("setupTwoFactorAuth not yet implemented - requires JWT integration");
    }

    /**
     * Verify and activate 2FA from JWT token.
     *
     * @param token JWT token
     * @param code TOTP code
     */
    public void verifyAndActivateTwoFactorAuth(String token, String code) {
        // TODO: Extract user ID from JWT token
        // For now, throw exception - needs JwtTokenProvider integration
        throw new UnsupportedOperationException("verifyAndActivateTwoFactorAuth not yet implemented - requires JWT integration");
    }

    /**
     * Disable 2FA from JWT token with password or backup code verification.
     *
     * @param token JWT token
     * @param password Current password (optional)
     * @param backupCode Backup code (optional)
     */
    public void disableTwoFactorAuth(String token, String password, String backupCode) {
        // TODO: Extract user ID from JWT token and verify password or backup code
        // For now, throw exception - needs JwtTokenProvider integration
        throw new UnsupportedOperationException("disableTwoFactorAuth not yet implemented - requires JWT integration");
    }
}
