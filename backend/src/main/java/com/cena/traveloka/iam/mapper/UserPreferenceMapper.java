package com.cena.traveloka.iam.mapper;

import com.cena.traveloka.iam.dto.request.UserPreferenceUpdateReq;
import com.cena.traveloka.iam.dto.response.UserPreferenceRes;
import com.cena.traveloka.iam.entity.UserPreference;
import org.mapstruct.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * MapStruct mapper for UserPreference entity and DTOs
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserPreferenceMapper {

    // === Entity to Response DTOs ===

    /**
     * Map UserPreference entity to UserPreferenceRes DTO
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "language", source = "language")
    @Mapping(target = "timeZone", source = "timezone")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "dateFormat", source = ".", qualifiedByName = "extractDateFormat")
    @Mapping(target = "timeFormat", source = ".", qualifiedByName = "extractTimeFormat")
    @Mapping(target = "theme", source = ".", qualifiedByName = "extractTheme")
    @Mapping(target = "notificationSettings", source = "notificationPreferences")
    @Mapping(target = "privacySettings", source = "privacySettings")
    @Mapping(target = "communicationPreferences", ignore = true) // Not available in entity
    @Mapping(target = "bookingPreferences", source = "bookingPreferences")
    @Mapping(target = "searchPreferences", ignore = true) // Not available in entity
    @Mapping(target = "accessibilitySettings", source = "accessibilityOptions")
    @Mapping(target = "customSettings", source = ".", qualifiedByName = "extractCustomSettings")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "offsetDateTimeToInstant")
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "offsetDateTimeToInstant")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "updatedBy", source = "updatedBy")
    UserPreferenceRes toUserPreferenceRes(UserPreference preference);

    /**
     * Map list of UserPreference entities to list of UserPreferenceRes DTOs
     */
    List<UserPreferenceRes> toUserPreferenceResList(List<UserPreference> preferences);

    // === Request DTOs to Entity ===

    /**
     * Update UserPreference entity from UserPreferenceUpdateReq DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true) // Not updatable
    @Mapping(target = "language", source = "language")
    @Mapping(target = "timezone", source = "timeZone")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "accessibilityOptions", source = ".", qualifiedByName = "mapThemeToAppSettings")
    @Mapping(target = "notificationPreferences", source = "notificationSettings")
    @Mapping(target = "privacySettings", source = "privacySettings")
    @Mapping(target = "bookingPreferences", source = "bookingPreferences")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true) // Set by JPA
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true) // Set by service
    // No version field in entity - removed
    @Mapping(target = "user", ignore = true)
    void updateUserPreferenceFromDto(UserPreferenceUpdateReq request, @MappingTarget UserPreference preference);

    /**
     * Create UserPreference from UserPreferenceUpdateReq (for initial creation)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true) // Set by service
    @Mapping(target = "language", source = "language")
    @Mapping(target = "timezone", source = "timeZone")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "accessibilityOptions", source = ".", qualifiedByName = "mapThemeToAppSettings")
    @Mapping(target = "notificationPreferences", source = "notificationSettings")
    @Mapping(target = "privacySettings", source = "privacySettings")
    @Mapping(target = "bookingPreferences", source = "bookingPreferences")
    @Mapping(target = "createdAt", ignore = true) // Set by JPA
    @Mapping(target = "updatedAt", ignore = true) // Set by JPA
    @Mapping(target = "createdBy", ignore = true) // Set by service
    @Mapping(target = "updatedBy", ignore = true) // Set by service
    // No version field in entity - removed
    @Mapping(target = "user", ignore = true)
    UserPreference toUserPreference(UserPreferenceUpdateReq request);

    // === Custom Mapping Methods ===

    /**
     * Convert OffsetDateTime to Instant
     */
    @Named("offsetDateTimeToInstant")
    default Instant offsetDateTimeToInstant(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toInstant() : null;
    }

    /**
     * Extract date format from UI preferences
     */
    @Named("extractDateFormat")
    default String extractDateFormat(UserPreference preference) {
        if (preference == null || preference.getPrivacySettings() == null) {
            return "MM/dd/yyyy"; // Default date format
        }
        Object dateFormat = preference.getPrivacySettings().get("dateFormat");
        return dateFormat != null ? dateFormat.toString() : "MM/dd/yyyy";
    }

    /**
     * Extract time format from UI preferences
     */
    @Named("extractTimeFormat")
    default String extractTimeFormat(UserPreference preference) {
        if (preference == null || preference.getPrivacySettings() == null) {
            return "12h"; // Default time format
        }
        Object timeFormat = preference.getPrivacySettings().get("timeFormat");
        return timeFormat != null ? timeFormat.toString() : "12h";
    }

    /**
     * Extract theme from accessibility options
     */
    @Named("extractTheme")
    default String extractTheme(UserPreference preference) {
        if (preference == null || preference.getAccessibilityOptions() == null) {
            return "LIGHT"; // Default theme
        }
        Object theme = preference.getAccessibilityOptions().get("theme");
        return theme != null ? theme.toString() : "LIGHT";
    }

    /**
     * Extract custom settings from all preference fields
     */
    @Named("extractCustomSettings")
    default Map<String, Object> extractCustomSettings(UserPreference preference) {
        if (preference == null || preference.getAccessibilityOptions() == null) {
            return Map.of();
        }
        return preference.getAccessibilityOptions();
    }

    /**
     * Map theme to appSettings (for request mapping)
     */
    @Named("mapThemeToAppSettings")
    default Map<String, Object> mapThemeToAppSettings(Object request) {
        // This will be handled by the service layer for complex mapping
        // Return null here as we'll set appSettings manually in the service
        return null;
    }

    /**
     * Before mapping method for audit trail
     */
    @BeforeMapping
    default void beforeMapping(UserPreferenceUpdateReq request, @MappingTarget UserPreference preference) {
        if (preference != null) {
            preference.setUpdatedAt(OffsetDateTime.now());
        }
    }

    @BeforeMapping
    default void beforeCreateMapping(UserPreferenceUpdateReq request, @MappingTarget UserPreference preference) {
        if (preference != null) {
            preference.setCreatedAt(OffsetDateTime.now());
            preference.setUpdatedAt(OffsetDateTime.now());
        }
    }
}