package com.cena.traveloka.iam.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for user roles
 *
 * Validates:
 * - Role name format and length
 * - Role hierarchy constraints
 * - Valid role assignments
 * - Permission level validation
 */
@Documented
@Constraint(validatedBy = UserRoleValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUserRole {

    String message() default "Invalid user role assignment";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Whether to validate role hierarchy
     */
    boolean validateHierarchy() default true;

    /**
     * Whether to check role permissions
     */
    boolean checkPermissions() default true;

    /**
     * Maximum number of roles per user
     */
    int maxRoles() default 10;

    /**
     * Allowed role patterns
     */
    String[] allowedRolePatterns() default {"USER", "ADMIN", "MODERATOR", "GUEST"};
}