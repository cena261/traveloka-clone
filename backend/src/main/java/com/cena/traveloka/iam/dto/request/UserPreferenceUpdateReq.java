package com.cena.traveloka.iam.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * Request DTO for updating user preferences
 */
@Data
public class UserPreferenceUpdateReq {

    @Size(max = 10, message = "Language must not exceed 10 characters")
    private String language;

    @Size(max = 50, message = "Time zone must not exceed 50 characters")
    private String timeZone;

    @Size(max = 10, message = "Currency must not exceed 10 characters")
    private String currency;

    @Size(max = 10, message = "Date format must not exceed 10 characters")
    private String dateFormat;

    @Size(max = 10, message = "Time format must not exceed 10 characters")
    private String timeFormat;

    @Size(max = 50, message = "Theme must not exceed 50 characters")
    private String theme;

    private Map<String, Object> notificationSettings;

    private Map<String, Object> privacySettings;

    private Map<String, Object> communicationPreferences;

    private Map<String, Object> bookingPreferences;

    private Map<String, Object> searchPreferences;

    private Map<String, Object> accessibilitySettings;

    private Map<String, Object> customSettings;
}