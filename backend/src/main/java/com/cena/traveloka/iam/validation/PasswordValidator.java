package com.cena.traveloka.iam.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator implementation for ValidPassword annotation
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    // Common weak passwords to disallow
    private static final Set<String> COMMON_PASSWORDS = Set.of(
        "password", "123456", "123456789", "qwerty", "abc123",
        "password123", "admin", "letmein", "welcome", "monkey",
        "dragon", "master", "hello", "freedom", "whatever"
    );

    private int minLength;
    private int maxLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigit;
    private boolean requireSpecialChar;
    private String allowedSpecialChars;
    private boolean disallowCommon;

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        this.minLength = constraintAnnotation.minLength();
        this.maxLength = constraintAnnotation.maxLength();
        this.requireUppercase = constraintAnnotation.requireUppercase();
        this.requireLowercase = constraintAnnotation.requireLowercase();
        this.requireDigit = constraintAnnotation.requireDigit();
        this.requireSpecialChar = constraintAnnotation.requireSpecialChar();
        this.allowedSpecialChars = constraintAnnotation.allowedSpecialChars();
        this.disallowCommon = constraintAnnotation.disallowCommon();
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(password)) {
            return true; // Let @NotBlank handle null/empty validation
        }

        context.disableDefaultConstraintViolation();

        // Check length
        if (password.length() < minLength) {
            context.buildConstraintViolationWithTemplate(
                "Password must be at least " + minLength + " characters long"
            ).addConstraintViolation();
            return false;
        }

        if (password.length() > maxLength) {
            context.buildConstraintViolationWithTemplate(
                "Password must not exceed " + maxLength + " characters"
            ).addConstraintViolation();
            return false;
        }

        // Check uppercase requirement
        if (requireUppercase && !Pattern.matches(".*[A-Z].*", password)) {
            context.buildConstraintViolationWithTemplate(
                "Password must contain at least one uppercase letter"
            ).addConstraintViolation();
            return false;
        }

        // Check lowercase requirement
        if (requireLowercase && !Pattern.matches(".*[a-z].*", password)) {
            context.buildConstraintViolationWithTemplate(
                "Password must contain at least one lowercase letter"
            ).addConstraintViolation();
            return false;
        }

        // Check digit requirement
        if (requireDigit && !Pattern.matches(".*\\d.*", password)) {
            context.buildConstraintViolationWithTemplate(
                "Password must contain at least one digit"
            ).addConstraintViolation();
            return false;
        }

        // Check special character requirement
        if (requireSpecialChar) {
            String specialCharPattern = ".*[" + Pattern.quote(allowedSpecialChars) + "].*";
            if (!Pattern.matches(specialCharPattern, password)) {
                context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one special character from: " + allowedSpecialChars
                ).addConstraintViolation();
                return false;
            }
        }

        // Check common passwords
        if (disallowCommon && COMMON_PASSWORDS.contains(password.toLowerCase())) {
            context.buildConstraintViolationWithTemplate(
                "Password is too common and not allowed"
            ).addConstraintViolation();
            return false;
        }

        // Check for sequential characters (basic check)
        if (hasSequentialChars(password)) {
            context.buildConstraintViolationWithTemplate(
                "Password should not contain sequential characters"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }

    private boolean hasSequentialChars(String password) {
        // Check for simple sequential patterns like "123", "abc", "qwe"
        String lower = password.toLowerCase();

        // Check for 3+ consecutive digits
        if (Pattern.matches(".*\\d{3,}.*", password)) {
            for (int i = 0; i <= password.length() - 3; i++) {
                String sub = password.substring(i, i + 3);
                if (sub.matches("\\d{3}")) {
                    try {
                        int first = Character.getNumericValue(sub.charAt(0));
                        int second = Character.getNumericValue(sub.charAt(1));
                        int third = Character.getNumericValue(sub.charAt(2));

                        if (second == first + 1 && third == second + 1) {
                            return true;
                        }
                    } catch (Exception ignored) {
                        // Continue checking
                    }
                }
            }
        }

        // Check for 3+ consecutive letters
        if (Pattern.matches(".*[a-zA-Z]{3,}.*", lower)) {
            for (int i = 0; i <= lower.length() - 3; i++) {
                String sub = lower.substring(i, i + 3);
                if (sub.matches("[a-z]{3}")) {
                    char first = sub.charAt(0);
                    char second = sub.charAt(1);
                    char third = sub.charAt(2);

                    if (second == first + 1 && third == second + 1) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}