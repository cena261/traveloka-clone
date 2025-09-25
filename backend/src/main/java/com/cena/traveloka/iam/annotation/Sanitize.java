package com.cena.traveloka.iam.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to control input sanitization behavior
 *
 * Can be applied to:
 * - Fields to specify sanitization type
 * - Methods to enable/disable sanitization
 * - Classes to set default sanitization behavior
 * - Parameters to control parameter sanitization
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Sanitize {

    /**
     * Type of sanitization to apply
     */
    SanitizationType value() default SanitizationType.TEXT;

    /**
     * Whether to enable sanitization (can be used to disable)
     */
    boolean enabled() default true;

    /**
     * Whether to log sanitization activities
     */
    boolean logSanitization() default false;

    /**
     * Custom sanitization rules
     */
    String[] customRules() default {};

    /**
     * Maximum length after sanitization
     */
    int maxLength() default -1;

    /**
     * Whether to validate input after sanitization
     */
    boolean validateAfterSanitization() default true;

    /**
     * Enumeration of sanitization types
     */
    enum SanitizationType {
        TEXT,           // General text sanitization
        EMAIL,          // Email-specific sanitization
        PHONE,          // Phone number sanitization
        URL,            // URL sanitization
        NAME,           // Name sanitization (first name, last name)
        SEARCH,         // Search query sanitization
        HTML,           // HTML content sanitization
        NONE            // No sanitization
    }
}