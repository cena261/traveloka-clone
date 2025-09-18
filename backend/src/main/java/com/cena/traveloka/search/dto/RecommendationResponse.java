package com.cena.traveloka.search.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@Jacksonized
public class RecommendationResponse {

    // Recommended items
    private List<PropertyRecommendation> propertyRecommendations;
    private List<DestinationRecommendation> destinationRecommendations;
    private List<ServiceRecommendation> serviceRecommendations;
    private List<DealRecommendation> dealRecommendations;

    // Recommendation metadata
    private RecommendationMetadata metadata;

    // Performance metrics
    private RecommendationMetrics metrics;

    // Explanation and insights
    private RecommendationExplanation explanation;

    @Data
    @Builder
    @Jacksonized
    public static class PropertyRecommendation {
        private String recommendationId;
        private PropertySearchResult property;
        private RecommendationType type;
        private Double score;
        private Double confidence;
        private String reasoning;
        private List<String> matchingFeatures;
        private RecommendationContext context;
        private PricingInfo pricing;
        private AvailabilityInfo availability;
        private Integer rank;
        private Map<String, Object> additionalData;
    }

    @Data
    @Builder
    @Jacksonized
    public static class DestinationRecommendation {
        private String recommendationId;
        private String destinationName;
        private String country;
        private String region;
        private String description;
        private String imageUrl;
        private RecommendationType type;
        private Double score;
        private String reasoning;
        private List<String> attractions;
        private List<String> activities;
        private PriceRange priceRange;
        private SeasonalInfo seasonalInfo;
        private Integer rank;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ServiceRecommendation {
        private String recommendationId;
        private String serviceType; // transportation, tours, dining, spa
        private String serviceName;
        private String description;
        private String provider;
        private BigDecimal price;
        private String currency;
        private Double rating;
        private RecommendationType type;
        private Double score;
        private String reasoning;
        private BookingInfo bookingInfo;
        private Integer rank;
    }

    @Data
    @Builder
    @Jacksonized
    public static class DealRecommendation {
        private String recommendationId;
        private String dealType; // discount, package, early_bird, last_minute
        private String title;
        private String description;
        private BigDecimal originalPrice;
        private BigDecimal discountedPrice;
        private String currency;
        private Integer discountPercentage;
        private OffsetDateTime validFrom;
        private OffsetDateTime validUntil;
        private List<String> conditions;
        private String promoCode;
        private RecommendationType type;
        private Double score;
        private String reasoning;
        private Integer rank;
        private UrgencyIndicator urgency;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RecommendationContext {
        private String contextType; // similar_user, trending, seasonal, personal_history
        private Map<String, Object> contextData;
        private Double contextRelevance;
        private List<String> contextFactors;
    }

    @Data
    @Builder
    @Jacksonized
    public static class PricingInfo {
        private BigDecimal basePrice;
        private BigDecimal totalPrice;
        private String currency;
        private List<PriceComponent> priceBreakdown;
        private String priceCategory; // budget, mid-range, luxury
        private Boolean hasDiscount;
        private BigDecimal savingsAmount;
        private String savingsType;
    }

    @Data
    @Builder
    @Jacksonized
    public static class PriceComponent {
        private String component; // room_rate, taxes, fees, services
        private BigDecimal amount;
        private String description;
        private Boolean isOptional;
    }

    @Data
    @Builder
    @Jacksonized
    public static class AvailabilityInfo {
        private Boolean isAvailable;
        private Integer availableRooms;
        private OffsetDateTime lastUpdated;
        private String availabilityStatus; // high, medium, low, limited
        private List<DateRange> availableDates;
        private String bookingWindow; // immediate, advance, restricted
    }

    @Data
    @Builder
    @Jacksonized
    public static class DateRange {
        private OffsetDateTime startDate;
        private OffsetDateTime endDate;
        private String availabilityLevel;
    }

    @Data
    @Builder
    @Jacksonized
    public static class PriceRange {
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private String currency;
        private String priceLevel; // $, $$, $$$, $$$$
    }

    @Data
    @Builder
    @Jacksonized
    public static class SeasonalInfo {
        private String bestTimeToVisit;
        private String currentSeason;
        private List<String> seasonalHighlights;
        private WeatherInfo weather;
    }

    @Data
    @Builder
    @Jacksonized
    public static class WeatherInfo {
        private String currentWeather;
        private String forecast;
        private Integer temperature;
        private String temperatureUnit;
    }

    @Data
    @Builder
    @Jacksonized
    public static class BookingInfo {
        private Boolean isBookable;
        private String bookingUrl;
        private String contactInfo;
        private List<String> bookingMethods;
        private Boolean requiresAdvanceBooking;
        private String cancellationPolicy;
    }

    @Data
    @Builder
    @Jacksonized
    public static class UrgencyIndicator {
        private String urgencyLevel; // low, medium, high, critical
        private String urgencyReason;
        private OffsetDateTime deadline;
        private Integer remainingQuantity;
        private String urgencyMessage;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RecommendationMetadata {
        private String algorithmVersion;
        private List<String> appliedStrategies;
        private Map<String, Double> strategyWeights;
        private OffsetDateTime generatedAt;
        private Long generationTimeMs;
        private String requestId;
        private Integer totalRecommendations;
        private Boolean wasPersonalized;
        private String personalizationLevel;
        private Map<String, Object> debugInfo;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RecommendationMetrics {
        private Double averageScore;
        private Double confidenceLevel;
        private Double diversityIndex;
        private Double noveltyScore;
        private Double coverageScore;
        private Integer totalCandidates;
        private Integer filteredCandidates;
        private Map<String, Integer> recommendationsByType;
        private QualityMetrics quality;
    }

    @Data
    @Builder
    @Jacksonized
    public static class QualityMetrics {
        private Double relevanceScore;
        private Double freshnessScore;
        private Double availabilityScore;
        private Double priceCompetitiveness;
        private Double userAlignmentScore;
        private String overallQuality; // excellent, good, fair, poor
        private List<String> qualityFactors;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RecommendationExplanation {
        private String primaryReason;
        private List<String> contributingFactors;
        private UserInsights userInsights;
        private MarketInsights marketInsights;
        private PersonalizationInsights personalizationInsights;
        private String recommendationSummary;
        private List<ActionableInsight> actionableInsights;
    }

    @Data
    @Builder
    @Jacksonized
    public static class UserInsights {
        private String userSegment;
        private List<String> identifiedPreferences;
        private List<String> behaviorPatterns;
        private String travelPersona;
        private Map<String, Double> preferenceSimilarity;
    }

    @Data
    @Builder
    @Jacksonized
    public static class MarketInsights {
        private List<String> currentTrends;
        private List<String> seasonalFactors;
        private List<String> popularDestinations;
        private Map<String, String> priceComparisons;
        private String marketCondition;
    }

    @Data
    @Builder
    @Jacksonized
    public static class PersonalizationInsights {
        private String personalizationStrategy;
        private Map<String, Double> featureImportance;
        private List<String> personalizedFactors;
        private Double personalizationConfidence;
        private String personalizationExplanation;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ActionableInsight {
        private String insightType; // price_alert, booking_urgency, alternative_option
        private String title;
        private String description;
        private String action;
        private String priority;
        private Map<String, Object> actionData;
    }

    // Enhanced recommendation types with specific context
    @Data
    @Builder
    @Jacksonized
    public static class TrendingRecommendation {
        private String trendType; // viral_destination, seasonal_hotspot, emerging_location
        private String trendDescription;
        private Double trendStrength;
        private String trendSource;
        private OffsetDateTime trendStartDate;
    }

    @Data
    @Builder
    @Jacksonized
    public static class SimilarUserRecommendation {
        private String userSegment;
        private Integer similarUserCount;
        private Double similarityScore;
        private List<String> commonPreferences;
        private String recommendationBasis;
    }

    @Data
    @Builder
    @Jacksonized
    public static class SeasonalRecommendation {
        private String season;
        private String seasonalAppeal;
        private List<String> seasonalActivities;
        private WeatherInfo optimalWeather;
        private String timingAdvice;
    }

    @Data
    @Builder
    @Jacksonized
    public static class LocationBasedRecommendation {
        private Double distanceKm;
        private String transportOptions;
        private String locationContext;
        private List<String> nearbyAttractions;
        private String accessibilityInfo;
    }

    public enum RecommendationType {
        SIMILAR_PROPERTIES,
        TRENDING_NOW,
        PERSONALIZED_PICK,
        ALTERNATIVE_DESTINATION,
        UPGRADE_SUGGESTION,
        DEAL_ALERT,
        SEASONAL_RECOMMENDATION,
        SIMILAR_USER_CHOICE,
        LOCATION_BASED,
        COMPLEMENTARY_SERVICE,
        LAST_MINUTE_OFFER,
        EARLY_BIRD_SPECIAL
    }

    public enum RecommendationQuality {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR
    }

    public enum PersonalizationLevel {
        HIGHLY_PERSONALIZED,
        MODERATELY_PERSONALIZED,
        LIGHTLY_PERSONALIZED,
        GENERIC
    }
}