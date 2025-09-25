package com.cena.traveloka.iam.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for password complexity
 * Note: In OAuth2/Keycloak setup, this might be used for local password changes
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "Password does not meet complexity requirements";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Minimum password length
     */
    int minLength() default 8;

    /**
     * Maximum password length
     */
    int maxLength() default 128;

    /**
     * Require at least one uppercase letter
     */
    boolean requireUppercase() default true;

    /**
     * Require at least one lowercase letter
     */
    boolean requireLowercase() default true;

    /**
     * Require at least one digit
     */
    boolean requireDigit() default true;

    /**
     * Require at least one special character
     */
    boolean requireSpecialChar() default true;

    /**
     * Allowed special characters
     */
    String allowedSpecialChars() default "!@#$%^&*()_+-=[]{}|;:,.<>?";

    /**
     * Disallow common passwords
     */
    boolean disallowCommon() default true;
}