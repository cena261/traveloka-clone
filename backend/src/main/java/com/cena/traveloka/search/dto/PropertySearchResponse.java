package com.cena.traveloka.search.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Builder
@Jacksonized
public class PropertySearchResponse {
    private List<PropertySearchResult> results;
    private PaginationInfo pagination;
    private String query;
    private Integer totalResults;
    private Long searchTimeMs;
}