package com.cena.traveloka.iam.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator implementation for ValidPhoneNumber annotation
 */
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    // E.164 format: +[country code][phone number] (max 15 digits total)
    private static final String E164_PATTERN = "^\\+[1-9]\\d{1,14}$";
    private static final Pattern E164_REGEX = Pattern.compile(E164_PATTERN);

    // Local format patterns for common countries
    private static final String INDONESIA_LOCAL = "^(0[8][0-9]{8,11})$"; // 08xxxxxxxxx
    private static final String US_LOCAL = "^([2-9][0-9]{2}[2-9][0-9]{6})$"; // 10 digits
    private static final String UK_LOCAL = "^(0[1-9][0-9]{8,9})$"; // 0xxxxxxxxx

    private Set<String> allowedCountries;
    private boolean requireInternational;

    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        this.allowedCountries = Set.of(constraintAnnotation.allowedCountries());
        this.requireInternational = constraintAnnotation.requireInternational();
    }

    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(phoneNumber)) {
            return true; // Let @NotBlank handle null/empty validation
        }

        // Remove spaces and hyphens for validation
        String cleanNumber = phoneNumber.replaceAll("[\\s-]", "");

        if (requireInternational) {
            return validateInternationalFormat(cleanNumber, context);
        } else {
            return validateAnyFormat(cleanNumber, context);
        }
    }

    private boolean validateInternationalFormat(String phoneNumber, ConstraintValidatorContext context) {
        if (!E164_REGEX.matcher(phoneNumber).matches()) {
            setCustomMessage(context, "Phone number must be in international format (+country code followed by number)");
            return false;
        }

        if (!allowedCountries.isEmpty()) {
            String countryCode = extractCountryCode(phoneNumber);
            if (!allowedCountries.contains(countryCode)) {
                setCustomMessage(context, "Phone number country code is not allowed");
                return false;
            }
        }

        return true;
    }

    private boolean validateAnyFormat(String phoneNumber, ConstraintValidatorContext context) {
        // Try international format first
        if (E164_REGEX.matcher(phoneNumber).matches()) {
            return validateInternationalFormat(phoneNumber, context);
        }

        // Try local formats
        if (Pattern.matches(INDONESIA_LOCAL, phoneNumber) ||
            Pattern.matches(US_LOCAL, phoneNumber) ||
            Pattern.matches(UK_LOCAL, phoneNumber)) {
            return true;
        }

        setCustomMessage(context, "Phone number format is not recognized");
        return false;
    }

    private String extractCountryCode(String phoneNumber) {
        if (!phoneNumber.startsWith("+")) {
            return null;
        }

        // Extract country code (1-3 digits after +)
        for (int i = 2; i <= Math.min(4, phoneNumber.length()); i++) {
            String possibleCode = phoneNumber.substring(1, i);
            if (isValidCountryCode(possibleCode)) {
                return possibleCode;
            }
        }

        return null;
    }

    private boolean isValidCountryCode(String code) {
        // Basic validation - country codes are 1-3 digits
        try {
            int codeNum = Integer.parseInt(code);
            return codeNum >= 1 && codeNum <= 999;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void setCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}