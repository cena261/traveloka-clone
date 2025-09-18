package com.cena.traveloka.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchAnalyticsRequest {

    @NotNull(message = "Session ID is required")
    @JsonProperty("session_id")
    UUID sessionId;

    @JsonProperty("user_id")
    UUID userId;

    @NotBlank(message = "Search query is required")
    @Size(max = 500, message = "Search query cannot exceed 500 characters")
    @JsonProperty("search_query")
    String searchQuery;

    @NotNull(message = "Search type is required")
    @JsonProperty("search_type")
    SearchType searchType;

    @Valid
    @Builder.Default
    Map<String, Object> filters = Map.of();

    @Valid
    @JsonProperty("location_context")
    LocationContext locationContext;

    @Valid
    @JsonProperty("search_results")
    SearchResultsInfo searchResultsInfo;

    @Valid
    @JsonProperty("user_interactions")
    UserInteractionInfo userInteractionInfo;

    @Valid
    @JsonProperty("device_context")
    DeviceContext deviceContext;

    @JsonProperty("search_timestamp")
    @Builder.Default
    OffsetDateTime searchTimestamp = OffsetDateTime.now();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LocationContext {
        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        BigDecimal latitude;

        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        BigDecimal longitude;

        @DecimalMin(value = "0.1", message = "Search radius must be at least 0.1 km")
        @JsonProperty("search_radius_km")
        BigDecimal searchRadiusKm;

        @JsonProperty("detected_location")
        @Builder.Default
        Boolean detectedLocation = false;

        @JsonProperty("ip_country")
        String ipCountry;

        @JsonProperty("ip_city")
        String ipCity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SearchResultsInfo {
        @Min(value = 0, message = "Total results must be non-negative")
        @JsonProperty("total_results")
        @Builder.Default
        Integer totalResults = 0;

        @Min(value = 0, message = "Response time must be non-negative")
        @JsonProperty("response_time_ms")
        Integer responseTimeMs;

        @JsonProperty("cache_hit")
        Boolean cacheHit;

        @JsonProperty("elasticsearch_took_ms")
        Integer elasticsearchTookMs;

        @JsonProperty("zero_results")
        @Builder.Default
        Boolean zeroResults = false;

        @JsonProperty("results_page")
        @Builder.Default
        Integer resultsPage = 0;

        @JsonProperty("results_per_page")
        @Builder.Default
        Integer resultsPerPage = 20;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UserInteractionInfo {
        @JsonProperty("clicked_property_ids")
        List<UUID> clickedPropertyIds;

        @JsonProperty("time_to_first_click_ms")
        Integer timeToFirstClickMs;

        @JsonProperty("booking_completed")
        @Builder.Default
        Boolean bookingCompleted = false;

        @JsonProperty("conversion_value")
        @DecimalMin(value = "0.0", message = "Conversion value must be non-negative")
        BigDecimal conversionValue;

        @JsonProperty("conversion_currency")
        String conversionCurrency;

        @JsonProperty("property_views")
        List<PropertyViewInfo> propertyViews;

        @JsonProperty("filters_changed")
        @Builder.Default
        Integer filtersChanged = 0;

        @JsonProperty("search_refinements")
        @Builder.Default
        Integer searchRefinements = 0;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PropertyViewInfo {
        @NotNull(message = "Property ID is required")
        @JsonProperty("property_id")
        UUID propertyId;

        @JsonProperty("view_duration_ms")
        Integer viewDurationMs;

        @JsonProperty("click_position")
        Integer clickPosition;

        @JsonProperty("viewed_details")
        Boolean viewedDetails;

        @JsonProperty("shared")
        Boolean shared;

        @JsonProperty("favorited")
        Boolean favorited;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DeviceContext {
        @JsonProperty("device_type")
        DeviceType deviceType;

        @JsonProperty("user_agent")
        String userAgent;

        @JsonProperty("screen_resolution")
        String screenResolution;

        @JsonProperty("browser")
        String browser;

        @JsonProperty("operating_system")
        String operatingSystem;

        @JsonProperty("is_mobile")
        Boolean isMobile;

        @JsonProperty("connection_type")
        String connectionType;

        @JsonProperty("language")
        @Builder.Default
        String language = "vi";

        @JsonProperty("timezone")
        String timezone;
    }

    public enum SearchType {
        FULL_TEXT, LOCATION_BASED, SUGGESTION, FILTER_ONLY
    }

    public enum DeviceType {
        MOBILE, DESKTOP, TABLET
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
class SearchAnalyticsResponse {

    @JsonProperty("analytics_id")
    UUID analyticsId;

    @JsonProperty("processed_timestamp")
    OffsetDateTime processedTimestamp;

    @JsonProperty("processing_status")
    ProcessingStatus processingStatus;

    @JsonProperty("insights")
    SearchInsights insights;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SearchInsights {
        @JsonProperty("search_quality_score")
        BigDecimal searchQualityScore;

        @JsonProperty("relevance_score")
        BigDecimal relevanceScore;

        @JsonProperty("user_satisfaction_prediction")
        BigDecimal userSatisfactionPrediction;

        @JsonProperty("recommendations")
        List<String> recommendations;

        @JsonProperty("similar_searches")
        List<String> similarSearches;

        @JsonProperty("performance_metrics")
        PerformanceMetrics performanceMetrics;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PerformanceMetrics {
        @JsonProperty("response_time_percentile")
        String responseTimePercentile;

        @JsonProperty("cache_effectiveness")
        BigDecimal cacheEffectiveness;

        @JsonProperty("result_quality_index")
        BigDecimal resultQualityIndex;
    }

    public enum ProcessingStatus {
        RECEIVED, PROCESSING, COMPLETED, ERROR
    }
}
