package com.cena.traveloka.catalog.inventory.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PropertyCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPropertyCode {
    String message() default "Property code must be 3-20 characters, start with letter/number, and contain only letters, numbers, underscores, and hyphens";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}