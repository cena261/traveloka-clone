package com.cena.traveloka.search.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@Jacksonized
public class RecommendationRequest {

    // User context
    private String userId;
    private String sessionId;
    private String deviceType;
    private UserProfile userProfile;

    // Current search context
    private SearchContext searchContext;

    // Recommendation preferences
    private RecommendationPreferences preferences;

    // Historical data context
    private HistoricalContext historicalContext;

    // Real-time context
    private RealtimeContext realtimeContext;

    // Business rules
    private BusinessContext businessContext;

    @Data
    @Builder
    @Jacksonized
    public static class UserProfile {
        private String userSegment;
        private List<String> interests;
        private List<String> preferences;
        private PricePreference pricePreference;
        private LocationPreference locationPreference;
        private TravelStyle travelStyle;
        private Map<String, Object> demographicData;
        private List<String> previousDestinations;
        private BigDecimal averageSpending;
        private String loyaltyTier;
    }

    @Data
    @Builder
    @Jacksonized
    public static class PricePreference {
        private String priceRange; // budget, mid-range, luxury, premium
        private BigDecimal minBudget;
        private BigDecimal maxBudget;
        private String currency;
        private Boolean priceFlexible;
        private String valueOrientation; // price_focused, value_focused, luxury_focused
    }

    @Data
    @Builder
    @Jacksonized
    public static class LocationPreference {
        private List<String> preferredRegions;
        private List<String> avoidedRegions;
        private String locationStyle; // city_center, suburban, resort, remote
        private Boolean proximityImportant;
        private List<String> preferredAmenities;
    }

    @Data
    @Builder
    @Jacksonized
    public static class TravelStyle {
        private String travelType; // business, leisure, family, romantic, adventure
        private String planningStyle; // spontaneous, planned, flexible
        private String accommodationStyle; // luxury, comfort, basic, unique
        private List<String> activityPreferences;
        private Boolean groupTravel;
        private Integer groupSize;
    }

    @Data
    @Builder
    @Jacksonized
    public static class SearchContext {
        private String currentQuery;
        private String destination;
        private OffsetDateTime checkInDate;
        private OffsetDateTime checkOutDate;
        private Integer numberOfGuests;
        private Integer numberOfRooms;
        private List<PropertySearchResult> currentResults;
        private List<String> appliedFilters;
        private String sortOrder;
        private Integer currentPage;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RecommendationPreferences {
        private List<RecommendationType> requestedTypes;
        private Integer maxRecommendations;
        private Boolean enablePersonalization;
        private Boolean enableTrendingRecommendations;
        private Boolean enableSimilarPropertyRecommendations;
        private Boolean enableAlternativeDestinations;
        private Boolean enableUpgradeRecommendations;
        private Double diversityFactor; // 0.0 = similar results, 1.0 = diverse results
        private String recommendationStrategy; // collaborative, content_based, hybrid, trending
    }

    @Data
    @Builder
    @Jacksonized
    public static class HistoricalContext {
        private List<SearchHistory> recentSearches;
        private List<BookingHistory> bookingHistory;
        private List<ViewHistory> viewHistory;
        private Map<String, Integer> categoryPreferences;
        private Map<String, Double> featureWeights;
        private OffsetDateTime lookbackPeriod;
    }

    @Data
    @Builder
    @Jacksonized
    public static class SearchHistory {
        private String query;
        private String destination;
        private OffsetDateTime searchDate;
        private List<String> clickedProperties;
        private String finalAction; // booked, abandoned, continued
        private Map<String, Object> searchFilters;
    }

    @Data
    @Builder
    @Jacksonized
    public static class BookingHistory {
        private String propertyId;
        private String propertyName;
        private String destination;
        private OffsetDateTime bookingDate;
        private OffsetDateTime stayDate;
        private BigDecimal paidAmount;
        private String currency;
        private Double rating;
        private String review;
        private List<String> likedFeatures;
        private List<String> dislikedFeatures;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ViewHistory {
        private String propertyId;
        private OffsetDateTime viewDate;
        private Long viewDuration; // milliseconds
        private List<String> viewedSections;
        private Boolean addedToWishlist;
        private Boolean shared;
        private String exitAction;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RealtimeContext {
        private String currentSeason;
        private List<String> trendingDestinations;
        private Map<String, Object> marketConditions;
        private List<String> popularAmenities;
        private Map<String, Double> demandMetrics;
        private WeatherContext weatherContext;
        private EventContext eventContext;
    }

    @Data
    @Builder
    @Jacksonized
    public static class WeatherContext {
        private String currentWeather;
        private String forecast;
        private List<String> weatherBasedRecommendations;
        private Boolean isWeatherSensitive;
    }

    @Data
    @Builder
    @Jacksonized
    public static class EventContext {
        private List<String> upcomingEvents;
        private List<String> localFestivals;
        private List<String> businessEvents;
        private Boolean hasEventImpact;
    }

    @Data
    @Builder
    @Jacksonized
    public static class BusinessContext {
        private List<String> promotionalProperties;
        private List<String> featuredPartners;
        private Map<String, BigDecimal> commissionRates;
        private List<String> inventoryPriorities;
        private Boolean enableRevenueOptimization;
        private String businessObjective; // revenue, conversion, retention, acquisition
        private Map<String, Object> campaignContext;
    }

    public enum RecommendationType {
        SIMILAR_PROPERTIES,
        ALTERNATIVE_DESTINATIONS,
        UPGRADE_SUGGESTIONS,
        NEARBY_ATTRACTIONS,
        COMPLEMENTARY_SERVICES,
        TRENDING_NOW,
        PERSONALIZED_PICKS,
        PRICE_ALERTS,
        SEASONAL_RECOMMENDATIONS,
        EVENT_BASED_RECOMMENDATIONS,
        LAST_MINUTE_DEALS,
        EARLY_BIRD_OFFERS
    }

    public enum RecommendationStrategy {
        COLLABORATIVE_FILTERING,  // Based on similar users
        CONTENT_BASED,           // Based on property features
        HYBRID,                  // Combination of both
        TRENDING,                // Based on current trends
        PERSONALIZED,            // Based on user history
        BUSINESS_OPTIMIZED,      // Based on business rules
        CONTEXTUAL,              // Based on current context
        REAL_TIME               // Based on real-time data
    }
}