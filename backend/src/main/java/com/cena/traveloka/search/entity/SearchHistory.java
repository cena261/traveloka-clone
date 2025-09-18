package com.cena.traveloka.search.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "search_history", schema = "search", indexes = {
    @Index(name = "search_history_user_idx", columnList = "user_id"),
    @Index(name = "search_history_session_idx", columnList = "session_id"),
    @Index(name = "search_history_timestamp_idx", columnList = "search_timestamp"),
    @Index(name = "search_history_type_idx", columnList = "search_type"),
    @Index(name = "search_history_conversion_idx", columnList = "booking_completed, conversion_value")
})
public class SearchHistory {

    @Id
    @Column(nullable = false)
    @Builder.Default
    UUID id = UUID.randomUUID();

    @Column(name = "session_id", nullable = false)
    @NotNull(message = "Session ID is required")
    UUID sessionId;

    @Column(name = "user_id")
    UUID userId;

    @Column(name = "search_query", columnDefinition = "text")
    String searchQuery;

    @Enumerated(EnumType.STRING)
    @Column(name = "search_type", nullable = false, length = 50)
    @NotNull(message = "Search type is required")
    SearchType searchType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filters", columnDefinition = "jsonb")
    @Builder.Default
    Map<String, Object> filters = Map.of();

    @Column(name = "search_location", columnDefinition = "geography(Point,4326)")
    Point searchLocation;

    @Column(name = "search_radius", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Search radius must be non-negative")
    BigDecimal searchRadius;

    @Column(name = "detected_location")
    @Builder.Default
    Boolean detectedLocation = false;

    @Column(name = "total_results")
    @Min(value = 0, message = "Total results must be non-negative")
    @Builder.Default
    Integer totalResults = 0;

    @Column(name = "response_time_ms")
    @Min(value = 0, message = "Response time must be non-negative")
    Integer responseTimeMs;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "clicked_property_ids", columnDefinition = "uuid[]")
    UUID[] clickedPropertyIds;

    @Column(name = "booking_completed")
    @Builder.Default
    Boolean bookingCompleted = false;

    @Column(name = "conversion_value", precision = 12, scale = 2)
    @DecimalMin(value = "0.0", message = "Conversion value must be non-negative")
    BigDecimal conversionValue;

    @Column(name = "search_timestamp", nullable = false)
    @NotNull(message = "Search timestamp is required")
    @Builder.Default
    OffsetDateTime searchTimestamp = OffsetDateTime.now();

    @Column(name = "user_agent", columnDefinition = "text")
    String userAgent;

    @Column(length = 10)
    @Builder.Default
    String language = "vi";

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 20)
    DeviceType deviceType;

    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (searchTimestamp == null) {
            searchTimestamp = OffsetDateTime.now();
        }
        if (language == null) {
            language = "vi";
        }
        if (detectedLocation == null) {
            detectedLocation = false;
        }
        if (totalResults == null) {
            totalResults = 0;
        }
        if (bookingCompleted == null) {
            bookingCompleted = false;
        }
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum SearchType {
        FULL_TEXT, LOCATION_BASED, SUGGESTION, FILTER_ONLY
    }

    public enum DeviceType {
        MOBILE, DESKTOP, TABLET
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SearchFilters {
        @JsonProperty("price_range")
        PriceRangeFilter priceRange;

        @JsonProperty("star_rating")
        List<Integer> starRating;

        List<String> amenities;

        @JsonProperty("property_types")
        List<String> propertyTypes;

        @JsonProperty("guest_rating")
        GuestRatingFilter guestRating;

        @JsonProperty("distance_km")
        BigDecimal distanceKm;

        @JsonProperty("check_in")
        String checkIn;

        @JsonProperty("check_out")
        String checkOut;

        Integer guests;

        Integer rooms;

        @JsonProperty("sort_by")
        String sortBy;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        @FieldDefaults(level = AccessLevel.PRIVATE)
        public static class PriceRangeFilter {
            @JsonProperty("min_price")
            BigDecimal minPrice;

            @JsonProperty("max_price")
            BigDecimal maxPrice;

            String currency;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        @FieldDefaults(level = AccessLevel.PRIVATE)
        public static class GuestRatingFilter {
            @JsonProperty("min_rating")
            BigDecimal minRating;

            @JsonProperty("min_reviews")
            Integer minReviews;
        }
    }
}
