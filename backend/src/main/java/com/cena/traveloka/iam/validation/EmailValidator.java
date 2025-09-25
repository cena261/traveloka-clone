package com.cena.traveloka.iam.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator implementation for ValidEmail annotation
 */
public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

    private static final String EMAIL_PATTERN =
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    private static final Pattern EMAIL_REGEX = Pattern.compile(EMAIL_PATTERN);

    // Common disposable email domains
    private static final Set<String> DISPOSABLE_DOMAINS = Set.of(
        "10minutemail.com", "tempmail.org", "guerrillamail.com",
        "mailinator.com", "throwaway.email", "temp-mail.org"
    );

    // Corporate email patterns (basic check)
    private static final Set<String> PERSONAL_DOMAINS = Set.of(
        "gmail.com", "yahoo.com", "hotmail.com", "outlook.com",
        "aol.com", "icloud.com", "protonmail.com"
    );

    private boolean corporateOnly;
    private boolean allowDisposable;
    private int maxLength;

    @Override
    public void initialize(ValidEmail constraintAnnotation) {
        this.corporateOnly = constraintAnnotation.corporateOnly();
        this.allowDisposable = constraintAnnotation.allowDisposable();
        this.maxLength = constraintAnnotation.maxLength();
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(email)) {
            return true; // Let @NotBlank handle null/empty validation
        }

        // Check length
        if (email.length() > maxLength) {
            setCustomMessage(context, "Email address exceeds maximum length of " + maxLength);
            return false;
        }

        // Check basic format
        if (!EMAIL_REGEX.matcher(email).matches()) {
            setCustomMessage(context, "Email address format is invalid");
            return false;
        }

        String domain = extractDomain(email);
        if (domain == null) {
            return false;
        }

        // Check disposable email
        if (!allowDisposable && DISPOSABLE_DOMAINS.contains(domain.toLowerCase())) {
            setCustomMessage(context, "Disposable email addresses are not allowed");
            return false;
        }

        // Check corporate email requirement
        if (corporateOnly && PERSONAL_DOMAINS.contains(domain.toLowerCase())) {
            setCustomMessage(context, "Corporate email address is required");
            return false;
        }

        return true;
    }

    private String extractDomain(String email) {
        int atIndex = email.lastIndexOf('@');
        if (atIndex == -1 || atIndex == email.length() - 1) {
            return null;
        }
        return email.substring(atIndex + 1);
    }

    private void setCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}