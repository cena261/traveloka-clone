package com.cena.traveloka.iam.dto.request;

import com.cena.traveloka.common.enums.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * T030: UpdateProfileRequest DTO
 * Request DTO for updating user profile (FR-019).
 *
 * Constitutional Compliance:
 * - Principle X: Code Quality - Bean Validation for input validation
 * - FR-018: Vietnamese phone number validation
 * - Used by UserController PUT /api/v1/users/me
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    /**
     * First name.
     */
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    /**
     * Last name.
     */
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    /**
     * Display name.
     */
    @Size(max = 200, message = "Display name must not exceed 200 characters")
    private String displayName;

    /**
     * Phone number (Vietnamese format - FR-018).
     */
    @Pattern(regexp = "^\\+84[0-9]{9,10}$", message = "Phone must be in Vietnamese format (+84xxxxxxxxx)")
    private String phone;

    /**
     * Date of birth (must be in the past).
     */
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    /**
     * Gender.
     */
    private Gender gender;

    /**
     * Nationality (ISO 3166-1 alpha-2 code).
     */
    @Pattern(regexp = "^[A-Z]{2}$", message = "Nationality must be a 2-letter country code")
    private String nationality;

    /**
     * Preferred language (en, vi).
     */
    @Pattern(regexp = "^(en|vi)$", message = "Language must be 'en' or 'vi'")
    private String preferredLanguage;

    /**
     * Preferred currency (ISO 4217 code).
     */
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter currency code")
    private String preferredCurrency;

    /**
     * Timezone (IANA timezone).
     */
    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;

    /**
     * Avatar URL.
     */
    private String avatarUrl;
}
