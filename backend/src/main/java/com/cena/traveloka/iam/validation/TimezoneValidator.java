package com.cena.traveloka.iam.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.Set;

/**
 * Validator implementation for ValidTimezone annotation
 */
public class TimezoneValidator implements ConstraintValidator<ValidTimezone, String> {

    @Override
    public void initialize(ValidTimezone constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String timezone, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(timezone)) {
            return true; // Let @NotBlank handle null/empty validation
        }

        try {
            // Try to parse the timezone ID
            ZoneId.of(timezone);
            return true;
        } catch (Exception e) {
            // Check if it's a valid timezone abbreviation or offset
            return isValidTimezoneFormat(timezone);
        }
    }

    private boolean isValidTimezoneFormat(String timezone) {
        // Accept common timezone formats
        Set<String> validFormats = Set.of(
            "UTC", "GMT", "Z",
            "UTC+0", "GMT+0", "UTC-0", "GMT-0"
        );

        if (validFormats.contains(timezone.toUpperCase())) {
            return true;
        }

        // Check for offset formats like +07:00, -05:30, +0900
        if (timezone.matches("^[+-]\\d{2}:?\\d{2}$")) {
            return true;
        }

        // Check for timezone abbreviations (basic check)
        if (timezone.matches("^[A-Z]{3,4}$")) {
            return isKnownTimezoneAbbreviation(timezone);
        }

        return false;
    }

    private boolean isKnownTimezoneAbbreviation(String abbrev) {
        // Common timezone abbreviations
        Set<String> knownAbbreviations = Set.of(
            "PST", "PDT", "MST", "MDT", "CST", "CDT", "EST", "EDT",
            "WIB", "WITA", "WIT", "JST", "KST", "SGT", "HKT",
            "BST", "CET", "EET", "IST", "AST", "AWST", "ACST", "AEST"
        );

        return knownAbbreviations.contains(abbrev.toUpperCase());
    }
}