package com.cena.traveloka.search.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@Jacksonized
public class PersonalizedSearchRequest {

    private UUID userId;
    private String query;
    private String language;
    private Pageable pageable;

    // Optional override filters (user can still specify explicit filters)
    private PropertySearchRequest.LocationFilter location;
    private PropertySearchRequest.PriceFilter price;
    private PropertySearchRequest.PropertyFilter property;
    private PropertySearchRequest.GuestFilter guests;

    // Personalization context
    private PersonalizationContext context;

    // ML feature overrides
    private Map<String, Double> featureWeights;

    @Data
    @Builder
    @Jacksonized
    public static class PersonalizationContext {

        // Current trip context
        private String tripPurpose; // business, leisure, family
        private String tripType; // solo, couple, group, family
        private Integer tripDuration; // days
        private String bookingUrgency; // immediate, planning, flexible

        // Seasonal context
        private String season; // spring, summer, fall, winter
        private Boolean isHoliday;
        private Boolean isWeekend;

        // Device and session context
        private String deviceType; // mobile, desktop, tablet
        private String sessionId;
        private Integer sessionStep; // which step in the booking funnel

        // Geographic context
        private String currentLocation; // user's current location
        private String timeZone;

        // Business context
        private String channel; // web, mobile_app, partner
        private String campaignId; // marketing campaign context
        private String abTestGroup; // A/B testing group

        // Personalization settings
        private Boolean enablePersonalization;
        private String personalizationLevel; // light, moderate, aggressive
        private Map<String, Boolean> featureFlags;
    }
}