package com.cena.traveloka.iam.dto.response;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Response DTO for user profile information
 */
@Data
public class UserProfileRes {

    private String id;

    private String userId;

    private String displayName;

    private String bio;

    private String avatarUrl;

    private String nationality;

    private String occupation;

    private String company;

    private String address;

    private String city;

    private String state;

    private String postalCode;

    private String country;

    private String emergencyContact;

    private String emergencyContactName;

    private String emergencyContactRelationship;

    private LocalDate dateOfBirth;

    private Integer age;

    private String maritalStatus;

    private Map<String, Object> travelDocuments;

    private Map<String, Object> loyaltyPrograms;

    private Map<String, Object> customFields;

    private Boolean verified;

    private Instant verificationDate;

    private String verificationMethod;

    private Instant createdAt;

    private Instant updatedAt;

    private String createdBy;

    private String updatedBy;
}