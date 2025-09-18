package com.cena.traveloka.catalog.inventory.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PropertyCodeValidator implements ConstraintValidator<ValidPropertyCode, String> {

    private static final Pattern PROPERTY_CODE_PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{2,19}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        return PROPERTY_CODE_PATTERN.matcher(value.trim()).matches();
    }
}