package com.cena.traveloka.search.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;
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
@Table(name = "popular_destinations", schema = "search",
       uniqueConstraints = {
           @UniqueConstraint(name = "unique_destination_period",
                           columnNames = {"destination_name", "country_code", "period_start"})
       },
       indexes = {
           @Index(name = "popular_destinations_rank_idx", columnList = "popularity_rank"),
           @Index(name = "popular_destinations_period_idx", columnList = "period_start, period_end"),
           @Index(name = "popular_destinations_country_idx", columnList = "country_code"),
           @Index(name = "popular_destinations_type_idx", columnList = "destination_type"),
           @Index(name = "popular_destinations_trending_idx", columnList = "trending_score")
       })
public class PopularDestination {

    @Id
    @Column(nullable = false)
    @Builder.Default
    UUID id = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(name = "destination_type", nullable = false, length = 20)
    @NotNull(message = "Destination type is required")
    DestinationType destinationType;

    @Column(name = "destination_name", nullable = false, length = 200)
    @NotBlank(message = "Destination name is required")
    @Size(max = 200, message = "Destination name cannot exceed 200 characters")
    String destinationName;

    @Column(name = "country_code", nullable = false, length = 2)
    @NotBlank(message = "Country code is required")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be 2 uppercase letters")
    String countryCode;

    @Column(name = "coordinates", columnDefinition = "geography(Point,4326)")
    Point coordinates;

    @Column(name = "period_start", nullable = false)
    @NotNull(message = "Period start is required")
    OffsetDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    @NotNull(message = "Period end is required")
    OffsetDateTime periodEnd;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "search_metrics", nullable = false, columnDefinition = "jsonb")
    @NotNull(message = "Search metrics are required")
    @Builder.Default
    SearchMetrics searchMetrics = new SearchMetrics();

    @Column(name = "popularity_rank")
    @Min(value = 1, message = "Popularity rank must be at least 1")
    Integer popularityRank;

    @Column(name = "trending_score", precision = 10, scale = 4)
    @DecimalMin(value = "0.0", message = "Trending score must be non-negative")
    BigDecimal trendingScore;

    @Column(name = "business_value", precision = 15, scale = 2)
    @DecimalMin(value = "0.0", message = "Business value must be non-negative")
    BigDecimal businessValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (searchMetrics == null) {
            searchMetrics = new SearchMetrics();
        }
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum DestinationType {
        CITY, LANDMARK, REGION, AIRPORT, DISTRICT
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SearchMetrics {
        @JsonProperty("search_volume")
        @Builder.Default
        Integer searchVolume = 0;

        @JsonProperty("unique_users")
        @Builder.Default
        Integer uniqueUsers = 0;

        @JsonProperty("conversion_rate")
        @Builder.Default
        BigDecimal conversionRate = BigDecimal.ZERO;

        @JsonProperty("average_stay_duration")
        @Builder.Default
        BigDecimal averageStayDuration = BigDecimal.ZERO;

        @JsonProperty("seasonal_trends")
        @Builder.Default
        Map<String, Integer> seasonalTrends = Map.of();

        @JsonProperty("top_search_terms")
        @Builder.Default
        List<String> topSearchTerms = List.of();

        @JsonProperty("average_booking_value")
        BigDecimal averageBookingValue;

        @JsonProperty("repeat_visitor_rate")
        BigDecimal repeatVisitorRate;

        @JsonProperty("mobile_search_percentage")
        BigDecimal mobileSearchPercentage;

        @JsonProperty("international_visitor_percentage")
        BigDecimal internationalVisitorPercentage;

        @JsonProperty("peak_season_months")
        List<Integer> peakSeasonMonths;

        @JsonProperty("competitor_activity_score")
        BigDecimal competitorActivityScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DestinationAnalytics {
        @JsonProperty("growth_rate")
        BigDecimal growthRate;

        @JsonProperty("market_share")
        BigDecimal marketShare;

        @JsonProperty("demand_forecast")
        Map<String, BigDecimal> demandForecast;

        @JsonProperty("price_trend")
        Map<String, BigDecimal> priceTrend;

        @JsonProperty("supply_availability")
        Map<String, Integer> supplyAvailability;

        @JsonProperty("sentiment_score")
        BigDecimal sentimentScore;

        @JsonProperty("review_keywords")
        List<String> reviewKeywords;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CompetitiveMetrics {
        @JsonProperty("market_position")
        Integer marketPosition;

        @JsonProperty("share_of_voice")
        BigDecimal shareOfVoice;

        @JsonProperty("competitive_intensity")
        BigDecimal competitiveIntensity;

        @JsonProperty("differentiation_score")
        BigDecimal differentiationScore;

        @JsonProperty("opportunity_score")
        BigDecimal opportunityScore;
    }
}
