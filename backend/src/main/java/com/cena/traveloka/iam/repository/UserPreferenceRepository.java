package com.cena.traveloka.iam.repository;

import com.cena.traveloka.iam.entity.UserPreference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserPreference entity operations
 *
 * Provides operations for managing user preferences and settings including:
 * - Language and localization preferences
 * - Notification settings management
 * - Booking preferences and defaults
 * - Privacy and accessibility settings
 * - Preference analytics and reporting
 *
 * Key Features:
 * - User-preference relationship queries
 * - JSON-based preference filtering
 * - Locale and language analytics
 * - Notification preference management
 * - Bulk preference updates
 */
@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, String> {

    // === Basic User Preference Relationships ===

    /**
     * Find preferences by user ID - primary lookup method
     * Uses unique index on user_id for optimal performance
     */
    @Query("SELECT p FROM UserPreference p WHERE p.userId = :userId")
    Optional<UserPreference> findByUserId(@Param("userId") String userId);

    /**
     * Check if preferences exist for user
     */
    boolean existsByUserId(String userId);

    /**
     * Delete preferences by user ID
     */
    @Modifying
    @Query("DELETE FROM UserPreference p WHERE p.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);

    // === Language and Localization ===

    /**
     * Find preferences by language for localization analytics
     * Uses index on language for performance
     */
    @Query("SELECT p FROM UserPreference p WHERE p.language = :language")
    Page<UserPreference> findByLanguage(@Param("language") String language, Pageable pageable);

    /**
     * Count preferences by language for analytics
     */
    @Query("SELECT p.language, COUNT(p) FROM UserPreference p WHERE p.language IS NOT NULL GROUP BY p.language")
    List<Object[]> countByLanguage();

    /**
     * Find preferences by timezone for regional analysis
     */
    @Query("SELECT p FROM UserPreference p WHERE p.timezone = :timezone")
    Page<UserPreference> findByTimezone(@Param("timezone") String timezone, Pageable pageable);

    /**
     * Count preferences by timezone
     */
    @Query("SELECT p.timezone, COUNT(p) FROM UserPreference p WHERE p.timezone IS NOT NULL GROUP BY p.timezone")
    List<Object[]> countByTimezone();

    /**
     * Find preferences by currency for financial analytics
     */
    @Query("SELECT p FROM UserPreference p WHERE p.currency = :currency")
    Page<UserPreference> findByCurrency(@Param("currency") String currency, Pageable pageable);

    // === Notification Preferences ===

    /**
     * Find users who opted in for email notifications
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "JSON_EXTRACT(p.notificationPreferences, '$.email.enabled') = true")
    List<UserPreference> findUsersWithEmailNotificationsEnabled();

    /**
     * Find users who opted in for promotional emails
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "JSON_EXTRACT(p.notificationPreferences, '$.email.promotions') = true")
    List<UserPreference> findUsersOptedInForPromotions();

    /**
     * Find users who enabled SMS notifications
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "JSON_EXTRACT(p.notificationPreferences, '$.sms.enabled') = true")
    List<UserPreference> findUsersWithSmsNotificationsEnabled();

    /**
     * Find users who enabled push notifications
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "JSON_EXTRACT(p.notificationPreferences, '$.push.enabled') = true")
    List<UserPreference> findUsersWithPushNotificationsEnabled();

    /**
     * Find users by specific notification preference
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "JSON_EXTRACT(p.notificationPreferences, :jsonPath) = :value")
    List<UserPreference> findByNotificationPreference(
            @Param("jsonPath") String jsonPath,
            @Param("value") Object value);

    // === Booking Preferences ===

    /**
     * Find users by default guest count for targeted offers
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "JSON_EXTRACT(p.bookingPreferences, '$.defaultGuestCount') = :guestCount")
    List<UserPreference> findByDefaultGuestCount(@Param("guestCount") Integer guestCount);

    /**
     * Find users by preferred room type
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "JSON_EXTRACT(p.bookingPreferences, '$.preferredRoomType') = :roomType")
    List<UserPreference> findByPreferredRoomType(@Param("roomType") String roomType);

    /**
     * Find users with smoking preference
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "JSON_EXTRACT(p.bookingPreferences, '$.smokingPreference') = :smokingPreference")
    List<UserPreference> findBySmokingPreference(@Param("smokingPreference") String smokingPreference);

    /**
     * Find users by bed type preference
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "JSON_EXTRACT(p.bookingPreferences, '$.bedType') = :bedType")
    List<UserPreference> findByBedTypePreference(@Param("bedType") String bedType);

    // === Privacy Settings ===

    /**
     * Find users with public profiles
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "JSON_EXTRACT(p.privacySettings, '$.profileVisibility') = 'PUBLIC'")
    List<UserPreference> findUsersWithPublicProfiles();

    /**
     * Find users who opted out of data sharing
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "JSON_EXTRACT(p.privacySettings, '$.dataSharing') = false")
    List<UserPreference> findUsersOptedOutOfDataSharing();

    /**
     * Find users who disabled analytics tracking
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "JSON_EXTRACT(p.privacySettings, '$.analytics') = false")
    List<UserPreference> findUsersWithAnalyticsDisabled();

    // === Accessibility Settings ===

    /**
     * Find users with accessibility requirements
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "SIZE(p.accessibilityOptions) > 0")
    List<UserPreference> findUsersWithAccessibilityOptions();

    /**
     * Find users by specific accessibility requirement
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "KEY(p.accessibilityOptions) = :requirement OR VALUE(p.accessibilityOptions) = :requirement")
    List<UserPreference> findByAccessibilityRequirement(@Param("requirement") String requirement);

    // === Search and Filtering ===

    /**
     * Search preferences by multiple criteria
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "(:language IS NULL OR p.language = :language) AND " +
           "(:timezone IS NULL OR p.timezone = :timezone) AND " +
           "(:currency IS NULL OR p.currency = :currency)")
    Page<UserPreference> searchPreferences(
            @Param("language") String language,
            @Param("timezone") String timezone,
            @Param("currency") String currency,
            Pageable pageable);

    /**
     * Find preferences with incomplete localization settings
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "p.language IS NULL OR p.timezone IS NULL OR p.currency IS NULL")
    List<UserPreference> findIncompleteLocalizationPreferences();

    // === Time-based Queries ===

    /**
     * Find recently created preferences for onboarding analysis
     */
    @Query("SELECT p FROM UserPreference p WHERE p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<UserPreference> findRecentlyCreatedPreferences(@Param("since") Instant since);

    /**
     * Find preferences created within date range
     */
    @Query("SELECT p FROM UserPreference p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    Page<UserPreference> findPreferencesCreatedBetween(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    /**
     * Find preferences not updated recently
     */
    @Query("SELECT p FROM UserPreference p WHERE p.updatedAt < :threshold")
    List<UserPreference> findStalePreferences(@Param("threshold") Instant threshold);

    // === Bulk Operations ===

    /**
     * Bulk update language preferences for migration
     */
    @Modifying
    @Query("UPDATE UserPreference p SET p.language = :newLanguage, " +
           "p.updatedAt = CURRENT_TIMESTAMP, p.updatedBy = :updatedBy " +
           "WHERE p.language = :oldLanguage")
    int updateLanguageForUsers(
            @Param("oldLanguage") String oldLanguage,
            @Param("newLanguage") String newLanguage,
            @Param("updatedBy") String updatedBy);

    /**
     * Bulk update timezone for regional changes
     */
    @Modifying
    @Query("UPDATE UserPreference p SET p.timezone = :newTimezone, " +
           "p.updatedAt = CURRENT_TIMESTAMP, p.updatedBy = :updatedBy " +
           "WHERE p.timezone = :oldTimezone")
    int updateTimezoneForUsers(
            @Param("oldTimezone") String oldTimezone,
            @Param("newTimezone") String newTimezone,
            @Param("updatedBy") String updatedBy);

    /**
     * Bulk touch preferences for cache invalidation
     */
    @Modifying
    @Query("UPDATE UserPreference p SET p.updatedAt = CURRENT_TIMESTAMP, p.updatedBy = :updatedBy " +
           "WHERE p.userId IN :userIds")
    int touchPreferencesForUsers(
            @Param("userIds") List<String> userIds,
            @Param("updatedBy") String updatedBy);

    // === Analytics and Reporting ===

    /**
     * Get preference statistics for dashboard
     */
    @Query("SELECT " +
           "COUNT(*) as totalPreferences, " +
           "COUNT(CASE WHEN p.language IS NOT NULL THEN 1 END) as preferencesWithLanguage, " +
           "COUNT(CASE WHEN p.timezone IS NOT NULL THEN 1 END) as preferencesWithTimezone, " +
           "COUNT(CASE WHEN p.currency IS NOT NULL THEN 1 END) as preferencesWithCurrency, " +
           "COUNT(DISTINCT p.language) as uniqueLanguages, " +
           "COUNT(DISTINCT p.timezone) as uniqueTimezones " +
           "FROM UserPreference p")
    PreferenceStatistics getPreferenceStatistics();

    /**
     * Interface for preference statistics projection
     */
    interface PreferenceStatistics {
        long getTotalPreferences();
        long getPreferencesWithLanguage();
        long getPreferencesWithTimezone();
        long getPreferencesWithCurrency();
        long getUniqueLanguages();
        long getUniqueTimezones();
    }

    /**
     * Get notification preference statistics
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN JSON_EXTRACT(p.notificationPreferences, '$.email.enabled') = true THEN 1 END) as emailEnabled, " +
           "COUNT(CASE WHEN JSON_EXTRACT(p.notificationPreferences, '$.sms.enabled') = true THEN 1 END) as smsEnabled, " +
           "COUNT(CASE WHEN JSON_EXTRACT(p.notificationPreferences, '$.push.enabled') = true THEN 1 END) as pushEnabled, " +
           "COUNT(CASE WHEN JSON_EXTRACT(p.notificationPreferences, '$.email.promotions') = true THEN 1 END) as promotionsEnabled " +
           "FROM UserPreference p")
    NotificationStatistics getNotificationStatistics();

    /**
     * Interface for notification statistics projection
     */
    interface NotificationStatistics {
        long getEmailEnabled();
        long getSmsEnabled();
        long getPushEnabled();
        long getPromotionsEnabled();
    }

    // === Data Quality and Validation ===

    /**
     * Find preferences with invalid language codes
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "p.language IS NOT NULL AND " +
           "LENGTH(p.language) != 5 AND " +
           "p.language NOT LIKE '^[a-z]{2}-[A-Z]{2}$'")
    List<UserPreference> findPreferencesWithInvalidLanguageCodes();

    /**
     * Find preferences with invalid timezone
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "p.timezone IS NOT NULL AND " +
           "p.timezone NOT LIKE '^[A-Za-z_/]+$'")
    List<UserPreference> findPreferencesWithInvalidTimezones();

    /**
     * Find preferences with invalid currency codes
     */
    @Query("SELECT p FROM UserPreference p WHERE " +
           "p.currency IS NOT NULL AND " +
           "LENGTH(p.currency) != 3")
    List<UserPreference> findPreferencesWithInvalidCurrencyCodes();
}
