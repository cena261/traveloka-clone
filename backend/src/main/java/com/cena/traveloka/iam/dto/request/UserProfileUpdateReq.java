package com.cena.traveloka.iam.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

/**
 * Request DTO for updating user profile
 */
@Data
public class UserProfileUpdateReq {

    @Size(max = 200, message = "Display name must not exceed 200 characters")
    private String displayName;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    @Pattern(regexp = "^https?://.*", message = "Avatar URL must be a valid HTTP/HTTPS URL")
    private String avatarUrl;

    @Size(max = 3, message = "Nationality must be 2-3 character country code")
    private String nationality;

    @Size(max = 100, message = "Occupation must not exceed 100 characters")
    private String occupation;

    @Size(max = 100, message = "Company must not exceed 100 characters")
    private String company;

    @Size(max = 200, message = "Address must not exceed 200 characters")
    private String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    @Size(max = 3, message = "Country must be 2-3 character country code")
    private String country;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Emergency contact must be valid E.164 format")
    private String emergencyContact;

    @Size(max = 100, message = "Emergency contact name must not exceed 100 characters")
    private String emergencyContactName;

    @Size(max = 50, message = "Emergency contact relationship must not exceed 50 characters")
    private String emergencyContactRelationship;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 20, message = "Marital status must not exceed 20 characters")
    private String maritalStatus;

    private Map<String, Object> travelDocuments;

    private Map<String, Object> loyaltyPrograms;

    private Map<String, Object> customFields;

    private Boolean verified;
}