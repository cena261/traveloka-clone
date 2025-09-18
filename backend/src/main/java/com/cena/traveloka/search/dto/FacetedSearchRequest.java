package com.cena.traveloka.search.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@Jacksonized
public class FacetedSearchRequest {

    private String query;
    private String language;
    private Pageable pageable;

    // Advanced location filters
    private AdvancedLocationFilter location;

    // Enhanced property filters
    private AdvancedPropertyFilter property;

    // Pricing filters
    private AdvancedPriceFilter price;

    // Availability filters
    private AvailabilityFilter availability;

    // Review and rating filters
    private ReviewFilter reviews;

    // Business filters
    private BusinessFilter business;

    // Facet configuration
    private FacetConfiguration facets;

    @Data
    @Builder
    @Jacksonized
    public static class AdvancedLocationFilter {
        // Geographic coordinates
        private BigDecimal latitude;
        private BigDecimal longitude;
        private BigDecimal radiusKm;

        // Administrative divisions
        private String city;
        private String region;
        private String countryCode;
        private String postalCode;

        // Points of Interest
        private List<PoiFilter> nearbyPois;
        private BigDecimal poiRadiusKm;

        // Transportation
        private List<TransportationFilter> transportation;

        // Geographic boundaries
        private List<GeographicBoundary> boundaries;
    }

    @Data
    @Builder
    @Jacksonized
    public static class PoiFilter {
        private String type; // airport, train_station, shopping_mall, beach, etc.
        private String name;
        private BigDecimal maxDistanceKm;
        private Integer priority; // 1-5, higher = more important
    }

    @Data
    @Builder
    @Jacksonized
    public static class TransportationFilter {
        private String type; // airport, train, bus, metro
        private String name;
        private BigDecimal maxDistanceKm;
        private Boolean shuttleService;
    }

    @Data
    @Builder
    @Jacksonized
    public static class GeographicBoundary {
        private String type; // polygon, circle, rectangle
        private List<Coordinate> coordinates;
        private String description;
    }

    @Data
    @Builder
    @Jacksonized
    public static class Coordinate {
        private BigDecimal latitude;
        private BigDecimal longitude;
    }

    @Data
    @Builder
    @Jacksonized
    public static class AdvancedPropertyFilter {
        // Property characteristics
        private List<String> propertyTypes;
        private List<Integer> starRating;
        private List<String> chains; // hotel chains
        private List<String> brands;

        // Room and capacity
        private Integer minRooms;
        private Integer maxRooms;
        private Integer minGuests;
        private Integer maxGuests;

        // Amenities with categories
        private Map<String, List<String>> amenitiesByCategory;
        private List<String> requiredAmenities;
        private List<String> preferredAmenities;

        // Accessibility
        private List<String> accessibility;
        private Boolean wheelchairAccessible;

        // Property features
        private List<String> themes; // luxury, business, family, eco-friendly
        private List<String> styles; // modern, traditional, boutique
        private Boolean petFriendly;
        private Boolean adultOnly;
        private Boolean familyFriendly;

        // Sustainability
        private List<String> certifications; // green certifications
        private Boolean ecoFriendly;
    }

    @Data
    @Builder
    @Jacksonized
    public static class AdvancedPriceFilter {
        // Price range
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private String currency;
        private Boolean perNight;

        // Price types
        private List<String> priceTypes; // room_only, breakfast_included, all_inclusive
        private Boolean includesTaxes;
        private Boolean includesFees;

        // Promotions and discounts
        private Boolean onSale;
        private Integer minDiscountPercent;
        private List<String> promotionTypes;
        private Boolean memberDealsOnly;

        // Payment options
        private List<String> paymentMethods;
        private Boolean freeBooking;
        private Boolean payLater;
        private Boolean freeCancellation;
    }

    @Data
    @Builder
    @Jacksonized
    public static class AvailabilityFilter {
        // Check-in/out dates
        private OffsetDateTime checkIn;
        private OffsetDateTime checkOut;
        private Integer nights;

        // Guest requirements
        private GuestRequirements guests;

        // Booking requirements
        private Integer minNightsStay;
        private Integer maxNightsStay;
        private List<String> dayOfWeek; // available days

        // Instant booking
        private Boolean instantBooking;
        private Integer maxBookingLeadTime; // days in advance
        private Integer minBookingLeadTime;
    }

    @Data
    @Builder
    @Jacksonized
    public static class GuestRequirements {
        private Integer adults;
        private Integer children;
        private List<Integer> childrenAges;
        private Integer infants;
        private Integer rooms;
        private Boolean petsAllowed;
        private Integer petCount;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ReviewFilter {
        // Rating filters
        private BigDecimal minGuestRating;
        private BigDecimal maxGuestRating;
        private Integer minReviewCount;

        // Review categories
        private Map<String, BigDecimal> categoryRatings; // cleanliness, service, location, etc.

        // Review recency
        private OffsetDateTime reviewsSince;
        private Boolean recentReviewsOnly; // last 12 months

        // Review sources
        private List<String> reviewSources;
        private Boolean verifiedReviewsOnly;

        // Language preferences
        private List<String> reviewLanguages;
    }

    @Data
    @Builder
    @Jacksonized
    public static class BusinessFilter {
        // Business model
        private List<String> businessTypes; // hotel, ota, direct
        private Boolean directBookingOnly;
        private Boolean partnerPropertiesOnly;

        // Verification status
        private Boolean verifiedOnly;
        private List<String> verificationLevels;

        // Partner preferences
        private List<String> preferredPartners;
        private List<String> excludedPartners;

        // Commission and pricing
        private BigDecimal minCommissionRate;
        private List<String> pricingModels;
    }

    @Data
    @Builder
    @Jacksonized
    public static class FacetConfiguration {
        // Which facets to include
        private List<String> enabledFacets;

        // Facet limits
        private Integer maxFacetValues;
        private Integer minFacetCount;

        // Facet sorting
        private Map<String, String> facetSorting; // count, value, index

        // Hierarchical facets
        private Boolean enableHierarchy;
        private List<String> hierarchicalFacets;

        // Range facets
        private Map<String, RangeFacetConfig> rangeFacets;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RangeFacetConfig {
        private BigDecimal start;
        private BigDecimal end;
        private BigDecimal gap;
        private List<String> ranges; // predefined ranges
    }
}