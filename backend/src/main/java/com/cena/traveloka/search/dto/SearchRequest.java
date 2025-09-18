package com.cena.traveloka.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PropertySearchRequest {

    @NotBlank(message = "Search query is required")
    @Size(max = 500, message = "Search query cannot exceed 500 characters")
    String query;

    @Pattern(regexp = "^(vi|en)$", message = "Language must be 'vi' or 'en'")
    @Builder.Default
    String language = "vi";

    @Valid
    LocationFilter location;

    @Valid
    PriceFilter price;

    @Valid
    PropertyFilter property;

    @Valid
    GuestFilter guests;

    @Valid
    DateFilter dates;

    @Valid
    @Builder.Default
    PaginationRequest pagination = PaginationRequest.builder().build();

    @Pattern(regexp = "^(RELEVANCE|PRICE_LOW_TO_HIGH|PRICE_HIGH_TO_LOW|RATING|DISTANCE|POPULARITY)$",
             message = "Invalid sort option")
    @JsonProperty("sort_by")
    @Builder.Default
    String sortBy = "RELEVANCE";

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LocationFilter {
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        BigDecimal latitude;

        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        BigDecimal longitude;

        @DecimalMin(value = "0.1", message = "Radius must be at least 0.1 km")
        @DecimalMax(value = "1000.0", message = "Radius cannot exceed 1000 km")
        @JsonProperty("radius_km")
        BigDecimal radiusKm;

        String city;

        @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be 2 uppercase letters")
        @JsonProperty("country_code")
        String countryCode;

        String region;

        @JsonProperty("postal_code")
        String postalCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PriceFilter {
        @DecimalMin(value = "0.0", message = "Minimum price must be non-negative")
        @JsonProperty("min_price")
        BigDecimal minPrice;

        @DecimalMin(value = "0.0", message = "Maximum price must be non-negative")
        @JsonProperty("max_price")
        BigDecimal maxPrice;

        @Pattern(regexp = "^(VND|USD|EUR)$", message = "Currency must be VND, USD, or EUR")
        @Builder.Default
        String currency = "VND";

        @JsonProperty("per_night")
        @Builder.Default
        Boolean perNight = true;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PropertyFilter {
        @JsonProperty("star_rating")
        List<@Min(value = 1, message = "Star rating must be at least 1")
             @Max(value = 5, message = "Star rating cannot exceed 5") Integer> starRating;

        @JsonProperty("property_types")
        List<@Pattern(regexp = "^(hotel|homestay|villa|restaurant|meeting_room)$",
                     message = "Invalid property type") String> propertyTypes;

        List<String> amenities;

        @JsonProperty("guest_rating")
        GuestRatingFilter guestRating;

        @JsonProperty("instant_book")
        Boolean instantBook;

        @JsonProperty("free_cancellation")
        Boolean freeCancellation;

        @JsonProperty("verified_only")
        Boolean verifiedOnly;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GuestRatingFilter {
        @DecimalMin(value = "0.0", message = "Minimum rating must be between 0.0 and 5.0")
        @DecimalMax(value = "5.0", message = "Minimum rating must be between 0.0 and 5.0")
        @JsonProperty("min_rating")
        BigDecimal minRating;

        @Min(value = 1, message = "Minimum reviews must be at least 1")
        @JsonProperty("min_reviews")
        Integer minReviews;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GuestFilter {
        @Min(value = 1, message = "Number of adults must be at least 1")
        @Max(value = 20, message = "Number of adults cannot exceed 20")
        @Builder.Default
        Integer adults = 1;

        @Min(value = 0, message = "Number of children must be non-negative")
        @Max(value = 10, message = "Number of children cannot exceed 10")
        @Builder.Default
        Integer children = 0;

        @Min(value = 1, message = "Number of rooms must be at least 1")
        @Max(value = 10, message = "Number of rooms cannot exceed 10")
        @Builder.Default
        Integer rooms = 1;

        @JsonProperty("children_ages")
        List<@Min(value = 0, message = "Child age must be non-negative")
             @Max(value = 17, message = "Child age cannot exceed 17") Integer> childrenAges;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DateFilter {
        @JsonProperty("check_in")
        LocalDate checkIn;

        @JsonProperty("check_out")
        LocalDate checkOut;

        @JsonProperty("flexible_dates")
        Boolean flexibleDates;

        @JsonProperty("length_of_stay")
        Integer lengthOfStay;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PaginationRequest {
        @Min(value = 0, message = "Page number must be non-negative")
        @Builder.Default
        Integer page = 0;

        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size cannot exceed 100")
        @Builder.Default
        Integer size = 20;
    }
}
