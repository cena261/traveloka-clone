package com.cena.traveloka.common.util;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Utility class for validation operations including Vietnamese-specific format validation.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Vietnamese phone number validation (+84 format)</li>
 *   <li>Email validation</li>
 *   <li>String validation (null, empty, blank checks)</li>
 *   <li>Numeric validation</li>
 *   <li>Vietnamese address validation</li>
 * </ul>
 *
 * <p>Vietnamese phone number format:</p>
 * <ul>
 *   <li>Must start with +84</li>
 *   <li>Followed by 9-10 digits</li>
 *   <li>Examples: +84901234567, +840901234567</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>
 * boolean isValid = ValidationUtils.isValidVietnamesePhone("+84901234567"); // true
 * boolean isValid = ValidationUtils.isValidVietnamesePhone("0901234567");   // false
 * </pre>
 *
 * @since 1.0.0
 */
public final class ValidationUtils {

    /**
     * Pattern for Vietnamese phone numbers (international format with +84)
     * Format: +84 followed by 9-10 digits
     * Examples: +84901234567, +840901234567
     */
    private static final Pattern VIETNAMESE_PHONE_PATTERN =
        Pattern.compile("^\\+84[0-9]{9,10}$");

    /**
     * Pattern for Vietnamese mobile phone numbers (national format)
     * Format: 0 followed by 9 digits (mobile operators: 03, 05, 07, 08, 09)
     * Examples: 0901234567, 0391234567
     */
    private static final Pattern VIETNAMESE_MOBILE_NATIONAL_PATTERN =
        Pattern.compile("^0[3|5|7|8|9][0-9]{8}$");

    /**
     * Pattern for email validation (RFC 5322 simplified)
     */
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    /**
     * Pattern for Vietnamese postal code (6 digits)
     * Examples: 700000, 100000
     */
    private static final Pattern VIETNAMESE_POSTAL_CODE_PATTERN =
        Pattern.compile("^[0-9]{6}$");

    /**
     * Private constructor to prevent instantiation
     */
    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Validates Vietnamese phone number in international format (+84).
     *
     * @param phoneNumber The phone number to validate
     * @return true if the phone number is valid Vietnamese format with +84 prefix
     */
    public static boolean isValidVietnamesePhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        return VIETNAMESE_PHONE_PATTERN.matcher(phoneNumber.trim()).matches();
    }

    /**
     * Validates Vietnamese mobile phone number in national format (starts with 0).
     *
     * @param phoneNumber The phone number to validate
     * @return true if the phone number is valid Vietnamese mobile format
     */
    public static boolean isValidVietnameseMobileNational(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        return VIETNAMESE_MOBILE_NATIONAL_PATTERN.matcher(phoneNumber.trim()).matches();
    }

    /**
     * Converts Vietnamese national format phone number to international format.
     * Converts 0901234567 to +84901234567
     *
     * @param nationalPhone National format phone number (starts with 0)
     * @return International format phone number (+84...) or null if input is invalid
     */
    public static String convertToInternationalFormat(String nationalPhone) {
        if (nationalPhone == null || nationalPhone.trim().isEmpty()) {
            return null;
        }

        String trimmed = nationalPhone.trim();
        if (trimmed.startsWith("0") && trimmed.length() >= 10) {
            return "+84" + trimmed.substring(1);
        }

        return null;
    }

    /**
     * Validates email address format.
     *
     * @param email The email address to validate
     * @return true if the email format is valid
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validates Vietnamese postal code (6 digits).
     *
     * @param postalCode The postal code to validate
     * @return true if the postal code is valid Vietnamese format
     */
    public static boolean isValidVietnamesePostalCode(String postalCode) {
        if (postalCode == null || postalCode.trim().isEmpty()) {
            return false;
        }
        return VIETNAMESE_POSTAL_CODE_PATTERN.matcher(postalCode.trim()).matches();
    }

    /**
     * Checks if a string is null or empty.
     *
     * @param str The string to check
     * @return true if the string is null or empty
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Checks if a string is null, empty, or contains only whitespace.
     *
     * @param str The string to check
     * @return true if the string is null, empty, or blank
     */
    public static boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Checks if a string is not null and not empty.
     *
     * @param str The string to check
     * @return true if the string is not null and not empty
     */
    public static boolean isNotNullOrEmpty(String str) {
        return !isNullOrEmpty(str);
    }

    /**
     * Checks if a string is not null and not blank.
     *
     * @param str The string to check
     * @return true if the string is not null and not blank
     */
    public static boolean isNotNullOrBlank(String str) {
        return !isNullOrBlank(str);
    }

    /**
     * Validates that a string has content within specified length bounds.
     *
     * @param str The string to validate
     * @param minLength Minimum length (inclusive)
     * @param maxLength Maximum length (inclusive)
     * @return true if string is not null and length is within bounds
     */
    public static boolean hasLengthBetween(String str, int minLength, int maxLength) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * Validates that a number is within specified bounds.
     *
     * @param value The value to check
     * @param min Minimum value (inclusive)
     * @param max Maximum value (inclusive)
     * @return true if value is within bounds
     */
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Validates that a number is within specified bounds.
     *
     * @param value The value to check
     * @param min Minimum value (inclusive)
     * @param max Maximum value (inclusive)
     * @return true if value is within bounds
     */
    public static boolean isInRange(long value, long min, long max) {
        return value >= min && value <= max;
    }

    /**
     * Validates that a number is within specified bounds.
     *
     * @param value The value to check
     * @param min Minimum value (inclusive)
     * @param max Maximum value (inclusive)
     * @return true if value is within bounds
     */
    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * Validates that a number is positive (greater than zero).
     *
     * @param value The value to check
     * @return true if value is positive
     */
    public static boolean isPositive(int value) {
        return value > 0;
    }

    /**
     * Validates that a number is positive (greater than zero).
     *
     * @param value The value to check
     * @return true if value is positive
     */
    public static boolean isPositive(long value) {
        return value > 0;
    }

    /**
     * Validates that a number is positive (greater than zero).
     *
     * @param value The value to check
     * @return true if value is positive
     */
    public static boolean isPositive(double value) {
        return value > 0;
    }

    /**
     * Validates that a number is non-negative (zero or greater).
     *
     * @param value The value to check
     * @return true if value is non-negative
     */
    public static boolean isNonNegative(int value) {
        return value >= 0;
    }

    /**
     * Validates that a number is non-negative (zero or greater).
     *
     * @param value The value to check
     * @return true if value is non-negative
     */
    public static boolean isNonNegative(long value) {
        return value >= 0;
    }

    /**
     * Validates that a number is non-negative (zero or greater).
     *
     * @param value The value to check
     * @return true if value is non-negative
     */
    public static boolean isNonNegative(double value) {
        return value >= 0;
    }

    /**
     * Validates Vietnamese address format.
     * Basic validation checking for essential address components.
     *
     * @param address The address string to validate
     * @return true if address contains basic required components
     */
    public static boolean isValidVietnameseAddress(String address) {
        if (isNullOrBlank(address)) {
            return false;
        }

        String trimmed = address.trim();

        // Basic validation: address should have minimum length and contain some structure
        // Vietnamese addresses typically have: street number/name, ward, district, city
        return trimmed.length() >= 10 && trimmed.contains(",");
    }

    /**
     * Validates that a string matches a specific pattern.
     *
     * @param str The string to validate
     * @param pattern The regex pattern to match
     * @return true if string matches the pattern
     * @throws IllegalArgumentException if pattern is null
     */
    public static boolean matchesPattern(String str, String pattern) {
        Objects.requireNonNull(pattern, "Pattern cannot be null");
        if (str == null) {
            return false;
        }
        return Pattern.compile(pattern).matcher(str).matches();
    }

    /**
     * Validates that a string contains only alphabetic characters.
     *
     * @param str The string to validate
     * @return true if string contains only letters (a-z, A-Z)
     */
    public static boolean isAlphabetic(String str) {
        if (isNullOrEmpty(str)) {
            return false;
        }
        return str.chars().allMatch(Character::isLetter);
    }

    /**
     * Validates that a string contains only alphanumeric characters.
     *
     * @param str The string to validate
     * @return true if string contains only letters and digits
     */
    public static boolean isAlphanumeric(String str) {
        if (isNullOrEmpty(str)) {
            return false;
        }
        return str.chars().allMatch(Character::isLetterOrDigit);
    }

    /**
     * Validates that a string contains only numeric characters.
     *
     * @param str The string to validate
     * @return true if string contains only digits
     */
    public static boolean isNumeric(String str) {
        if (isNullOrEmpty(str)) {
            return false;
        }
        return str.chars().allMatch(Character::isDigit);
    }

    /**
     * Validates UUID string format.
     *
     * @param uuid The UUID string to validate
     * @return true if string is a valid UUID format
     */
    public static boolean isValidUuid(String uuid) {
        if (isNullOrEmpty(uuid)) {
            return false;
        }
        try {
            java.util.UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}