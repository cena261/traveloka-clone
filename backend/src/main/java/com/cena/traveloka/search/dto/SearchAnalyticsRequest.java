package com.cena.traveloka.search.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@Jacksonized
public class SearchAnalyticsRequest {
    private String sessionId;
    private String userId;
    private String query;
    private String searchType;
    private OffsetDateTime timestamp;
    private Integer totalResults;
    private Long responseTimeMs;
    private Map<String, Object> filters;
    private String deviceType;
    private String userAgent;
}