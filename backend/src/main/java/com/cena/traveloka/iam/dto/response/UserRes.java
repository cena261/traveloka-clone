package com.cena.traveloka.iam.dto.response;

import com.cena.traveloka.iam.entity.AppUser;
import com.cena.traveloka.iam.enums.UserStatus;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Response DTO for user information
 */
@Data
public class UserRes {

    private String id;

    private String email;

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private LocalDate dateOfBirth;

    private String gender;

    private String country;

    private String preferredLanguage;

    private String timeZone;

    private UserStatus status;

    private Boolean emailVerified;

    private Boolean phoneVerified;

    private Integer profileCompleteness;

    private Instant lastLoginAt;

    private String lastLoginIp;

    private Instant lastSyncAt;

    private Instant createdAt;

    private Instant updatedAt;

    private String createdBy;

    private String updatedBy;
}