package com.cena.traveloka.iam.service;

import com.cena.traveloka.iam.entity.UserPreference;
import com.cena.traveloka.iam.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Service class for UserPreference entity operations
 *
 * Provides comprehensive user preference management functionality including:
 * - Preference CRUD operations with validation
 * - Localization and internationalization settings
 * - Notification preference management
 * - Booking preference optimization
 * - Privacy and accessibility settings
 * - Preference analytics and insights
 *
 * Key Features:
 * - Transactional operations for data consistency
 * - Redis caching for frequently accessed preferences
 * - JSON-based flexible preference storage
 * - Real-time preference application
 * - Audit logging for preference changes
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;

    // Valid language codes (ISO 639-1 + ISO 3166-1)
    private static final Set<String> VALID_LANGUAGE_CODES = Set.of(
        "en-US", "en-GB", "vi-VN", "th-TH", "id-ID", "ms-MY", "zh-CN", "zh-TW", "ja-JP", "ko-KR", "fr-FR", "de-DE", "es-ES"
    );

    // Valid currency codes (ISO 4217)
    private static final Set<String> VALID_CURRENCY_CODES = Set.of(
        "USD", "VND", "THB", "IDR", "MYR", "SGD", "CNY", "JPY", "KRW", "EUR", "GBP"
    );

    // === Core CRUD Operations ===

    /**
     * Create new user preferences with validation
     *
     * @param preferences Preferences entity to create
     * @return Created preferences with generated ID
     * @throws IllegalArgumentException if preferences data is invalid
     */
    public UserPreference createPreferences(UserPreference preferences) {
        log.info("Creating new preferences for user: {}", preferences.getUserId());

        validatePreferences(preferences);
        validateUniqueConstraints(preferences);

        // Set defaults if not provided
        setDefaultPreferences(preferences);

        preferences.setCreatedBy("SYSTEM");
        preferences.setUpdatedBy("SYSTEM");

        UserPreference savedPreferences = userPreferenceRepository.save(preferences);
        log.info("Successfully created preferences with ID: {}", savedPreferences.getId());

        return savedPreferences;
    }

    /**
     * Find preferences by ID with caching
     *
     * @param preferencesId Preferences ID
     * @return Preferences if found
     */
    @Cacheable(value = "preferences", key = "#preferencesId")
    @Transactional(readOnly = true)
    public Optional<UserPreference> findById(String preferencesId) {
        log.debug("Finding preferences by ID: {}", preferencesId);
        return userPreferenceRepository.findById(preferencesId);
    }

    /**
     * Find preferences by user ID
     *
     * @param userId User ID
     * @return Preferences if found
     */
    @Cacheable(value = "preferences", key = "'user:' + #userId")
    @Transactional(readOnly = true)
    public Optional<UserPreference> findByUserId(String userId) {
        log.debug("Finding preferences by user ID: {}", userId);
        return userPreferenceRepository.findByUserId(userId);
    }

    /**
     * Update preferences with cache eviction
     *
     * @param preferences Preferences to update
     * @return Updated preferences
     */
    @CacheEvict(value = "preferences", allEntries = true)
    public UserPreference updatePreferences(UserPreference preferences) {
        log.info("Updating preferences with ID: {}", preferences.getId());

        validatePreferences(preferences);
        preferences.setUpdatedBy("SYSTEM");

        UserPreference updatedPreferences = userPreferenceRepository.save(preferences);
        log.info("Successfully updated preferences with ID: {}", updatedPreferences.getId());

        return updatedPreferences;
    }

    /**
     * Delete preferences by user ID
     *
     * @param userId User ID whose preferences to delete
     * @param deletedBy Who performed the deletion
     * @return Number of preferences deleted
     */
    @CacheEvict(value = "preferences", allEntries = true)
    public int deletePreferencesByUserId(String userId, String deletedBy) {
        log.info("Deleting preferences for user: {}", userId);

        int deletedCount = userPreferenceRepository.deleteByUserId(userId);
        log.info("Successfully deleted {} preference(s) for user: {}", deletedCount, userId);

        return deletedCount;
    }

    // === Localization and Language Management ===

    /**
     * Find preferences by language
     *
     * @param language Language code
     * @param pageable Pagination parameters
     * @return Page of preferences
     */
    @Transactional(readOnly = true)
    public Page<UserPreference> findByLanguage(String language, Pageable pageable) {
        log.debug("Finding preferences by language: {}", language);
        return userPreferenceRepository.findByLanguage(language, pageable);
    }

    /**
     * Find preferences by timezone
     *
     * @param timezone Timezone identifier
     * @param pageable Pagination parameters
     * @return Page of preferences
     */
    @Transactional(readOnly = true)
    public Page<UserPreference> findByTimezone(String timezone, Pageable pageable) {
        log.debug("Finding preferences by timezone: {}", timezone);
        return userPreferenceRepository.findByTimezone(timezone, pageable);
    }

    /**
     * Find preferences by currency
     *
     * @param currency Currency code
     * @param pageable Pagination parameters
     * @return Page of preferences
     */
    @Transactional(readOnly = true)
    public Page<UserPreference> findByCurrency(String currency, Pageable pageable) {
        log.debug("Finding preferences by currency: {}", currency);
        return userPreferenceRepository.findByCurrency(currency, pageable);
    }

    /**
     * Get language distribution for analytics
     *
     * @return List of language counts
     */
    @Transactional(readOnly = true)
    public List<Object[]> getLanguageDistribution() {
        log.debug("Getting language distribution");
        return userPreferenceRepository.countByLanguage();
    }

    /**
     * Get timezone distribution for analytics
     *
     * @return List of timezone counts
     */
    @Transactional(readOnly = true)
    public List<Object[]> getTimezoneDistribution() {
        log.debug("Getting timezone distribution");
        return userPreferenceRepository.countByTimezone();
    }

    /**
     * Update language for migration
     *
     * @param oldLanguage Old language code
     * @param newLanguage New language code
     * @param updatedBy Who performed the update
     * @return Number of preferences updated
     */
    @CacheEvict(value = "preferences", allEntries = true)
    public int updateLanguageForUsers(String oldLanguage, String newLanguage, String updatedBy) {
        log.info("Migrating language from {} to {} for users", oldLanguage, newLanguage);

        validateLanguageCode(newLanguage);

        int updatedCount = userPreferenceRepository.updateLanguageForUsers(oldLanguage, newLanguage, updatedBy);
        log.info("Successfully updated language for {} users", updatedCount);

        return updatedCount;
    }

    // === Notification Preferences ===

    /**
     * Find users with email notifications enabled
     *
     * @return List of users opted in for email notifications
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findUsersWithEmailNotificationsEnabled() {
        log.debug("Finding users with email notifications enabled");
        return userPreferenceRepository.findUsersWithEmailNotificationsEnabled();
    }

    /**
     * Find users opted in for promotional emails
     *
     * @return List of users opted in for promotions
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findUsersOptedInForPromotions() {
        log.debug("Finding users opted in for promotional emails");
        return userPreferenceRepository.findUsersOptedInForPromotions();
    }

    /**
     * Find users with SMS notifications enabled
     *
     * @return List of users with SMS notifications enabled
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findUsersWithSmsNotificationsEnabled() {
        log.debug("Finding users with SMS notifications enabled");
        return userPreferenceRepository.findUsersWithSmsNotificationsEnabled();
    }

    /**
     * Find users with push notifications enabled
     *
     * @return List of users with push notifications enabled
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findUsersWithPushNotificationsEnabled() {
        log.debug("Finding users with push notifications enabled");
        return userPreferenceRepository.findUsersWithPushNotificationsEnabled();
    }

    /**
     * Find users by specific notification preference
     *
     * @param jsonPath JSON path to the preference
     * @param value Expected value
     * @return List of matching users
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findByNotificationPreference(String jsonPath, Object value) {
        log.debug("Finding users by notification preference: {} = {}", jsonPath, value);
        return userPreferenceRepository.findByNotificationPreference(jsonPath, value);
    }

    // === Booking Preferences ===

    /**
     * Find users by default guest count
     *
     * @param guestCount Default guest count
     * @return List of users with matching guest count preference
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findByDefaultGuestCount(Integer guestCount) {
        log.debug("Finding users by default guest count: {}", guestCount);
        return userPreferenceRepository.findByDefaultGuestCount(guestCount);
    }

    /**
     * Find users by preferred room type
     *
     * @param roomType Room type preference
     * @return List of users with matching room type preference
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findByPreferredRoomType(String roomType) {
        log.debug("Finding users by preferred room type: {}", roomType);
        return userPreferenceRepository.findByPreferredRoomType(roomType);
    }

    /**
     * Find users by smoking preference
     *
     * @param smokingPreference Smoking preference
     * @return List of users with matching smoking preference
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findBySmokingPreference(String smokingPreference) {
        log.debug("Finding users by smoking preference: {}", smokingPreference);
        return userPreferenceRepository.findBySmokingPreference(smokingPreference);
    }

    /**
     * Find users by bed type preference
     *
     * @param bedType Bed type preference
     * @return List of users with matching bed type preference
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findByBedTypePreference(String bedType) {
        log.debug("Finding users by bed type preference: {}", bedType);
        return userPreferenceRepository.findByBedTypePreference(bedType);
    }

    // === Privacy and Accessibility ===

    /**
     * Find users with public profiles
     *
     * @return List of users with public profile visibility
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findUsersWithPublicProfiles() {
        log.debug("Finding users with public profiles");
        return userPreferenceRepository.findUsersWithPublicProfiles();
    }

    /**
     * Find users who opted out of data sharing
     *
     * @return List of users who opted out of data sharing
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findUsersOptedOutOfDataSharing() {
        log.debug("Finding users opted out of data sharing");
        return userPreferenceRepository.findUsersOptedOutOfDataSharing();
    }

    /**
     * Find users with analytics disabled
     *
     * @return List of users with analytics tracking disabled
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findUsersWithAnalyticsDisabled() {
        log.debug("Finding users with analytics disabled");
        return userPreferenceRepository.findUsersWithAnalyticsDisabled();
    }

    /**
     * Find users with accessibility requirements
     *
     * @return List of users with accessibility options
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findUsersWithAccessibilityOptions() {
        log.debug("Finding users with accessibility options");
        return userPreferenceRepository.findUsersWithAccessibilityOptions();
    }

    /**
     * Find users by specific accessibility requirement
     *
     * @param requirement Accessibility requirement
     * @return List of users with matching accessibility requirement
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findByAccessibilityRequirement(String requirement) {
        log.debug("Finding users by accessibility requirement: {}", requirement);
        return userPreferenceRepository.findByAccessibilityRequirement(requirement);
    }

    // === Search and Advanced Queries ===

    /**
     * Search preferences by multiple criteria
     *
     * @param language Language filter (optional)
     * @param timezone Timezone filter (optional)
     * @param currency Currency filter (optional)
     * @param pageable Pagination parameters
     * @return Page of matching preferences
     */
    @Transactional(readOnly = true)
    public Page<UserPreference> searchPreferences(String language, String timezone, String currency, Pageable pageable) {
        log.debug("Searching preferences with filters - language: {}, timezone: {}, currency: {}", language, timezone, currency);
        return userPreferenceRepository.searchPreferences(language, timezone, currency, pageable);
    }

    /**
     * Find preferences with incomplete localization settings
     *
     * @return List of preferences missing localization data
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findIncompleteLocalizationPreferences() {
        log.debug("Finding preferences with incomplete localization");
        return userPreferenceRepository.findIncompleteLocalizationPreferences();
    }

    // === Time-based Operations ===

    /**
     * Find recently created preferences
     *
     * @param hours Number of hours to look back
     * @return List of recently created preferences
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findRecentlyCreatedPreferences(int hours) {
        log.debug("Finding preferences created in the last {} hours", hours);

        Instant since = Instant.now().minusSeconds(hours * 3600L);
        return userPreferenceRepository.findRecentlyCreatedPreferences(since);
    }

    /**
     * Find stale preferences not updated recently
     *
     * @param thresholdDays Days since last update to consider stale
     * @return List of stale preferences
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findStalePreferences(int thresholdDays) {
        log.debug("Finding preferences not updated in {} days", thresholdDays);

        Instant threshold = Instant.now().minusSeconds(thresholdDays * 24 * 3600L);
        return userPreferenceRepository.findStalePreferences(threshold);
    }

    // === Analytics and Reporting ===

    /**
     * Get preference statistics for dashboard
     *
     * @return Preference statistics
     */
    @Transactional(readOnly = true)
    public UserPreferenceRepository.PreferenceStatistics getPreferenceStatistics() {
        log.debug("Retrieving preference statistics");
        return userPreferenceRepository.getPreferenceStatistics();
    }

    /**
     * Get notification preference statistics
     *
     * @return Notification statistics
     */
    @Transactional(readOnly = true)
    public UserPreferenceRepository.NotificationStatistics getNotificationStatistics() {
        log.debug("Retrieving notification statistics");
        return userPreferenceRepository.getNotificationStatistics();
    }

    // === Bulk Operations ===

    /**
     * Touch preferences to invalidate cache
     *
     * @param userIds List of user IDs
     * @param updatedBy Who performed the update
     * @return Number of preferences updated
     */
    @CacheEvict(value = "preferences", allEntries = true)
    public int touchPreferencesForUsers(List<String> userIds, String updatedBy) {
        log.info("Touching preferences for {} users", userIds.size());

        int updatedCount = userPreferenceRepository.touchPreferencesForUsers(userIds, updatedBy);
        log.info("Successfully touched preferences for {} users", updatedCount);

        return updatedCount;
    }

    // === Data Quality and Validation ===

    /**
     * Find preferences with invalid language codes
     *
     * @return List of preferences with invalid language codes
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findPreferencesWithInvalidLanguageCodes() {
        log.debug("Finding preferences with invalid language codes");
        return userPreferenceRepository.findPreferencesWithInvalidLanguageCodes();
    }

    /**
     * Find preferences with invalid timezones
     *
     * @return List of preferences with invalid timezones
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findPreferencesWithInvalidTimezones() {
        log.debug("Finding preferences with invalid timezones");
        return userPreferenceRepository.findPreferencesWithInvalidTimezones();
    }

    /**
     * Find preferences with invalid currency codes
     *
     * @return List of preferences with invalid currency codes
     */
    @Transactional(readOnly = true)
    public List<UserPreference> findPreferencesWithInvalidCurrencyCodes() {
        log.debug("Finding preferences with invalid currency codes");
        return userPreferenceRepository.findPreferencesWithInvalidCurrencyCodes();
    }

    // === Validation and Helper Methods ===

    /**
     * Set default preferences if not provided
     *
     * @param preferences Preferences to set defaults for
     */
    private void setDefaultPreferences(UserPreference preferences) {
        if (!StringUtils.hasText(preferences.getLanguage())) {
            preferences.setLanguage("en-US");
        }

        if (!StringUtils.hasText(preferences.getTimezone())) {
            preferences.setTimezone("UTC");
        }

        if (!StringUtils.hasText(preferences.getCurrency())) {
            preferences.setCurrency("USD");
        }
    }

    /**
     * Validate preferences data
     *
     * @param preferences Preferences to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validatePreferences(UserPreference preferences) {
        if (preferences.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        if (preferences.getLanguage() != null) {
            validateLanguageCode(preferences.getLanguage());
        }

        if (preferences.getTimezone() != null) {
            validateTimezone(preferences.getTimezone());
        }

        if (preferences.getCurrency() != null) {
            validateCurrencyCode(preferences.getCurrency());
        }
    }

    /**
     * Validate language code format
     *
     * @param languageCode Language code to validate
     * @throws IllegalArgumentException if invalid
     */
    private void validateLanguageCode(String languageCode) {
        if (!VALID_LANGUAGE_CODES.contains(languageCode)) {
            throw new IllegalArgumentException("Invalid language code: " + languageCode +
                                             ". Valid codes: " + VALID_LANGUAGE_CODES);
        }
    }

    /**
     * Validate timezone identifier
     *
     * @param timezone Timezone to validate
     * @throws IllegalArgumentException if invalid
     */
    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timezone: " + timezone, e);
        }
    }

    /**
     * Validate currency code
     *
     * @param currencyCode Currency code to validate
     * @throws IllegalArgumentException if invalid
     */
    private void validateCurrencyCode(String currencyCode) {
        if (!VALID_CURRENCY_CODES.contains(currencyCode)) {
            throw new IllegalArgumentException("Invalid currency code: " + currencyCode +
                                             ". Valid codes: " + VALID_CURRENCY_CODES);
        }

        try {
            Currency.getInstance(currencyCode);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid currency code: " + currencyCode, e);
        }
    }

    /**
     * Validate unique constraints
     *
     * @param preferences Preferences to validate
     * @throws IllegalArgumentException if constraint violation
     */
    private void validateUniqueConstraints(UserPreference preferences) {
        if (userPreferenceRepository.existsByUserId(preferences.getUserId().toString())) {
            throw new IllegalArgumentException("Preferences already exist for user: " + preferences.getUserId());
        }
    }

    /**
     * Check if preferences exist for user
     *
     * @param userId User ID to check
     * @return true if preferences exist
     */
    @Transactional(readOnly = true)
    public boolean existsByUserId(String userId) {
        return userPreferenceRepository.existsByUserId(userId);
    }

    /**
     * Get valid language codes
     *
     * @return Set of valid language codes
     */
    public Set<String> getValidLanguageCodes() {
        return VALID_LANGUAGE_CODES;
    }

    /**
     * Get valid currency codes
     *
     * @return Set of valid currency codes
     */
    public Set<String> getValidCurrencyCodes() {
        return VALID_CURRENCY_CODES;
    }
}