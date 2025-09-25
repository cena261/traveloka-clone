package com.cena.traveloka.iam.dto.request;

import com.cena.traveloka.iam.validation.ValidTimezone;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.util.Map;

/**
 * Request DTO for updating user preferences
 *
 * Includes validation for user preference settings:
 * - Communication preferences with boolean validation
 * - Language and timezone preferences
 * - Notification settings with granular controls
 * - Privacy settings with security validation
 * - Custom preferences with type safety
 */
@Data
public class UpdateUserPreferencesRequest {

    // Communication Preferences
    @NotNull(message = "Email notifications preference is required")
    private Boolean emailNotifications;

    @NotNull(message = "SMS notifications preference is required")
    private Boolean smsNotifications;

    @NotNull(message = "Push notifications preference is required")
    private Boolean pushNotifications;

    @NotNull(message = "Marketing emails preference is required")
    private Boolean marketingEmails;

    // Localization Preferences
    @Size(max = 10, message = "Preferred language must not exceed 10 characters")
    @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "Language must be in format 'en' or 'en-US'")
    private String preferredLanguage;

    @ValidTimezone
    private String timezone;

    @Size(max = 3, message = "Currency must be a 3-character ISO code")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-character ISO currency code")
    private String preferredCurrency;

    @Size(max = 5, message = "Date format must not exceed 5 characters")
    @Pattern(regexp = "^(dd/MM/yyyy|MM/dd/yyyy|yyyy-MM-dd)$",
             message = "Date format must be dd/MM/yyyy, MM/dd/yyyy, or yyyy-MM-dd")
    private String dateFormat;

    @Size(max = 10, message = "Time format must not exceed 10 characters")
    @Pattern(regexp = "^(12h|24h)$", message = "Time format must be 12h or 24h")
    private String timeFormat;

    // Notification Granularity
    @NotNull(message = "Booking confirmations preference is required")
    private Boolean bookingConfirmations;

    @NotNull(message = "Payment notifications preference is required")
    private Boolean paymentNotifications;

    @NotNull(message = "Security alerts preference is required")
    private Boolean securityAlerts;

    @NotNull(message = "Travel updates preference is required")
    private Boolean travelUpdates;

    @NotNull(message = "Promotional offers preference is required")
    private Boolean promotionalOffers;

    // Privacy Preferences
    @NotNull(message = "Profile visibility preference is required")
    private Boolean profileVisible;

    @NotNull(message = "Activity sharing preference is required")
    private Boolean shareActivity;

    @NotNull(message = "Data analytics preference is required")
    private Boolean allowAnalytics;

    @NotNull(message = "Third-party sharing preference is required")
    private Boolean allowThirdPartySharing;

    // Notification Timing
    @Min(value = 0, message = "Quiet hours start must be between 0 and 23")
    @Max(value = 23, message = "Quiet hours start must be between 0 and 23")
    private Integer quietHoursStart;

    @Min(value = 0, message = "Quiet hours end must be between 0 and 23")
    @Max(value = 23, message = "Quiet hours end must be between 0 and 23")
    private Integer quietHoursEnd;

    // Custom Preferences (flexible key-value pairs)
    @Size(max = 50, message = "Cannot have more than 50 custom preferences")
    private Map<@Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Preference key can only contain alphanumeric characters, underscore, and hyphen")
                @Size(min = 1, max = 50, message = "Preference key must be between 1 and 50 characters") String,
                @Size(max = 500, message = "Preference value cannot exceed 500 characters") String> customPreferences;

    @AssertTrue(message = "Quiet hours end must be different from start")
    public boolean isValidQuietHours() {
        if (quietHoursStart == null || quietHoursEnd == null) {
            return true; // Allow null values
        }
        return !quietHoursStart.equals(quietHoursEnd);
    }

    @AssertTrue(message = "At least one notification method must be enabled if security alerts are enabled")
    public boolean isValidSecurityNotificationSettings() {
        if (securityAlerts == null || !securityAlerts) {
            return true; // Security alerts disabled, no validation needed
        }
        return Boolean.TRUE.equals(emailNotifications) ||
               Boolean.TRUE.equals(smsNotifications) ||
               Boolean.TRUE.equals(pushNotifications);
    }
}