package com.cena.traveloka.inventory.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PropertyRes {
    private UUID id;
    private UUID partnerId;

    private String kind;
    private String name;
    private String description;

    private String countryCode;
    private String city;
    private String addressLine;
    private String postalCode;

    private Double lat;
    private Double lng;

    private BigDecimal ratingAvg;   // NUMERIC(3,2) -> BigDecimal
    private Integer ratingCount;

    private String status;          // draft|active|inactive
    private String timezone;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
