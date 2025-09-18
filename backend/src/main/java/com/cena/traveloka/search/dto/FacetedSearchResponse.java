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
public class FacetedSearchResponse {

    private List<PropertySearchResult> results;
    private PaginationInfo pagination;
    private SearchFacets facets;
    private SearchMetadata metadata;
    private AppliedFilters appliedFilters;

    @Data
    @Builder
    @Jacksonized
    public static class SearchFacets {
        // Location facets
        private LocationFacets location;

        // Property facets
        private PropertyFacets property;

        // Price facets
        private PriceFacets price;

        // Amenity facets
        private AmenityFacets amenities;

        // Review facets
        private ReviewFacets reviews;

        // Availability facets
        private AvailabilityFacets availability;

        // Business facets
        private BusinessFacets business;
    }

    @Data
    @Builder
    @Jacksonized
    public static class LocationFacets {
        private List<FacetValue> cities;
        private List<FacetValue> regions;
        private List<FacetValue> countries;
        private List<FacetValue> neighborhoods;
        private List<FacetValue> landmarks;
        private List<DistanceFacet> distanceFromCenter;
        private List<PoiFacet> nearbyPois;
        private Map<String, List<FacetValue>> transportation;
    }

    @Data
    @Builder
    @Jacksonized
    public static class PropertyFacets {
        private List<FacetValue> propertyTypes;
        private List<FacetValue> starRatings;
        private List<FacetValue> chains;
        private List<FacetValue> brands;
        private List<FacetValue> themes;
        private List<FacetValue> styles;
        private List<RangeFacet> roomCounts;
        private List<FacetValue> accessibility;
        private List<FacetValue> certifications;
    }

    @Data
    @Builder
    @Jacksonized
    public static class PriceFacets {
        private List<RangeFacet> priceRanges;
        private List<FacetValue> currencies;
        private List<FacetValue> priceTypes;
        private List<FacetValue> promotions;
        private List<FacetValue> paymentMethods;
        private List<RangeFacet> discountRanges;
    }

    @Data
    @Builder
    @Jacksonized
    public static class AmenityFacets {
        private Map<String, List<FacetValue>> categories;
        private List<FacetValue> popularAmenities;
        private List<FacetValue> uniqueAmenities;
        private List<FacetValue> accessibilityFeatures;
        private List<FacetValue> businessAmenities;
        private List<FacetValue> leisureAmenities;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ReviewFacets {
        private List<RangeFacet> guestRatingRanges;
        private List<RangeFacet> reviewCountRanges;
        private Map<String, List<RangeFacet>> categoryRatings;
        private List<FacetValue> reviewSources;
        private List<FacetValue> reviewLanguages;
        private List<TimePeriodFacet> reviewRecency;
    }

    @Data
    @Builder
    @Jacksonized
    public static class AvailabilityFacets {
        private List<FacetValue> instantBooking;
        private List<FacetValue> cancellationPolicy;
        private List<RangeFacet> minStayRanges;
        private List<FacetValue> bookingFlexibility;
        private List<FacetValue> availabilityStatus;
    }

    @Data
    @Builder
    @Jacksonized
    public static class BusinessFacets {
        private List<FacetValue> businessTypes;
        private List<FacetValue> partners;
        private List<FacetValue> verificationLevels;
        private List<FacetValue> pricingModels;
        private List<RangeFacet> commissionRanges;
    }

    @Data
    @Builder
    @Jacksonized
    public static class FacetValue {
        private String key;
        private String label;
        private String displayName;
        private Long count;
        private Boolean selected;
        private String category;
        private Integer priority;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RangeFacet {
        private String key;
        private String label;
        private BigDecimal from;
        private BigDecimal to;
        private Long count;
        private Boolean selected;
        private String unit;
        private String format;
    }

    @Data
    @Builder
    @Jacksonized
    public static class DistanceFacet {
        private String label;
        private BigDecimal distanceKm;
        private Long count;
        private Boolean selected;
        private String centerPoint;
    }

    @Data
    @Builder
    @Jacksonized
    public static class PoiFacet {
        private String poiType;
        private String poiName;
        private BigDecimal distanceKm;
        private Long count;
        private String category;
        private Integer priority;
    }

    @Data
    @Builder
    @Jacksonized
    public static class TimePeriodFacet {
        private String period;
        private String label;
        private Long count;
        private Boolean selected;
        private Integer months;
    }

    @Data
    @Builder
    @Jacksonized
    public static class AppliedFilters {
        private Map<String, List<String>> activeFilters;
        private Integer totalFilterCount;
        private List<FilterBreadcrumb> breadcrumbs;
        private String clearAllUrl;
        private Map<String, String> removeFilterUrls;
    }

    @Data
    @Builder
    @Jacksonized
    public static class FilterBreadcrumb {
        private String category;
        private String value;
        private String displayName;
        private String removeUrl;
        private Boolean removable;
    }

    @Data
    @Builder
    @Jacksonized
    public static class SearchMetadata {
        private Long responseTimeMs;
        private String searchId;
        private Long totalResults;
        private String language;
        private Boolean cacheHit;
        private String sortBy;
        private Integer facetCount;
        private Map<String, Object> debugInfo;
    }
}