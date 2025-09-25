package com.cena.traveloka.iam.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for phone numbers
 * Validates phone numbers in E.164 format
 */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhoneNumber {

    String message() default "Phone number is not valid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Allowed country codes (empty means all allowed)
     */
    String[] allowedCountries() default {};

    /**
     * Whether international format is required (+country code)
     */
    boolean requireInternational() default true;
}