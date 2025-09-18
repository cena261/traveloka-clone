package com.cena.traveloka.search.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
@Jacksonized
public class PropertySearchRequest {
    private String query;
    private String destination;
    private OffsetDateTime checkInDate;
    private OffsetDateTime checkOutDate;
    private Integer adults;
    private Integer children;
    private Integer rooms;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String currency;
    private List<String> amenities;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Double radiusKm;
    private String sortBy;
    private String sortOrder;
    private Integer page;
    private Integer size;
}