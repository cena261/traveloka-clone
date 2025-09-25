package com.cena.traveloka.iam.dto.response;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for user preferences
 */
@Data
public class UserPreferenceRes {

    private String id;

    private String userId;

    private String language;

    private String timeZone;

    private String currency;

    private String dateFormat;

    private String timeFormat;

    private String theme;

    private Map<String, Object> notificationSettings;

    private Map<String, Object> privacySettings;

    private Map<String, Object> communicationPreferences;

    private Map<String, Object> bookingPreferences;

    private Map<String, Object> searchPreferences;

    private Map<String, Object> accessibilitySettings;

    private Map<String, Object> customSettings;

    private Instant createdAt;

    private Instant updatedAt;

    private String createdBy;

    private String updatedBy;
}