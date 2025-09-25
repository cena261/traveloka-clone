package com.cena.traveloka.iam.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Utility class for security-related operations
 *
 * Provides helper methods for:
 * - Input validation and sanitization
 * - Security token generation
 * - Hash generation for sensitive data
 * - Rate limiting helpers
 * - Security header validation
 */
@Slf4j
public final class SecurityUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Security patterns
    private static final Pattern SUSPICIOUS_PATTERN = Pattern.compile(
            "(?i)(script|javascript|vbscript|onload|onerror|onclick|eval|expression|alert|confirm|prompt)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._-]+$"
    );

    private static final Pattern SAFE_USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._-]{3,50}$"
    );

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"
    );

    private SecurityUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Check if input contains suspicious content
     *
     * @param input Input to check
     * @return true if suspicious content detected
     */
    public static boolean containsSuspiciousContent(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }

        return SUSPICIOUS_PATTERN.matcher(input).find();
    }

    /**
     * Validate filename for security
     *
     * @param filename Filename to validate
     * @return true if filename is safe
     */
    public static boolean isSafeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return false;
        }

        return SAFE_FILENAME_PATTERN.matcher(filename).matches() &&
               !filename.contains("..") &&
               filename.length() <= 255;
    }

    /**
     * Validate username format
     *
     * @param username Username to validate
     * @return true if username is safe
     */
    public static boolean isSafeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }

        return SAFE_USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * Validate IP address format
     *
     * @param ipAddress IP address to validate
     * @return true if IP address is valid
     */
    public static boolean isValidIpAddress(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            return false;
        }

        return IPV4_PATTERN.matcher(ipAddress).matches() ||
               IPV6_PATTERN.matcher(ipAddress).matches();
    }

    /**
     * Generate secure random token
     *
     * @param length Token length in bytes
     * @return Base64 encoded secure token
     */
    public static String generateSecureToken(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generate session ID
     *
     * @return Secure session ID
     */
    public static String generateSessionId() {
        return generateSecureToken(32);
    }

    /**
     * Generate device fingerprint hash
     *
     * @param userAgent User agent string
     * @param ipAddress IP address
     * @param additionalInfo Additional device info
     * @return Device fingerprint hash
     */
    public static String generateDeviceFingerprint(String userAgent, String ipAddress, String additionalInfo) {
        try {
            String combined = String.join("|",
                    StringUtils.hasText(userAgent) ? userAgent : "",
                    StringUtils.hasText(ipAddress) ? ipAddress : "",
                    StringUtils.hasText(additionalInfo) ? additionalInfo : ""
            );

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(combined.getBytes());
            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating device fingerprint", e);
            return generateSecureToken(32);
        }
    }

    /**
     * Hash sensitive data for storage
     *
     * @param data Sensitive data to hash
     * @param salt Salt for hashing
     * @return Hashed data
     */
    public static String hashSensitiveData(String data, String salt) {
        if (!StringUtils.hasText(data)) {
            return null;
        }

        try {
            String combined = data + salt;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(combined.getBytes());
            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            log.error("Error hashing sensitive data", e);
            throw new RuntimeException("Hashing algorithm not available", e);
        }
    }

    /**
     * Validate password strength
     *
     * @param password Password to validate
     * @return true if password meets strength requirements
     */
    public static boolean isStrongPassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < 8) {
            return false;
        }

        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);

        return hasUppercase && hasLowercase && hasDigit && hasSpecial;
    }

    /**
     * Check if request is from a private IP address
     *
     * @param ipAddress IP address to check
     * @return true if IP is private
     */
    public static boolean isPrivateIpAddress(String ipAddress) {
        if (!isValidIpAddress(ipAddress)) {
            return false;
        }

        // Check for private IPv4 ranges
        if (ipAddress.startsWith("10.") ||
            ipAddress.startsWith("192.168.") ||
            (ipAddress.startsWith("172.") && isInRange172(ipAddress))) {
            return true;
        }

        // Check for localhost
        return ipAddress.equals("127.0.0.1") || ipAddress.equals("::1");
    }

    /**
     * Check if IPv4 address is in 172.16.0.0 - 172.31.255.255 range
     */
    private static boolean isInRange172(String ipAddress) {
        try {
            String[] parts = ipAddress.split("\\.");
            if (parts.length != 4) return false;

            int secondOctet = Integer.parseInt(parts[1]);
            return secondOctet >= 16 && secondOctet <= 31;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Sanitize header value for logging
     *
     * @param headerValue Header value to sanitize
     * @return Sanitized header value
     */
    public static String sanitizeHeaderValue(String headerValue) {
        if (!StringUtils.hasText(headerValue)) {
            return headerValue;
        }

        // Remove control characters and limit length
        String sanitized = headerValue.replaceAll("[\\x00-\\x1F\\x7F]", "");

        if (sanitized.length() > 1000) {
            sanitized = sanitized.substring(0, 1000) + "...";
        }

        return sanitized;
    }

    /**
     * Generate CSRF token
     *
     * @return CSRF token
     */
    public static String generateCsrfToken() {
        return generateSecureToken(24);
    }

    /**
     * Check if user agent looks like a bot
     *
     * @param userAgent User agent string
     * @return true if appears to be a bot
     */
    public static boolean isLikelyBot(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return true; // No user agent is suspicious
        }

        String lowercaseUserAgent = userAgent.toLowerCase();

        return lowercaseUserAgent.contains("bot") ||
               lowercaseUserAgent.contains("crawler") ||
               lowercaseUserAgent.contains("spider") ||
               lowercaseUserAgent.contains("scraper") ||
               lowercaseUserAgent.contains("curl") ||
               lowercaseUserAgent.contains("wget") ||
               lowercaseUserAgent.length() < 10 ||
               lowercaseUserAgent.length() > 2000;
    }

    /**
     * Validate session timeout value
     *
     * @param timeoutHours Timeout in hours
     * @return true if valid timeout
     */
    public static boolean isValidSessionTimeout(Integer timeoutHours) {
        if (timeoutHours == null) {
            return false;
        }

        return timeoutHours >= 1 && timeoutHours <= 168; // 1 hour to 7 days
    }

    /**
     * Mask sensitive data for logging
     *
     * @param data Sensitive data to mask
     * @param visibleChars Number of characters to show at start and end
     * @return Masked data
     */
    public static String maskSensitiveData(String data, int visibleChars) {
        if (!StringUtils.hasText(data) || data.length() <= visibleChars * 2) {
            return "***";
        }

        String start = data.substring(0, visibleChars);
        String end = data.substring(data.length() - visibleChars);
        return start + "***" + end;
    }
}