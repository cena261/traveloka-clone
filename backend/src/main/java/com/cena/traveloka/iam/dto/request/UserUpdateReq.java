package com.cena.traveloka.iam.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request DTO for updating user information
 */
@Data
public class UserUpdateReq {

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid E.164 format")
    private String phoneNumber;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 10, message = "Gender code must not exceed 10 characters")
    private String gender;

    @Size(max = 3, message = "Country code must be 2-3 characters")
    private String country;

    @Size(max = 10, message = "Language code must not exceed 10 characters")
    private String preferredLanguage;

    @Size(max = 50, message = "Time zone must not exceed 50 characters")
    private String timeZone;

    private Boolean emailVerified;

    private Boolean phoneVerified;
}