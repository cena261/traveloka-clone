package com.cena.traveloka.iam.dto.request;

import com.cena.traveloka.iam.validation.ValidEmail;
import com.cena.traveloka.iam.validation.ValidPhoneNumber;
import com.cena.traveloka.iam.validation.ValidTimezone;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * Request DTO for updating user profile information
 *
 * Includes comprehensive validation for all profile fields:
 * - Name validation with proper length and character restrictions
 * - Email validation with domain checks
 * - Phone number validation with international format support
 * - Date validation for birth date with age restrictions
 * - Address validation with sanitization
 */
@Data
public class UpdateUserProfileRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "First name can only contain letters, spaces, hyphens, and apostrophes")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Last name can only contain letters, spaces, hyphens, and apostrophes")
    private String lastName;

    @ValidEmail(maxLength = 255, allowDisposable = false)
    @NotBlank(message = "Email is required")
    private String email;

    @ValidPhoneNumber
    private String phoneNumber;

    @Past(message = "Birth date must be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @Size(max = 1, message = "Gender must be a single character")
    @Pattern(regexp = "^[MFO]$", message = "Gender must be M (Male), F (Female), or O (Other)")
    private String gender;

    @Size(max = 100, message = "Address line 1 cannot exceed 100 characters")
    private String addressLine1;

    @Size(max = 100, message = "Address line 2 cannot exceed 100 characters")
    private String addressLine2;

    @Size(min = 2, max = 50, message = "City must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "City can only contain letters, spaces, hyphens, and apostrophes")
    private String city;

    @Size(min = 2, max = 50, message = "State/Province must be between 2 and 50 characters")
    private String stateProvince;

    @Size(min = 2, max = 20, message = "Postal code must be between 2 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s-]+$", message = "Postal code can only contain alphanumeric characters, spaces, and hyphens")
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 2, message = "Country must be a 2-character ISO code")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be a valid 2-character ISO country code")
    private String country;

    @ValidTimezone
    private String timezone;

    @Size(max = 10, message = "Preferred language must not exceed 10 characters")
    @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "Language must be in format 'en' or 'en-US'")
    private String preferredLanguage;

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;

    @Size(max = 100, message = "Occupation cannot exceed 100 characters")
    private String occupation;

    @Size(max = 100, message = "Company cannot exceed 100 characters")
    private String company;

    @AssertTrue(message = "You must be at least 13 years old")
    public boolean isValidAge() {
        if (birthDate == null) {
            return true; // Let @Past handle null validation
        }
        return birthDate.isBefore(LocalDate.now().minusYears(13));
    }
}