package com.cena.traveloka.iam.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * T029: RegisterRequest DTO
 * Request DTO for user registration (FR-001).
 *
 * Password policy (NFR-001):
 * - Minimum 8 characters
 * - At least 1 uppercase letter
 * - At least 1 lowercase letter
 * - At least 1 number
 * - At least 1 special character
 *
 * Constitutional Compliance:
 * - Principle X: Code Quality - Bean Validation for input validation
 * - Used by AuthController POST /api/v1/auth/register
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /**
     * Username (3-100 characters, unique).
     */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscores and hyphens")
    private String username;

    /**
     * Email address (must be valid format, unique).
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /**
     * Password (min 8 chars, must meet complexity requirements).
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$",
        message = "Password must contain at least 1 uppercase, 1 lowercase, 1 number, and 1 special character"
    )
    private String password;

    /**
     * Password confirmation (must match password).
     */
    @NotBlank(message = "Password confirmation is required")
    private String passwordConfirmation;

    /**
     * First name.
     */
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    /**
     * Last name.
     */
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    /**
     * Optional phone number (Vietnamese format).
     */
    @Pattern(regexp = "^\\+84[0-9]{9,10}$", message = "Phone must be in Vietnamese format (+84xxxxxxxxx)")
    private String phone;

    /**
     * Optional preferred language (en, vi).
     */
    @Pattern(regexp = "^(en|vi)$", message = "Language must be 'en' or 'vi'")
    private String preferredLanguage;

    /**
     * Terms and conditions acceptance.
     */
    @AssertTrue(message = "You must accept the terms and conditions")
    private Boolean acceptTerms;
}
