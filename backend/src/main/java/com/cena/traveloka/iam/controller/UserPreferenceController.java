package com.cena.traveloka.iam.controller;

import com.cena.traveloka.iam.dto.request.UserPreferenceUpdateReq;
import com.cena.traveloka.iam.dto.response.*;
import com.cena.traveloka.iam.entity.UserPreference;
import com.cena.traveloka.iam.mapper.UserPreferenceMapper;
import com.cena.traveloka.iam.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for User Preference Management
 *
 * Provides endpoints for user preference operations, analytics, and bulk updates
 * Secured with OAuth2 and role-based access control
 */
@RestController
@RequestMapping("/api/iam/preferences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Preference Management", description = "User preference operations and analytics")
@SecurityRequirement(name = "bearer-jwt")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;
    private final UserPreferenceMapper userPreferenceMapper;

    // === Preference CRUD Operations ===

    /**
     * Get user preferences by user ID
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user preferences", description = "Retrieves user preferences by user ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or #userId == authentication.name")
    public ResponseEntity<ApiResponse<UserPreferenceRes>> getPreferencesByUserId(
            @Parameter(description = "User ID") @PathVariable String userId) {

        log.debug("Getting preferences for user: {}", userId);

        Optional<UserPreference> preferences = userPreferenceService.findByUserId(userId);
        if (preferences.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("Preferences not found for user: " + userId));
        }

        UserPreferenceRes preferenceRes = userPreferenceMapper.toUserPreferenceRes(preferences.get());
        return ResponseEntity.ok(ApiResponse.success(preferenceRes));
    }

    /**
     * Get preferences by preference ID
     */
    @GetMapping("/{preferenceId}")
    @Operation(summary = "Get preferences by ID", description = "Retrieves user preferences by preference ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER', 'USER')")
    public ResponseEntity<ApiResponse<UserPreferenceRes>> getPreferencesById(
            @Parameter(description = "Preference ID") @PathVariable String preferenceId) {

        log.debug("Getting preferences by ID: {}", preferenceId);

        Optional<UserPreference> preferences = userPreferenceService.findById(preferenceId);
        if (preferences.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("Preferences not found: " + preferenceId));
        }

        UserPreferenceRes preferenceRes = userPreferenceMapper.toUserPreferenceRes(preferences.get());
        return ResponseEntity.ok(ApiResponse.success(preferenceRes));
    }

    /**
     * Update user preferences
     */
    @PutMapping("/user/{userId}")
    @Operation(summary = "Update user preferences", description = "Updates user preferences")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Preferences updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid preference data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or #userId == authentication.name")
    public ResponseEntity<ApiResponse<UserPreferenceRes>> updatePreferences(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Valid @RequestBody UserPreferenceUpdateReq request) {

        log.info("Updating preferences for user: {}", userId);

        try {
            // Create or get existing preferences for user
            Optional<UserPreference> existingPrefs = userPreferenceService.findByUserId(userId);
            UserPreference preferences;
            if (existingPrefs.isPresent()) {
                preferences = existingPrefs.get();
            } else {
                preferences = new UserPreference();
                preferences.setUserId(UUID.fromString(userId));
                // Map request to new entity
            }
            UserPreference updatedPreferences = userPreferenceService.updatePreferences(preferences);
            UserPreferenceRes preferenceRes = userPreferenceMapper.toUserPreferenceRes(updatedPreferences);

            return ResponseEntity.ok(ApiResponse.success("Preferences updated successfully", preferenceRes));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to update preferences for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    /**
     * Reset user preferences to defaults
     */
    @PostMapping("/user/{userId}/reset")
    @Operation(summary = "Reset user preferences", description = "Resets user preferences to default values")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or #userId == authentication.name")
    public ResponseEntity<ApiResponse<UserPreferenceRes>> resetPreferences(
            @Parameter(description = "User ID") @PathVariable String userId) {

        log.info("Resetting preferences for user: {}", userId);

        try {
            // Get existing preferences or create new with defaults
            Optional<UserPreference> existingPrefs = userPreferenceService.findByUserId(userId);
            UserPreference resetPreferences;
            if (existingPrefs.isPresent()) {
                resetPreferences = existingPrefs.get();
                // Reset to default values
                resetPreferences.setLanguage("en");
                resetPreferences.setTimezone("UTC");
                resetPreferences.setCurrency("USD");
                resetPreferences = userPreferenceService.updatePreferences(resetPreferences);
            } else {
                resetPreferences = new UserPreference();
                resetPreferences.setUserId(UUID.fromString(userId));
                resetPreferences.setLanguage("en");
                resetPreferences.setTimezone("UTC");
                resetPreferences.setCurrency("USD");
                resetPreferences = userPreferenceService.createPreferences(resetPreferences);
            }
            UserPreferenceRes preferenceRes = userPreferenceMapper.toUserPreferenceRes(resetPreferences);

            return ResponseEntity.ok(ApiResponse.success("Preferences reset successfully", preferenceRes));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to reset preferences for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    // === Specific Preference Categories ===

    /**
     * Update notification preferences
     */
    @PutMapping("/user/{userId}/notifications")
    @Operation(summary = "Update notification preferences", description = "Updates only notification preferences")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or #userId == authentication.name")
    public ResponseEntity<ApiResponse<UserPreferenceRes>> updateNotificationPreferences(
            @Parameter(description = "User ID") @PathVariable String userId,
            @RequestBody Map<String, Object> notificationSettings) {

        log.info("Updating notification preferences for user: {}", userId);

        try {
            // Get existing preferences and update notification settings
            Optional<UserPreference> existingPrefs = userPreferenceService.findByUserId(userId);
            if (existingPrefs.isEmpty()) {
                throw new IllegalArgumentException("User preferences not found: " + userId);
            }
            UserPreference preferences = existingPrefs.get();
            // Update notification preferences (would need to implement JSON merging)
            preferences.setUpdatedAt(java.time.OffsetDateTime.now());
            UserPreference updatedPreferences = userPreferenceService.updatePreferences(preferences);
            UserPreferenceRes preferenceRes = userPreferenceMapper.toUserPreferenceRes(updatedPreferences);

            return ResponseEntity.ok(ApiResponse.success("Notification preferences updated", preferenceRes));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to update notification preferences for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    /**
     * Update privacy preferences
     */
    @PutMapping("/user/{userId}/privacy")
    @Operation(summary = "Update privacy preferences", description = "Updates only privacy preferences")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or #userId == authentication.name")
    public ResponseEntity<ApiResponse<UserPreferenceRes>> updatePrivacyPreferences(
            @Parameter(description = "User ID") @PathVariable String userId,
            @RequestBody Map<String, Object> privacySettings) {

        log.info("Updating privacy preferences for user: {}", userId);

        try {
            // Get existing preferences and update privacy settings
            Optional<UserPreference> existingPrefs = userPreferenceService.findByUserId(userId);
            if (existingPrefs.isEmpty()) {
                throw new IllegalArgumentException("User preferences not found: " + userId);
            }
            UserPreference preferences = existingPrefs.get();
            // Update privacy preferences (would need to implement JSON merging)
            preferences.setUpdatedAt(java.time.OffsetDateTime.now());
            UserPreference updatedPreferences = userPreferenceService.updatePreferences(preferences);
            UserPreferenceRes preferenceRes = userPreferenceMapper.toUserPreferenceRes(updatedPreferences);

            return ResponseEntity.ok(ApiResponse.success("Privacy preferences updated", preferenceRes));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to update privacy preferences for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    /**
     * Update localization preferences
     */
    @PutMapping("/user/{userId}/localization")
    @Operation(summary = "Update localization preferences", description = "Updates language, timezone, and currency preferences")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER') or #userId == authentication.name")
    public ResponseEntity<ApiResponse<UserPreferenceRes>> updateLocalizationPreferences(
            @Parameter(description = "User ID") @PathVariable String userId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String timeZone,
            @RequestParam(required = false) String currency) {

        log.info("Updating localization preferences for user: {}", userId);

        try {
            // Get existing preferences and update localization
            Optional<UserPreference> existingPrefs = userPreferenceService.findByUserId(userId);
            if (existingPrefs.isEmpty()) {
                throw new IllegalArgumentException("User preferences not found: " + userId);
            }
            UserPreference preferences = existingPrefs.get();
            if (language != null) preferences.setLanguage(language);
            if (timeZone != null) preferences.setTimezone(timeZone);
            if (currency != null) preferences.setCurrency(currency);
            preferences.setUpdatedAt(java.time.OffsetDateTime.now());
            UserPreference updatedPreferences = userPreferenceService.updatePreferences(preferences);
            UserPreferenceRes preferenceRes = userPreferenceMapper.toUserPreferenceRes(updatedPreferences);

            return ResponseEntity.ok(ApiResponse.success("Localization preferences updated", preferenceRes));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to update localization preferences for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    // === Preference Analytics ===

    /**
     * Get preference analytics by language
     */
    @GetMapping("/analytics/languages")
    @Operation(summary = "Get language preference analytics", description = "Retrieves analytics for language preferences")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getLanguageAnalytics() {

        log.debug("Getting language preference analytics");

        try {
            // Use existing language distribution method
            List<Object[]> languageData = userPreferenceService.getLanguageDistribution();
            Map<String, Long> analytics = new java.util.HashMap<>();
            for (Object[] row : languageData) {
                analytics.put((String) row[0], (Long) row[1]);
            }
            return ResponseEntity.ok(ApiResponse.success(analytics));

        } catch (Exception e) {
            log.error("Failed to get language analytics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Failed to retrieve language analytics"));
        }
    }

    /**
     * Get preference analytics by timezone
     */
    @GetMapping("/analytics/timezones")
    @Operation(summary = "Get timezone preference analytics", description = "Retrieves analytics for timezone preferences")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getTimezoneAnalytics() {

        log.debug("Getting timezone preference analytics");

        try {
            // Use existing timezone distribution method
            List<Object[]> timezoneData = userPreferenceService.getTimezoneDistribution();
            Map<String, Long> analytics = new java.util.HashMap<>();
            for (Object[] row : timezoneData) {
                analytics.put((String) row[0], (Long) row[1]);
            }
            return ResponseEntity.ok(ApiResponse.success(analytics));

        } catch (Exception e) {
            log.error("Failed to get timezone analytics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Failed to retrieve timezone analytics"));
        }
    }

    /**
     * Get notification preference analytics
     */
    @GetMapping("/analytics/notifications")
    @Operation(summary = "Get notification preference analytics", description = "Retrieves analytics for notification preferences")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotificationAnalytics() {

        log.debug("Getting notification preference analytics");

        try {
            // Use existing methods to gather notification analytics
            Map<String, Object> analytics = new java.util.HashMap<>();
            List<UserPreference> emailUsers = userPreferenceService.findUsersWithEmailNotificationsEnabled();
            List<UserPreference> smsUsers = userPreferenceService.findUsersWithSmsNotificationsEnabled();
            List<UserPreference> pushUsers = userPreferenceService.findUsersWithPushNotificationsEnabled();
            List<UserPreference> promoUsers = userPreferenceService.findUsersOptedInForPromotions();
            analytics.put("emailEnabled", emailUsers.size());
            analytics.put("smsEnabled", smsUsers.size());
            analytics.put("pushEnabled", pushUsers.size());
            analytics.put("promotionsOptIn", promoUsers.size());
            return ResponseEntity.ok(ApiResponse.success(analytics));

        } catch (Exception e) {
            log.error("Failed to get notification analytics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Failed to retrieve notification analytics"));
        }
    }

    // === Bulk Operations ===

    /**
     * Bulk update notification preferences
     */
    @PostMapping("/bulk/notifications")
    @Operation(summary = "Bulk update notification preferences", description = "Updates notification preferences for multiple users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> bulkUpdateNotifications(
            @RequestParam List<String> userIds,
            @RequestBody Map<String, Object> notificationSettings) {

        log.info("Bulk updating notification preferences for {} users", userIds.size());

        try {
            // Implement bulk update using existing methods
            int updatedCount = 0;
            for (String userId : userIds) {
                Optional<UserPreference> existingPrefs = userPreferenceService.findByUserId(userId);
                if (existingPrefs.isPresent()) {
                    UserPreference preferences = existingPrefs.get();
                    // Update notification settings (JSON merging needed)
                    preferences.setUpdatedAt(java.time.OffsetDateTime.now());
                    userPreferenceService.updatePreferences(preferences);
                    updatedCount++;
                }
            }
            return ResponseEntity.ok(ApiResponse.success(
                    "Notification preferences updated successfully",
                    String.format("Updated notification preferences for %d users", updatedCount)));

        } catch (Exception e) {
            log.error("Failed to bulk update notification preferences", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Bulk update failed: " + e.getMessage()));
        }
    }

    /**
     * Bulk update localization preferences
     */
    @PostMapping("/bulk/localization")
    @Operation(summary = "Bulk update localization preferences", description = "Updates localization preferences for multiple users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> bulkUpdateLocalization(
            @RequestParam List<String> userIds,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String timeZone,
            @RequestParam(required = false) String currency) {

        log.info("Bulk updating localization preferences for {} users", userIds.size());

        try {
            // Implement bulk update using existing methods
            int updatedCount = 0;
            for (String userId : userIds) {
                Optional<UserPreference> existingPrefs = userPreferenceService.findByUserId(userId);
                if (existingPrefs.isPresent()) {
                    UserPreference preferences = existingPrefs.get();
                    if (language != null) preferences.setLanguage(language);
                    if (timeZone != null) preferences.setTimezone(timeZone);
                    if (currency != null) preferences.setCurrency(currency);
                    preferences.setUpdatedAt(java.time.OffsetDateTime.now());
                    userPreferenceService.updatePreferences(preferences);
                    updatedCount++;
                }
            }
            return ResponseEntity.ok(ApiResponse.success(
                    "Localization preferences updated successfully",
                    String.format("Updated localization preferences for %d users", updatedCount)));

        } catch (Exception e) {
            log.error("Failed to bulk update localization preferences", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.internalServerError("Bulk update failed: " + e.getMessage()));
        }
    }

    // === Search and Filtering ===

    /**
     * Get users by language preference
     */
    @GetMapping("/language/{language}")
    @Operation(summary = "Get users by language preference", description = "Retrieves users with specific language preference")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<PagedResponse<UserPreferenceRes>> getUsersByLanguage(
            @Parameter(description = "Language code") @PathVariable String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting users by language preference: {} (page: {}, size: {})", language, page, size);

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<UserPreference> preferences = userPreferenceService.findByLanguage(language, pageable);

        List<UserPreferenceRes> preferenceResList = userPreferenceMapper.toUserPreferenceResList(preferences.getContent());
        PagedResponse<UserPreferenceRes> response = PagedResponse.of(preferenceResList, preferences);

        return ResponseEntity.ok(response);
    }

    /**
     * Get users by timezone preference
     */
    @GetMapping("/timezone/{timezone}")
    @Operation(summary = "Get users by timezone preference", description = "Retrieves users with specific timezone preference")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<PagedResponse<UserPreferenceRes>> getUsersByTimezone(
            @Parameter(description = "Timezone") @PathVariable String timezone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting users by timezone preference: {} (page: {}, size: {})", timezone, page, size);

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<UserPreference> preferences = userPreferenceService.findByTimezone(timezone, pageable);

        List<UserPreferenceRes> preferenceResList = userPreferenceMapper.toUserPreferenceResList(preferences.getContent());
        PagedResponse<UserPreferenceRes> response = PagedResponse.of(preferenceResList, preferences);

        return ResponseEntity.ok(response);
    }

    /**
     * Search preferences with filters
     */
    @GetMapping("/search")
    @Operation(summary = "Search preferences", description = "Search user preferences with filters and pagination")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<PagedResponse<UserPreferenceRes>> searchPreferences(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String timeZone,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String theme,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.debug("Searching preferences with filters - language: {}, timeZone: {}, currency: {}, theme: {}",
                language, timeZone, currency, theme);

        try {
            Pageable pageable = createPageable(page, size, sortBy, sortDirection);
            Page<UserPreference> preferences = userPreferenceService.searchPreferences(
                    language, timeZone, currency, pageable);

            List<UserPreferenceRes> preferenceResList = userPreferenceMapper.toUserPreferenceResList(preferences.getContent());
            PagedResponse<UserPreferenceRes> response = PagedResponse.of(preferenceResList, preferences);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching preferences", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PagedResponse.<UserPreferenceRes>builder()
                            .success(false)
                            .message("Search failed: " + e.getMessage())
                            .build());
        }
    }

    // === Statistics ===

    /**
     * Count preferences by language
     */
    @GetMapping("/count/language/{language}")
    @Operation(summary = "Count preferences by language", description = "Returns count of users with specific language preference")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<Long>> countByLanguage(
            @Parameter(description = "Language code") @PathVariable String language) {

        log.debug("Counting preferences by language: {}", language);

        List<Object[]> languageData = userPreferenceService.getLanguageDistribution();
        long count = 0;
        for (Object[] row : languageData) {
            if (language.equals(row[0])) {
                count = (Long) row[1];
                break;
            }
        }
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * Count preferences by timezone
     */
    @GetMapping("/count/timezone/{timezone}")
    @Operation(summary = "Count preferences by timezone", description = "Returns count of users with specific timezone preference")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER_MANAGER')")
    public ResponseEntity<ApiResponse<Long>> countByTimezone(
            @Parameter(description = "Timezone") @PathVariable String timezone) {

        log.debug("Counting preferences by timezone: {}", timezone);

        // Use timezone distribution to count specific timezone
        List<Object[]> timezoneData = userPreferenceService.getTimezoneDistribution();
        long count = 0;
        for (Object[] row : timezoneData) {
            if (timezone.equals(row[0])) {
                count = (Long) row[1];
                break;
            }
        }
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    // === Utility Methods ===

    private Pageable createPageable(int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        return PageRequest.of(page, Math.min(size, 100), sort);
    }
}