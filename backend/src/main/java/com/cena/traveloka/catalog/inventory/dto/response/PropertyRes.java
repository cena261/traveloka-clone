package com.cena.traveloka.catalog.inventory.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PropertyRes {
    UUID id;
    UUID partnerId;

    String kind;
    String name;
    String description;

    String countryCode;
    String city;
    String addressLine;
    String postalCode;

    Double lat;
    Double lng;

    BigDecimal ratingAvg;   // NUMERIC(3,2) -> BigDecimal
    Integer ratingCount;

    String status;          // draft|active|inactive
    String timezone;

    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
