package com.cena.traveloka.iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import com.cena.traveloka.iam.enums.UserStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(schema = "iam", name = "users")
public class AppUser {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    @Generated(GenerationTime.INSERT)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true, columnDefinition = "text")
    @NotNull
    private String username;

    @Column(name = "email", unique = true, columnDefinition = "text")
    private String email;

    @Column(name = "display_name", columnDefinition = "text")
    private String displayName;

    @Column(name = "locale", columnDefinition = "text")
    private String locale = "vi";

    @Column(name = "is_active", nullable = false, columnDefinition = "boolean")
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    @Generated(GenerationTime.INSERT)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    // V9 additions
    @Column(name = "keycloak_id", unique = true, columnDefinition = "text")
    private String keycloakId;

    @Column(name = "first_name", columnDefinition = "text")
    private String firstName;

    @Column(name = "last_name", columnDefinition = "text")
    private String lastName;

    @Column(name = "phone_number", columnDefinition = "text")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "text")
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "profile_completeness", columnDefinition = "integer")
    private Integer profileCompleteness = 0;

    @Column(name = "last_sync_at", columnDefinition = "timestamptz")
    private OffsetDateTime lastSyncAt;

    // Constructors
    public AppUser() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getKeycloakId() { return keycloakId; }
    public void setKeycloakId(String keycloakId) { this.keycloakId = keycloakId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public Integer getProfileCompleteness() { return profileCompleteness; }
    public void setProfileCompleteness(Integer profileCompleteness) { this.profileCompleteness = profileCompleteness; }

    public OffsetDateTime getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(OffsetDateTime lastSyncAt) { this.lastSyncAt = lastSyncAt; }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    // Additional methods expected by service classes
    public void updateProfileCompleteness() {
        int completeness = 0;
        if (firstName != null && !firstName.trim().isEmpty()) completeness += 20;
        if (lastName != null && !lastName.trim().isEmpty()) completeness += 20;
        if (email != null && !email.trim().isEmpty()) completeness += 20;
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) completeness += 20;
        if (displayName != null && !displayName.trim().isEmpty()) completeness += 20;
        this.profileCompleteness = completeness;
    }

    public void setCreatedBy(String createdBy) {
        // This method is expected by service code but not implemented as field
        // Could store in metadata or ignore
    }

    public void setUpdatedBy(String updatedBy) {
        // This method is expected by service code but not implemented as field
        // Could store in metadata or ignore
    }

    public void markSynced() {
        this.lastSyncAt = OffsetDateTime.now();
    }

    public void softDelete() {
        this.status = UserStatus.DELETED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.updatedAt = OffsetDateTime.now();
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
        this.updatedAt = OffsetDateTime.now();
    }

    // Methods expected by AuthenticationService
    public void setLastLoginAt(java.time.Instant lastLoginAt) {
        // Convert Instant to OffsetDateTime and store in updatedAt for now
        // Since there's no lastLoginAt field in the schema
        this.updatedAt = lastLoginAt.atOffset(java.time.ZoneOffset.UTC);
    }

    public void setLastLoginIp(String lastLoginIp) {
        // This method is expected but not implemented as field
        // Could store in metadata or ignore
    }

    public java.time.Instant getLastLoginAt() {
        // Return updatedAt as Instant since we don't have separate lastLoginAt field
        return updatedAt != null ? updatedAt.toInstant() : null;
    }
}