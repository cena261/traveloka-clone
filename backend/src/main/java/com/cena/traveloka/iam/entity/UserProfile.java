package com.cena.traveloka.iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * UserProfile entity mapped to iam.user_profiles table
 * Matches PostgreSQL schema exactly from V9 migration
 */
@Entity
@Table(schema = "iam", name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "bigserial")
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, columnDefinition = "uuid")
    @NotNull
    private UUID userId;

    @Column(name = "date_of_birth", columnDefinition = "date")
    private LocalDate dateOfBirth;

    @Column(name = "gender", columnDefinition = "text")
    private String gender;

    @Column(name = "nationality", length = 2, columnDefinition = "char(2)")
    private String nationality;

    @Column(name = "passport_number", columnDefinition = "text")
    private String passportNumber;

    @Column(name = "emergency_contact_name", columnDefinition = "text")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", columnDefinition = "text")
    private String emergencyContactPhone;

    @Column(name = "bio", columnDefinition = "text")
    private String bio;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Column(name = "verification_status", nullable = false, columnDefinition = "text")
    private String verificationStatus = "UNVERIFIED";

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    @Generated(GenerationTime.INSERT)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", columnDefinition = "text")
    private String createdBy;

    @Column(name = "updated_by", columnDefinition = "text")
    private String updatedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @ElementCollection
    @CollectionTable(name = "user_profile_data",
                     joinColumns = @JoinColumn(name = "user_profile_id"),
                     schema = "iam")
    @MapKeyColumn(name = "data_key")
    @Column(name = "data_value", columnDefinition = "jsonb")
    private Map<String, Object> profileData;

    @Column(name = "verified_at", columnDefinition = "timestamptz")
    private OffsetDateTime verifiedAt;

    // JPA relationship (optional)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private AppUser user;

    // Constructors
    public UserProfile() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public Map<String, Object> getProfileData() { return profileData; }
    public void setProfileData(Map<String, Object> profileData) { this.profileData = profileData; }

    public OffsetDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(OffsetDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
}