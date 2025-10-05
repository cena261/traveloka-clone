package com.cena.traveloka.common.util;

import java.util.Objects;
import java.util.regex.Pattern;

public final class ValidationUtils {

    private static final Pattern VIETNAMESE_PHONE_PATTERN =
        Pattern.compile("^\\+84[0-9]{9,10}$");

    private static final Pattern VIETNAMESE_MOBILE_NATIONAL_PATTERN =
        Pattern.compile("^0[3|5|7|8|9][0-9]{8}$");

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    private static final Pattern VIETNAMESE_POSTAL_CODE_PATTERN =
        Pattern.compile("^[0-9]{6}$");

    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static boolean isValidVietnamesePhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        return VIETNAMESE_PHONE_PATTERN.matcher(phoneNumber.trim()).matches();
    }

    public static boolean isValidVietnameseMobileNational(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        return VIETNAMESE_MOBILE_NATIONAL_PATTERN.matcher(phoneNumber.trim()).matches();
    }

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

    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidVietnamesePostalCode(String postalCode) {
        if (postalCode == null || postalCode.trim().isEmpty()) {
            return false;
        }
        return VIETNAMESE_POSTAL_CODE_PATTERN.matcher(postalCode.trim()).matches();
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotNullOrEmpty(String str) {
        return !isNullOrEmpty(str);
    }

    public static boolean isNotNullOrBlank(String str) {
        return !isNullOrBlank(str);
    }

    public static boolean hasLengthBetween(String str, int minLength, int maxLength) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        return length >= minLength && length <= maxLength;
    }

    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    public static boolean isInRange(long value, long min, long max) {
        return value >= min && value <= max;
    }

    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    public static boolean isPositive(int value) {
        return value > 0;
    }

    public static boolean isPositive(long value) {
        return value > 0;
    }

    public static boolean isPositive(double value) {
        return value > 0;
    }

    public static boolean isNonNegative(int value) {
        return value >= 0;
    }

    public static boolean isNonNegative(long value) {
        return value >= 0;
    }

    public static boolean isNonNegative(double value) {
        return value >= 0;
    }

    public static boolean isValidVietnameseAddress(String address) {
        if (isNullOrBlank(address)) {
            return false;
        }

        String trimmed = address.trim();

        return trimmed.length() >= 10 && trimmed.contains(",");
    }

    public static boolean matchesPattern(String str, String pattern) {
        Objects.requireNonNull(pattern, "Pattern cannot be null");
        if (str == null) {
            return false;
        }
        return Pattern.compile(pattern).matcher(str).matches();
    }

    public static boolean isAlphabetic(String str) {
        if (isNullOrEmpty(str)) {
            return false;
        }
        return str.chars().allMatch(Character::isLetter);
    }

    public static boolean isAlphanumeric(String str) {
        if (isNullOrEmpty(str)) {
            return false;
        }
        return str.chars().allMatch(Character::isLetterOrDigit);
    }

    public static boolean isNumeric(String str) {
        if (isNullOrEmpty(str)) {
            return false;
        }
        return str.chars().allMatch(Character::isDigit);
    }

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