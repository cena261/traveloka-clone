package com.cena.traveloka.search.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@Jacksonized
public class PropertySearchResult {
    private UUID propertyId;
    private String name;
    private String description;
    private String city;
    private String countryCode;
    private String addressLine;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer starRating;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private BigDecimal basePrice;
    private String currency;
    private String status;
    private List<String> amenities;
    private List<String> images;
    private Map<String, Object> roomTypes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private BigDecimal searchScore;
    private String searchRelevance;
}
