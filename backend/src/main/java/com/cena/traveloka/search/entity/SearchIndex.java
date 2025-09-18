package com.cena.traveloka.search.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

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
@Document(indexName = "traveloka-properties")
@Setting(settingPath = "elasticsearch/property-index-settings.json")
@Mapping(mappingPath = "elasticsearch/property-index-mapping.json")
public class SearchIndex {

    @Id
    @JsonProperty("property_id")
    UUID propertyId;

    @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer", searchAnalyzer = "vietnamese_analyzer")
    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer"),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword),
            @InnerField(suffix = "suggest", type = FieldType.Search_As_You_Type, analyzer = "edge_ngram_analyzer")
        }
    )
    String name;

    @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer")
    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer"),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
        }
    )
    Map<String, String> description;

    @Field(type = FieldType.Keyword)
    String kind;

    @Field(type = FieldType.Integer)
    @JsonProperty("star_rating")
    Integer starRating;

    @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer")
    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer"),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword),
            @InnerField(suffix = "suggest", type = FieldType.Search_As_You_Type, analyzer = "edge_ngram_analyzer")
        }
    )
    String city;

    @Field(type = FieldType.Keyword)
    @JsonProperty("country_code")
    String countryCode;

    @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer")
    @JsonProperty("address_line")
    String addressLine;

    @Field(type = FieldType.Keyword)
    @JsonProperty("postal_code")
    String postalCode;

    @Field(type = FieldType.Object)
    LocationData location;

    @Field(type = FieldType.Text)
    @JsonProperty("phone_number")
    String phoneNumber;

    @Field(type = FieldType.Keyword)
    String email;

    @Field(type = FieldType.Keyword)
    String website;

    @Field(type = FieldType.Date, format = DateFormat.time)
    @JsonProperty("check_in_time")
    @JsonFormat(pattern = "HH:mm:ss")
    LocalTime checkInTime;

    @Field(type = FieldType.Date, format = DateFormat.time)
    @JsonProperty("check_out_time")
    @JsonFormat(pattern = "HH:mm:ss")
    LocalTime checkOutTime;

    @Field(type = FieldType.Integer)
    @JsonProperty("total_rooms")
    Integer totalRooms;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    @JsonProperty("rating_avg")
    BigDecimal ratingAvg;

    @Field(type = FieldType.Integer)
    @JsonProperty("rating_count")
    Integer ratingCount;

    @Field(type = FieldType.Keyword)
    String status;

    @Field(type = FieldType.Keyword)
    String timezone;

    @Field(type = FieldType.Nested)
    List<AmenityData> amenities;

    @Field(type = FieldType.Nested)
    List<ImageData> images;

    @Field(type = FieldType.Nested)
    @JsonProperty("room_types")
    List<RoomTypeData> roomTypes;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    OffsetDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    @JsonProperty("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    OffsetDateTime updatedAt;

    @Field(type = FieldType.Object)
    @JsonProperty("search_boost")
    SearchBoostData searchBoost;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LocationData {
        @Field(type = FieldType.GEO_POINT)
        GeoPointData coordinates;

        @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer")
        String city;

        @Field(type = FieldType.Keyword)
        @JsonProperty("country_code")
        String countryCode;

        @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer")
        String address;

        @Field(type = FieldType.Keyword)
        @JsonProperty("postal_code")
        String postalCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GeoPointData {
        @Field(type = FieldType.Double)
        Double lat;

        @Field(type = FieldType.Double)
        Double lon;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AmenityData {
        @Field(type = FieldType.Keyword)
        UUID id;

        @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer")
        @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer"),
            otherFields = {
                @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
        )
        String name;

        @Field(type = FieldType.Keyword)
        String category;

        @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer")
        String description;

        @Field(type = FieldType.Boolean)
        @JsonProperty("is_featured")
        Boolean isFeatured;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ImageData {
        @Field(type = FieldType.Keyword)
        UUID id;

        @Field(type = FieldType.Keyword)
        String url;

        @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer")
        String alt;

        @Field(type = FieldType.Keyword)
        String type;

        @Field(type = FieldType.Integer)
        @JsonProperty("sort_order")
        Integer sortOrder;

        @Field(type = FieldType.Boolean)
        @JsonProperty("is_primary")
        Boolean isPrimary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RoomTypeData {
        @Field(type = FieldType.Keyword)
        UUID id;

        @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer")
        @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer"),
            otherFields = {
                @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
        )
        String name;

        @Field(type = FieldType.Text, analyzer = "vietnamese_analyzer")
        String description;

        @Field(type = FieldType.Integer)
        @JsonProperty("max_occupancy")
        Integer maxOccupancy;

        @Field(type = FieldType.Integer)
        @JsonProperty("available_rooms")
        Integer availableRooms;

        @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
        @JsonProperty("base_price")
        BigDecimal basePrice;

        @Field(type = FieldType.Keyword)
        String currency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SearchBoostData {
        @Field(type = FieldType.Float)
        @JsonProperty("popularity_score")
        Float popularityScore;

        @Field(type = FieldType.Float)
        @JsonProperty("conversion_rate")
        Float conversionRate;

        @Field(type = FieldType.Float)
        @JsonProperty("review_score")
        Float reviewScore;

        @Field(type = FieldType.Integer)
        @JsonProperty("search_frequency")
        Integer searchFrequency;

        @Field(type = FieldType.Boolean)
        @JsonProperty("is_promoted")
        Boolean isPromoted;

        @Field(type = FieldType.Date, format = DateFormat.date_time)
        @JsonProperty("last_updated")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime lastUpdated;
    }
}
