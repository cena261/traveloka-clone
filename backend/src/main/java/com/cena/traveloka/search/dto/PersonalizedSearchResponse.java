package com.cena.traveloka.search.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@Jacksonized
public class PersonalizedSearchResponse {

    private List<PropertySearchResult> results;
    private List<Recommendation> recommendations;
    private PersonalizationInsights insights;
    private PaginationInfo pagination;
    private Map<String, Object> metadata;

    @Data
    @Builder
    @Jacksonized
    public static class Recommendation {

        private String type; // similar_properties, trending_destinations, price_deals, etc.
        private String title;
        private String description;
        private String imageUrl;
        private String actionUrl;

        // Recommendation specifics
        private RecommendationDetails details;

        // ML confidence and explanation
        private Double confidence; // 0.0 - 1.0
        private String reasoning;
        private List<String> factors; // what influenced this recommendation

        // Interaction tracking
        private String recommendationId;
        private Long displayOrder;
        private String trackingContext;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RecommendationDetails {

        // Property recommendations
        private PropertyRecommendation property;

        // Destination recommendations
        private DestinationRecommendation destination;

        // Deal recommendations
        private DealRecommendation deal;

        // Experience recommendations
        private ExperienceRecommendation experience;
    }

    @Data
    @Builder
    @Jacksonized
    public static class PropertyRecommendation {
        private String propertyId;
        private String propertyName;
        private String propertyType;
        private Integer starRating;
        private String location;
        private BigDecimal price;
        private String currency;
        private List<String> highlights;
        private BigDecimal rating;
        private Integer reviewCount;
    }

    @Data
    @Builder
    @Jacksonized
    public static class DestinationRecommendation {
        private String destinationName;
        private String countryCode;
        private String region;
        private String imageUrl;
        private String description;
        private List<String> attractions;
        private String bestTimeToVisit;
        private Integer averageStay;
        private String popularWith; // families, couples, business travelers
    }

    @Data
    @Builder
    @Jacksonized
    public static class DealRecommendation {
        private String dealType; // early_bird, last_minute, package, loyalty
        private String dealTitle;
        private BigDecimal originalPrice;
        private BigDecimal discountedPrice;
        private String currency;
        private Integer discountPercent;
        private String validUntil;
        private List<String> conditions;
        private String promotionCode;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ExperienceRecommendation {
        private String experienceType; // tour, activity, dining, spa
        private String title;
        private String description;
        private String location;
        private BigDecimal price;
        private String currency;
        private String duration;
        private BigDecimal rating;
        private List<String> includes;
    }

    @Data
    @Builder
    @Jacksonized
    public static class PersonalizationInsights {

        // User segment information
        private String userSegment;
        private BigDecimal personalizationScore;
        private String confidenceLevel; // low, medium, high

        // Applied personalization
        private List<String> appliedPreferences;
        private Map<String, Double> featureWeights;
        private List<PersonalizationFactor> factors;

        // User behavior insights
        private UserBehaviorInsights behaviorInsights;

        // Recommendations explanation
        private String explanationSummary;
        private List<String> keyInfluencers;

        // A/B testing information
        private String testGroup;
        private String personalizationVersion;
    }

    @Data
    @Builder
    @Jacksonized
    public static class PersonalizationFactor {
        private String factor;
        private String category;
        private Double weight;
        private String impact; // positive, negative, neutral
        private String description;
    }

    @Data
    @Builder
    @Jacksonized
    public static class UserBehaviorInsights {

        // Search behavior
        private String searchStyle; // explorer, focused, comparison_shopper
        private String decisionSpeed; // quick, deliberate, researcher
        private String priceOrientation; // budget_conscious, value_seeker, luxury_focused

        // Travel patterns
        private String travelFrequency; // frequent, occasional, rare
        private String bookingPattern; // early_planner, last_minute, flexible
        private String loyaltyLevel; // brand_loyal, switcher, explorer

        // Preferences strength
        private Map<String, String> preferenceStrength; // strong, moderate, weak

        // Seasonal patterns
        private List<String> preferredSeasons;
        private List<String> preferredDestinationTypes;

        // Device and channel preferences
        private String preferredChannel; // mobile, desktop, app
        private String preferredContactTime;
    }

    // Enhanced property search result with personalization data
    public static class PersonalizedPropertyResult extends PropertySearchResult {
        private BigDecimal personalizationScore;
        private List<String> personalizedHighlights;
        private String recommendationReason;
        private Map<String, Double> featureMatches;
        private Boolean isRecommended;
        private String personalizationTags;
    }
}