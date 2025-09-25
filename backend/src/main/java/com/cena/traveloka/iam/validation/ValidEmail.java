package com.cena.traveloka.iam.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for email addresses
 * Provides enhanced email validation beyond the standard @Email annotation
 */
@Documented
@Constraint(validatedBy = EmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmail {

    String message() default "Email address is not valid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Whether to allow corporate email addresses only
     */
    boolean corporateOnly() default false;

    /**
     * Whether to allow disposable email addresses
     */
    boolean allowDisposable() default true;

    /**
     * Maximum length for email address
     */
    int maxLength() default 255;
}