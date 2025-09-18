package com.cena.traveloka.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PropertySearchResponse {

    @JsonProperty("total_count")
    Long totalCount;

    @JsonProperty("page_info")
    PageInfo pageInfo;

    List<PropertyResult> properties;

    SearchMetadata metadata;

    @JsonProperty("aggregations")
    SearchAggregations aggregations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PageInfo {
        @JsonProperty("current_page")
        Integer currentPage;

        @JsonProperty("page_size")
        Integer pageSize;

        @JsonProperty("total_pages")
        Integer totalPages;

        @JsonProperty("has_next")
        Boolean hasNext;

        @JsonProperty("has_previous")
        Boolean hasPrevious;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PropertyResult {
        UUID id;

        String name;

        Map<String, String> description;

        @JsonProperty("property_type")
        String propertyType;

        @JsonProperty("star_rating")
        Integer starRating;

        LocationInfo location;

        @JsonProperty("rating_info")
        RatingInfo ratingInfo;

        @JsonProperty("price_info")
        PriceInfo priceInfo;

        List<AmenityInfo> amenities;

        List<ImageInfo> images;

        @JsonProperty("room_types")
        List<RoomTypeInfo> roomTypes;

        @JsonProperty("search_score")
        Float searchScore;

        @JsonProperty("distance_km")
        BigDecimal distanceKm;

        @JsonProperty("is_promoted")
        Boolean isPromoted;

        @JsonProperty("instant_book")
        Boolean instantBook;

        @JsonProperty("free_cancellation")
        Boolean freeCancellation;

        @JsonProperty("last_booking")
        String lastBooking;

        @JsonProperty("availability_status")
        String availabilityStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LocationInfo {
        String city;

        @JsonProperty("country_code")
        String countryCode;

        String address;

        @JsonProperty("postal_code")
        String postalCode;

        GeoLocation coordinates;

        @JsonProperty("nearby_landmarks")
        List<String> nearbyLandmarks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GeoLocation {
        BigDecimal latitude;
        BigDecimal longitude;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RatingInfo {
        @JsonProperty("average_rating")
        BigDecimal averageRating;

        @JsonProperty("total_reviews")
        Integer totalReviews;

        @JsonProperty("rating_breakdown")
        Map<String, Integer> ratingBreakdown;

        @JsonProperty("recent_reviews")
        List<ReviewSummary> recentReviews;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ReviewSummary {
        String reviewer;
        BigDecimal rating;
        String comment;
        @JsonProperty("review_date")
        OffsetDateTime reviewDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PriceInfo {
        @JsonProperty("base_price")
        BigDecimal basePrice;

        @JsonProperty("discounted_price")
        BigDecimal discountedPrice;

        String currency;

        @JsonProperty("price_per_night")
        Boolean pricePerNight;

        @JsonProperty("total_price")
        BigDecimal totalPrice;

        @JsonProperty("taxes_included")
        Boolean taxesIncluded;

        @JsonProperty("discount_percentage")
        Integer discountPercentage;

        @JsonProperty("price_trend")
        String priceTrend;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AmenityInfo {
        UUID id;
        String name;
        String category;
        @JsonProperty("is_featured")
        Boolean isFeatured;
        String icon;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ImageInfo {
        UUID id;
        String url;
        String alt;
        String type;
        @JsonProperty("is_primary")
        Boolean isPrimary;
        @JsonProperty("sort_order")
        Integer sortOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RoomTypeInfo {
        UUID id;
        String name;
        String description;
        @JsonProperty("max_occupancy")
        Integer maxOccupancy;
        @JsonProperty("available_rooms")
        Integer availableRooms;
        @JsonProperty("base_price")
        BigDecimal basePrice;
        String currency;
        List<String> amenities;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SearchMetadata {
        @JsonProperty("search_id")
        String searchId;

        @JsonProperty("response_time_ms")
        Long responseTimeMs;

        @JsonProperty("cache_hit")
        Boolean cacheHit;

        @JsonProperty("applied_filters")
        Map<String, Object> appliedFilters;

        @JsonProperty("search_suggestions")
        List<String> searchSuggestions;

        @JsonProperty("popular_destinations")
        List<String> popularDestinations;

        @JsonProperty("search_timestamp")
        OffsetDateTime searchTimestamp;

        @JsonProperty("language")
        String language;

        @JsonProperty("currency")
        String currency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SearchAggregations {
        @JsonProperty("price_ranges")
        List<PriceRangeAggregation> priceRanges;

        @JsonProperty("star_ratings")
        List<StarRatingAggregation> starRatings;

        @JsonProperty("property_types")
        List<PropertyTypeAggregation> propertyTypes;

        @JsonProperty("amenities")
        List<AmenityAggregation> amenities;

        @JsonProperty("locations")
        List<LocationAggregation> locations;

        @JsonProperty("guest_ratings")
        List<GuestRatingAggregation> guestRatings;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PriceRangeAggregation {
        @JsonProperty("min_price")
        BigDecimal minPrice;

        @JsonProperty("max_price")
        BigDecimal maxPrice;

        @JsonProperty("doc_count")
        Long docCount;

        String currency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class StarRatingAggregation {
        Integer rating;

        @JsonProperty("doc_count")
        Long docCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PropertyTypeAggregation {
        String type;

        @JsonProperty("doc_count")
        Long docCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AmenityAggregation {
        String name;

        String category;

        @JsonProperty("doc_count")
        Long docCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LocationAggregation {
        String city;

        @JsonProperty("country_code")
        String countryCode;

        @JsonProperty("doc_count")
        Long docCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GuestRatingAggregation {
        @JsonProperty("rating_range")
        String ratingRange;

        @JsonProperty("doc_count")
        Long docCount;
    }
}
