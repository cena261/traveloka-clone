package com.cena.traveloka.iam.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for timezone strings
 * Validates timezone identifiers (e.g., "Asia/Jakarta", "UTC")
 */
@Documented
@Constraint(validatedBy = TimezoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTimezone {

    String message() default "Invalid timezone identifier";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}