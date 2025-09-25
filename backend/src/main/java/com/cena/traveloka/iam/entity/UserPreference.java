package com.cena.traveloka.iam.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * UserPreference entity mapped to iam.user_preferences table
 * Matches PostgreSQL schema exactly from V9 migration
 */
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(schema = "iam", name = "user_preferences")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "bigserial")
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true, columnDefinition = "uuid")
    @NotNull
    private UUID userId;

    @Column(name = "language", columnDefinition = "text")
    private String language = "en-US";

    @Column(name = "timezone", columnDefinition = "text")
    private String timezone = "UTC";

    @Column(name = "currency", length = 3, columnDefinition = "char(3)")
    private String currency = "USD";

    @JdbcTypeCode(SqlTypes.JSON)
    @ElementCollection
    @CollectionTable(name = "user_notification_preferences",
                     joinColumns = @JoinColumn(name = "user_preference_id"),
                     schema = "iam")
    @MapKeyColumn(name = "preference_key")
    @Column(name = "preference_value", columnDefinition = "jsonb")
    private Map<String, Object> notificationPreferences = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @ElementCollection
    @CollectionTable(name = "user_booking_preferences",
                     joinColumns = @JoinColumn(name = "user_preference_id"),
                     schema = "iam")
    @MapKeyColumn(name = "preference_key")
    @Column(name = "preference_value", columnDefinition = "jsonb")
    private Map<String, Object> bookingPreferences = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @ElementCollection
    @CollectionTable(name = "user_privacy_settings",
                     joinColumns = @JoinColumn(name = "user_preference_id"),
                     schema = "iam")
    @MapKeyColumn(name = "setting_key")
    @Column(name = "setting_value", columnDefinition = "jsonb")
    private Map<String, Object> privacySettings = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @ElementCollection
    @CollectionTable(name = "user_accessibility_options",
                     joinColumns = @JoinColumn(name = "user_preference_id"),
                     schema = "iam")
    @MapKeyColumn(name = "option_key")
    @Column(name = "option_value", columnDefinition = "jsonb")
    private Map<String, Object> accessibilityOptions = Map.of();

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    @Generated(GenerationTime.INSERT)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", columnDefinition = "text")
    private String createdBy;

    @Column(name = "updated_by", columnDefinition = "text")
    private String updatedBy;

    // JPA relationship (optional)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private AppUser user;

//    // Constructors
//    public UserPreference() {}
//
//    // Getters and Setters
//    public Long getId() { return id; }
//    public void setId(Long id) { this.id = id; }
//
//    public UUID getUserId() { return userId; }
//    public void setUserId(UUID userId) { this.userId = userId; }
//
//    public String getLanguage() { return language; }
//    public void setLanguage(String language) { this.language = language; }
//
//    public String getTimezone() { return timezone; }
//    public void setTimezone(String timezone) { this.timezone = timezone; }
//
//    public String getCurrency() { return currency; }
//    public void setCurrency(String currency) { this.currency = currency; }
//
//    public Map<String, Object> getNotificationPreferences() { return notificationPreferences; }
//    public void setNotificationPreferences(Map<String, Object> notificationPreferences) { this.notificationPreferences = notificationPreferences; }
//
//    public Map<String, Object> getBookingPreferences() { return bookingPreferences; }
//    public void setBookingPreferences(Map<String, Object> bookingPreferences) { this.bookingPreferences = bookingPreferences; }
//
//    public Map<String, Object> getPrivacySettings() { return privacySettings; }
//    public void setPrivacySettings(Map<String, Object> privacySettings) { this.privacySettings = privacySettings; }
//
//    public Map<String, Object> getAccessibilityOptions() { return accessibilityOptions; }
//    public void setAccessibilityOptions(Map<String, Object> accessibilityOptions) { this.accessibilityOptions = accessibilityOptions; }
//
//    public OffsetDateTime getCreatedAt() { return createdAt; }
//    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
//
//    public OffsetDateTime getUpdatedAt() { return updatedAt; }
//    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
//
//    public String getCreatedBy() { return createdBy; }
//    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
//
//    public String getUpdatedBy() { return updatedBy; }
//    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
//
//    public AppUser getUser() { return user; }
//    public void setUser(AppUser user) { this.user = user; }
}