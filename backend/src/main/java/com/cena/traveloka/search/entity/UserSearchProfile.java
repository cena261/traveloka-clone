package com.cena.traveloka.search.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "user_search_profiles", schema = "search_analytics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class UserSearchProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    // Demographic preferences
    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage;

    @Column(name = "home_country", length = 3)
    private String homeCountry;

    @Column(name = "home_city", length = 100)
    private String homeCity;

    // Travel preferences
    @Embedded
    private TravelPreferences travelPreferences;

    // Property preferences
    @Embedded
    private PropertyPreferences propertyPreferences;

    // Price preferences
    @Embedded
    private PricePreferences pricePreferences;

    // Behavioral patterns
    @Embedded
    private BehavioralPatterns behavioralPatterns;

    // ML-generated features
    @Column(name = "preference_vector", columnDefinition = "json")
    private Map<String, Double> preferenceVector;

    @Column(name = "clustering_segment", length = 50)
    private String clusteringSegment;

    @Column(name = "personalization_score")
    private BigDecimal personalizationScore;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "last_interaction_at")
    private OffsetDateTime lastInteractionAt;

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TravelPreferences {

        @Column(name = "preferred_destinations", columnDefinition = "json")
        private Map<String, Integer> preferredDestinations; // destination -> frequency

        @Column(name = "travel_frequency")
        private String travelFrequency; // frequent, occasional, rare

        @Column(name = "trip_duration_preference")
        private String tripDurationPreference; // short, medium, long

        @Column(name = "travel_purpose", columnDefinition = "json")
        private Map<String, Double> travelPurpose; // business, leisure, family -> weight

        @Column(name = "seasonal_preference", columnDefinition = "json")
        private Map<String, Double> seasonalPreference; // spring, summer, etc. -> preference

        @Column(name = "group_size_preference")
        private Integer groupSizePreference;

        @Column(name = "advance_booking_days")
        private Integer advanceBookingDays;
    }

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyPreferences {

        @Column(name = "property_types", columnDefinition = "json")
        private Map<String, Double> propertyTypes; // hotel, villa, etc. -> preference weight

        @Column(name = "star_rating_preference", columnDefinition = "json")
        private Map<Integer, Double> starRatingPreference; // 1-5 stars -> preference

        @Column(name = "chain_preference", columnDefinition = "json")
        private Map<String, Double> chainPreference; // hotel chains -> preference

        @Column(name = "amenity_importance", columnDefinition = "json")
        private Map<String, Double> amenityImportance; // amenity -> importance weight

        @Column(name = "room_features", columnDefinition = "json")
        private Map<String, Double> roomFeatures; // wifi, balcony, etc. -> importance

        @Column(name = "accessibility_needs", columnDefinition = "json")
        private Map<String, Boolean> accessibilityNeeds;

        @Column(name = "sustainability_preference")
        private Double sustainabilityPreference; // 0.0 - 1.0

        @Column(name = "location_preference")
        private String locationPreference; // city_center, quiet, beach, etc.
    }

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricePreferences {

        @Column(name = "price_sensitivity")
        private Double priceSensitivity; // 0.0 - 1.0

        @Column(name = "preferred_price_range_min")
        private BigDecimal preferredPriceRangeMin;

        @Column(name = "preferred_price_range_max")
        private BigDecimal preferredPriceRangeMax;

        @Column(name = "currency_preference", length = 3)
        private String currencyPreference;

        @Column(name = "deal_seeking_behavior")
        private Double dealSeekingBehavior; // 0.0 - 1.0

        @Column(name = "promotion_responsiveness")
        private Double promotionResponsiveness; // 0.0 - 1.0

        @Column(name = "price_vs_quality_balance")
        private Double priceVsQualityBalance; // 0.0 (price focused) - 1.0 (quality focused)

        @Column(name = "booking_timing_preference")
        private String bookingTimingPreference; // early_bird, last_minute, flexible
    }

    @Embeddable
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BehavioralPatterns {

        @Column(name = "search_frequency")
        private Integer searchFrequency; // searches per month

        @Column(name = "avg_session_duration")
        private Integer avgSessionDuration; // minutes

        @Column(name = "pages_per_session")
        private Integer pagesPerSession;

        @Column(name = "click_through_rate")
        private Double clickThroughRate; // 0.0 - 1.0

        @Column(name = "conversion_rate")
        private Double conversionRate; // 0.0 - 1.0

        @Column(name = "favorite_search_time", columnDefinition = "json")
        private Map<Integer, Double> favoriteSearchTime; // hour -> frequency

        @Column(name = "device_preference", columnDefinition = "json")
        private Map<String, Double> devicePreference; // mobile, desktop -> usage

        @Column(name = "filter_usage_pattern", columnDefinition = "json")
        private Map<String, Double> filterUsagePattern; // filter type -> usage frequency

        @Column(name = "sort_preference", columnDefinition = "json")
        private Map<String, Double> sortPreference; // sort type -> preference

        @Column(name = "comparison_behavior")
        private Double comparisonBehavior; // tendency to compare options
    }

    public enum SegmentType {
        LUXURY_TRAVELER,
        BUDGET_CONSCIOUS,
        BUSINESS_TRAVELER,
        FAMILY_TRAVELER,
        ADVENTURE_SEEKER,
        COMFORT_SEEKER,
        DEAL_HUNTER,
        SPONTANEOUS_BOOKER,
        PLANNER,
        LOCATION_FOCUSED
    }
}