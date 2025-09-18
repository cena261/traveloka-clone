package com.cena.traveloka.catalog.inventory.dto.response;

import com.cena.traveloka.catalog.inventory.entity.Property;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PropertyRes {
    UUID id;
    UUID partnerId;
    String propertyCode;
    Property.PropertyKind kind;
    String name;
    Map<String, String> description;
    String countryCode;
    String city;
    String addressLine;
    String postalCode;
    Double lat;
    Double lng;
    Integer starRating;
    Double averageRating;
    Integer totalReviews;
    Integer totalBookings;
    Property.PropertyStatus status;
    String timezone;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
