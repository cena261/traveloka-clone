package com.cena.traveloka.iam.validation;

import com.cena.traveloka.iam.dto.request.AssignUserRoleRequest;
import org.springframework.util.StringUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Validator implementation for ValidUserRole annotation
 */
public class UserRoleValidator implements ConstraintValidator<ValidUserRole, Object> {

    private static final Set<String> SYSTEM_ROLES = Set.of(
            "SYSTEM_ADMIN", "SUPER_USER", "ROOT"
    );

    private static final Set<String> VALID_ROLES = Set.of(
            "USER", "PREMIUM_USER", "BUSINESS_USER",
            "MODERATOR", "ADMIN", "SUPPORT_AGENT",
            "PARTNER", "VENDOR", "GUEST"
    );

    private boolean validateHierarchy;
    private boolean checkPermissions;
    private int maxRoles;
    private List<String> allowedRolePatterns;

    @Override
    public void initialize(ValidUserRole constraintAnnotation) {
        this.validateHierarchy = constraintAnnotation.validateHierarchy();
        this.checkPermissions = constraintAnnotation.checkPermissions();
        this.maxRoles = constraintAnnotation.maxRoles();
        this.allowedRolePatterns = Arrays.asList(constraintAnnotation.allowedRolePatterns());
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }

        // Handle different input types
        if (value instanceof String) {
            return validateSingleRole((String) value, context);
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) value;
            return validateRoleList(roles, context);
        } else if (value instanceof AssignUserRoleRequest) {
            return validateRoleAssignmentRequest((AssignUserRoleRequest) value, context);
        }

        return false;
    }

    private boolean validateSingleRole(String role, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(role)) {
            return true; // Let @NotBlank handle empty validation
        }

        // Check if role is in allowed patterns
        if (!allowedRolePatterns.isEmpty() && !allowedRolePatterns.contains(role)) {
            setCustomMessage(context, "Role '" + role + "' is not in allowed role patterns");
            return false;
        }

        // Check if role is valid
        if (!VALID_ROLES.contains(role)) {
            setCustomMessage(context, "Role '" + role + "' is not a valid role");
            return false;
        }

        // Check if system role (not allowed for regular assignment)
        if (SYSTEM_ROLES.contains(role)) {
            setCustomMessage(context, "System role '" + role + "' cannot be assigned through this interface");
            return false;
        }

        return true;
    }

    private boolean validateRoleList(List<String> roles, ConstraintValidatorContext context) {
        if (roles.isEmpty()) {
            setCustomMessage(context, "At least one role must be specified");
            return false;
        }

        // Check maximum roles limit
        if (roles.size() > maxRoles) {
            setCustomMessage(context, "Cannot assign more than " + maxRoles + " roles to a user");
            return false;
        }

        // Validate each role
        for (String role : roles) {
            if (!validateSingleRole(role, context)) {
                return false;
            }
        }

        // Check for conflicting roles
        if (validateHierarchy && hasConflictingRoles(roles)) {
            setCustomMessage(context, "Conflicting roles detected in assignment");
            return false;
        }

        // Check for duplicate roles
        if (roles.size() != roles.stream().distinct().count()) {
            setCustomMessage(context, "Duplicate roles are not allowed");
            return false;
        }

        return true;
    }

    private boolean validateRoleAssignmentRequest(AssignUserRoleRequest request, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(request.getUserId())) {
            setCustomMessage(context, "User ID is required for role assignment");
            return false;
        }

        if (!StringUtils.hasText(request.getRoleName())) {
            setCustomMessage(context, "Role name must be specified");
            return false;
        }

        return validateRoleList(List.of(request.getRoleName()), context);
    }

    private boolean hasConflictingRoles(List<String> roles) {
        // Define conflicting role combinations
        boolean hasAdminRole = roles.stream().anyMatch(role -> role.contains("ADMIN"));
        boolean hasGuestRole = roles.contains("GUEST");
        boolean hasUserRole = roles.stream().anyMatch(role -> role.contains("USER"));

        // Admin and Guest roles are conflicting
        if (hasAdminRole && hasGuestRole) {
            return true;
        }

        // Check for multiple admin-level roles
        long adminRoleCount = roles.stream()
                .filter(role -> role.contains("ADMIN") || role.equals("MODERATOR"))
                .count();

        return adminRoleCount > 1;
    }

    private void setCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}