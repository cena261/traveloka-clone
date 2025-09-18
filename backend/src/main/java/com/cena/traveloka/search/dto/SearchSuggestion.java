package com.cena.traveloka.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SuggestionResponse {

    List<Suggestion> suggestions;

    @JsonProperty("response_metadata")
    ResponseMetadata responseMetadata;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Suggestion {
        String text;

        @JsonProperty("display_text")
        String displayText;

        SuggestionType type;

        Float score;

        @JsonProperty("match_indices")
        List<MatchIndex> matchIndices;

        SuggestionDetails details;

        @JsonProperty("is_popular")
        Boolean isPopular;

        @JsonProperty("search_count")
        Integer searchCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MatchIndex {
        Integer start;
        Integer end;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SuggestionDetails {
        // For location suggestions
        LocationDetails location;

        // For property suggestions
        PropertyDetails property;

        // For amenity suggestions
        AmenityDetails amenity;

        // For brand suggestions
        BrandDetails brand;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LocationDetails {
        String city;

        @JsonProperty("country_code")
        String countryCode;

        String region;

        @JsonProperty("location_type")
        String locationType;

        GeoLocation coordinates;

        @JsonProperty("property_count")
        Integer propertyCount;

        @JsonProperty("average_price")
        BigDecimal averagePrice;

        @JsonProperty("popular_months")
        List<Integer> popularMonths;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PropertyDetails {
        UUID id;

        @JsonProperty("property_type")
        String propertyType;

        @JsonProperty("star_rating")
        Integer starRating;

        LocationInfo location;

        @JsonProperty("rating_average")
        BigDecimal ratingAverage;

        @JsonProperty("review_count")
        Integer reviewCount;

        @JsonProperty("starting_price")
        BigDecimal startingPrice;

        String currency;

        @JsonProperty("is_featured")
        Boolean isFeatured;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AmenityDetails {
        UUID id;
        String category;
        String description;
        @JsonProperty("property_count")
        Integer propertyCount;
        String icon;
        @JsonProperty("is_premium")
        Boolean isPremium;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class BrandDetails {
        UUID id;
        @JsonProperty("brand_name")
        String brandName;
        @JsonProperty("property_count")
        Integer propertyCount;
        @JsonProperty("average_rating")
        BigDecimal averageRating;
        String logo;
        @JsonProperty("is_verified")
        Boolean isVerified;
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
        String district;
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
    public static class ResponseMetadata {
        @JsonProperty("response_time_ms")
        Long responseTimeMs;

        @JsonProperty("cache_hit")
        Boolean cacheHit;

        String language;

        @JsonProperty("suggestion_count")
        Integer suggestionCount;

        @JsonProperty("query_normalized")
        String queryNormalized;

        @JsonProperty("popular_searches")
        List<String> popularSearches;

        @JsonProperty("trending_destinations")
        List<String> trendingDestinations;

        @JsonProperty("autocomplete_session_id")
        String autocompleteSessionId;
    }

    public enum SuggestionType {
        LOCATION,
        PROPERTY,
        AMENITY,
        BRAND,
        KEYWORD,
        POPULAR_SEARCH,
        RECENT_SEARCH
    }
}
