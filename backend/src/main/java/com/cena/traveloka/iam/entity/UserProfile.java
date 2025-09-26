package com.cena.traveloka.iam.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "user_profiles", schema = "iam")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfile {
    @Id
    @Column(name="user_id")
    java.util.UUID userId;

    @MapsId
    @OneToOne
    @JoinColumn(name="user_id")
    User user;

    String middleName;
    String nickname;
    @Column(columnDefinition = "text") String bio;
    String occupation;
    String company;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    JsonNode notificationPreferences;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    JsonNode privacySettings;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    JsonNode accessibilitySettings;

    String website;
    String facebookUrl;
    String twitterUrl;
    String linkedinUrl;
    String instagramUrl;

    @ElementCollection
    @CollectionTable(name="user_profile_pref_airlines", schema="iam", joinColumns=@JoinColumn(name="user_id"))
    @Column(name="airline")
    List<String> preferredAirlines;

    @ElementCollection
    @CollectionTable(name="user_profile_pref_hotels", schema="iam", joinColumns=@JoinColumn(name="user_id"))
    @Column(name="hotel")
    List<String> preferredHotels;

    @ElementCollection
    @CollectionTable(name="user_profile_dietary", schema="iam", joinColumns=@JoinColumn(name="user_id"))
    @Column(name="item")
    List<String> dietaryRestrictions;

    @Column(columnDefinition = "text") String specialNeeds;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    JsonNode frequentFlyerNumbers;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    JsonNode hotelLoyaltyPrograms;

    Integer totalBookings = 0;
    java.math.BigDecimal totalSpent; // common.price -> BigDecimal
    java.time.LocalDate memberSince;
    Integer loyaltyPoints = 0;
    String loyaltyTier = "bronze";

    Integer profileCompletionPercentage = 0;
    OffsetDateTime lastProfileUpdate;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
