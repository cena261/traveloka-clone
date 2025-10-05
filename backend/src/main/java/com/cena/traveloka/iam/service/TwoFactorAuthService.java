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

    public TwoFactorSetupDto setupTotpAuth(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (twoFactorRepository.findByUserIdAndMethod(userId, "totp").isPresent()) {
            throw new RuntimeException("TOTP 2FA already enabled for user");
        }

        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        String secret = key.getKey();

        List<String> backupCodes = generateBackupCodes();

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

        String qrCodeUrl = generateQrCodeUrl(user.getEmail(), secret);

        log.info("TOTP 2FA setup initiated for user: {}", userId);

        return TwoFactorSetupDto.builder()
                .secret(secret)
                .qrCode(qrCodeUrl) // Using qrCode field instead of qrCodeUrl
                .backupCodes(backupCodes)
                .accountName(user.getEmail())
                .build();
    }

    public boolean verifyTotpSetup(UUID userId, String code) {
        TwoFactorAuth twoFactorAuth = twoFactorRepository.findByUserIdAndMethod(userId, "totp")
                .orElseThrow(() -> new RuntimeException("TOTP 2FA not setup for user"));

        boolean isValid = googleAuthenticator.authorize(twoFactorAuth.getSecret(), Integer.parseInt(code));

        if (!isValid) {
            log.warn("Invalid TOTP code provided for user: {}", userId);
            return false;
        }

        twoFactorAuth.setVerified(true);
        twoFactorAuth.setVerifiedAt(OffsetDateTime.now());
        twoFactorAuth.setIsActive(true);
        twoFactorAuth.setIsPrimary(true);
        twoFactorAuth.setUpdatedAt(OffsetDateTime.now());
        twoFactorRepository.save(twoFactorAuth);

        User user = twoFactorAuth.getUser();
        user.setTwoFactorEnabled(true);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("TOTP 2FA verified and activated for user: {}", userId);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean verifyTotpCode(UUID userId, String code) {
        TwoFactorAuth twoFactorAuth = twoFactorRepository.findByUserIdAndMethod(userId, "totp")
                .orElseThrow(() -> new RuntimeException("TOTP 2FA not enabled for user"));

        if (!twoFactorAuth.getIsActive() || !twoFactorAuth.getVerified()) {
            throw new RuntimeException("TOTP 2FA not active for user");
        }

        boolean isValid = googleAuthenticator.authorize(twoFactorAuth.getSecret(), Integer.parseInt(code));

        if (isValid) {
            updateLastUsed(twoFactorAuth.getId());
        }

        return isValid;
    }

    public void setupSmsAuth(UUID userId, String phoneNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

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

    public void setupEmailAuth(UUID userId, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

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

    public boolean verifyBackupCode(UUID userId, String code) {
        TwoFactorAuth twoFactorAuth = twoFactorRepository.findByUserIdAndMethod(userId, "totp")
                .orElseThrow(() -> new RuntimeException("TOTP 2FA not enabled for user"));

        if (twoFactorAuth.getBackupCodes() == null || twoFactorAuth.getBackupCodes().isEmpty()) {
            return false;
        }

        if (!twoFactorAuth.getBackupCodes().contains(code)) {
            return false;
        }

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

    public void disable2FA(UUID userId) {
        List<TwoFactorAuth> twoFactorAuths = twoFactorRepository.findByUserId(userId);

        twoFactorAuths.forEach(auth -> {
            auth.setIsActive(false);
            auth.setUpdatedAt(OffsetDateTime.now());
            twoFactorRepository.save(auth);
        });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        user.setTwoFactorEnabled(false);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("2FA disabled for user: {}", userId);
    }

    @Transactional(readOnly = true)
    public List<TwoFactorAuth> getUserTwoFactorMethods(UUID userId) {
        return twoFactorRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean hasActive2FA(UUID userId) {
        return twoFactorRepository.existsByUserIdAndIsActiveTrueAndVerifiedTrue(userId);
    }

    @Transactional(readOnly = true)
    public TwoFactorAuth getPrimary2FAMethod(UUID userId) {
        return twoFactorRepository.findByUserIdAndIsPrimaryTrue(userId).orElse(null);
    }

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

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void updateLastUsed(UUID twoFactorId) {
        twoFactorRepository.findById(twoFactorId).ifPresent(auth -> {
            auth.setLastUsedAt(OffsetDateTime.now());
            auth.setUpdatedAt(OffsetDateTime.now());
            twoFactorRepository.save(auth);
        });
    }

    public TwoFactorSetupDto setupTwoFactorAuth(String token) {
        throw new UnsupportedOperationException("setupTwoFactorAuth not yet implemented - requires JWT integration");
    }

    public void verifyAndActivateTwoFactorAuth(String token, String code) {
        throw new UnsupportedOperationException("verifyAndActivateTwoFactorAuth not yet implemented - requires JWT integration");
    }

    public void disableTwoFactorAuth(String token, String password, String backupCode) {
        throw new UnsupportedOperationException("disableTwoFactorAuth not yet implemented - requires JWT integration");
    }
}
