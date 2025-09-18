package com.cena.traveloka.catalog.inventory.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CoordinatesValidator implements ConstraintValidator<ValidCoordinates, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }

        try {
            // This would be used on DTOs with latitude and longitude fields
            // For now, we'll implement basic validation logic
            if (value instanceof Double) {
                Double coordinate = (Double) value;
                return coordinate >= -180.0 && coordinate <= 180.0;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}