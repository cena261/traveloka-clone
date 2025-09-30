package com.cena.traveloka.common.constant;

/**
 * Constants for cache names and configuration values used throughout the application.
 *
 * <p>Cache naming convention follows the pattern: MODULE_ENTITY</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Centralized cache name definitions</li>
 *   <li>Module-specific cache names for all 11 business modules</li>
 *   <li>Common cache configuration values</li>
 *   <li>TTL (Time-To-Live) constants</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>
 * {@code @Cacheable}(value = CacheConstants.USER_PROFILES, key = "#userId")
 * public UserDto getUserProfile(UUID userId) {
 *     // Method implementation
 * }
 * </pre>
 *
 * @since 1.0.0
 */
public final class CacheConstants {

    /**
     * Private constructor to prevent instantiation
     */
    private CacheConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    // ========== IAM Module Cache Names ==========

    /**
     * Cache for user profiles
     */
    public static final String USER_PROFILES = "user_profiles";

    /**
     * Cache for user sessions
     */
    public static final String USER_SESSIONS = "user_sessions";

    /**
     * Cache for user roles and permissions
     */
    public static final String USER_ROLES = "user_roles";

    /**
     * Cache for authentication tokens
     */
    public static final String AUTH_TOKENS = "auth_tokens";

    // ========== Inventory Module Cache Names ==========

    /**
     * Cache for hotel details
     */
    public static final String HOTEL_DETAILS = "hotel_details";

    /**
     * Cache for hotel rooms
     */
    public static final String HOTEL_ROOMS = "hotel_rooms";

    /**
     * Cache for flight details
     */
    public static final String FLIGHT_DETAILS = "flight_details";

    /**
     * Cache for accommodation listings
     */
    public static final String ACCOMMODATIONS = "accommodations";

    // ========== Geo Module Cache Names ==========

    /**
     * Cache for location data
     */
    public static final String LOCATIONS = "locations";

    /**
     * Cache for city information
     */
    public static final String CITIES = "cities";

    /**
     * Cache for country information
     */
    public static final String COUNTRIES = "countries";

    /**
     * Cache for map coordinates
     */
    public static final String COORDINATES = "coordinates";

    // ========== Media Module Cache Names ==========

    /**
     * Cache for image URLs
     */
    public static final String IMAGE_URLS = "image_urls";

    /**
     * Cache for media metadata
     */
    public static final String MEDIA_METADATA = "media_metadata";

    // ========== Pricing Module Cache Names ==========

    /**
     * Cache for pricing information
     */
    public static final String PRICING = "pricing";

    /**
     * Cache for promotions and discounts
     */
    public static final String PROMOTIONS = "promotions";

    /**
     * Cache for dynamic pricing calculations
     */
    public static final String DYNAMIC_PRICING = "dynamic_pricing";

    // ========== Availability Module Cache Names ==========

    /**
     * Cache for availability checks
     */
    public static final String AVAILABILITY = "availability";

    /**
     * Cache for room availability
     */
    public static final String ROOM_AVAILABILITY = "room_availability";

    /**
     * Cache for flight availability
     */
    public static final String FLIGHT_AVAILABILITY = "flight_availability";

    // ========== Booking Module Cache Names ==========

    /**
     * Cache for booking details
     */
    public static final String BOOKING_DETAILS = "booking_details";

    /**
     * Cache for booking history
     */
    public static final String BOOKING_HISTORY = "booking_history";

    /**
     * Cache for pending bookings
     */
    public static final String PENDING_BOOKINGS = "pending_bookings";

    // ========== Payment Module Cache Names ==========

    /**
     * Cache for payment methods
     */
    public static final String PAYMENT_METHODS = "payment_methods";

    /**
     * Cache for payment transactions
     */
    public static final String PAYMENT_TRANSACTIONS = "payment_transactions";

    // ========== Review Module Cache Names ==========

    /**
     * Cache for reviews
     */
    public static final String REVIEWS = "reviews";

    /**
     * Cache for review ratings
     */
    public static final String REVIEW_RATINGS = "review_ratings";

    /**
     * Cache for review summaries
     */
    public static final String REVIEW_SUMMARIES = "review_summaries";

    // ========== Notify Module Cache Names ==========

    /**
     * Cache for notification templates
     */
    public static final String NOTIFICATION_TEMPLATES = "notification_templates";

    /**
     * Cache for user notification preferences
     */
    public static final String NOTIFICATION_PREFERENCES = "notification_preferences";

    // ========== Analytics Module Cache Names ==========

    /**
     * Cache for analytics reports
     */
    public static final String ANALYTICS_REPORTS = "analytics_reports";

    /**
     * Cache for statistics data
     */
    public static final String STATISTICS = "statistics";

    // ========== Common Cache Names ==========

    /**
     * Cache for configuration settings
     */
    public static final String CONFIGURATIONS = "configurations";

    /**
     * Cache for lookup data (enums, reference data)
     */
    public static final String LOOKUPS = "lookups";

    /**
     * Cache for search results
     */
    public static final String SEARCH_RESULTS = "search_results";

    // ========== Cache TTL Values (in seconds) ==========

    /**
     * Short TTL: 5 minutes (300 seconds)
     * Used for frequently changing data
     */
    public static final long TTL_SHORT = 300L;

    /**
     * Medium TTL: 30 minutes (1800 seconds)
     * Used for moderately changing data
     */
    public static final long TTL_MEDIUM = 1800L;

    /**
     * Long TTL: 1 hour (3600 seconds)
     * Used for relatively static data
     */
    public static final long TTL_LONG = 3600L;

    /**
     * Extra long TTL: 24 hours (86400 seconds)
     * Used for rarely changing data
     */
    public static final long TTL_EXTRA_LONG = 86400L;

    /**
     * No expiration (manual eviction only)
     * As per specification: manual cache eviction (no default TTL)
     */
    public static final long TTL_MANUAL = -1L;

    // ========== Cache Configuration Values ==========

    /**
     * Default page size for paginated cache entries
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Maximum page size for paginated cache entries
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Maximum number of cached entries per cache
     */
    public static final int MAX_ENTRIES_PER_CACHE = 10000;

    /**
     * Cache key prefix separator
     */
    public static final String KEY_SEPARATOR = ":";

    /**
     * Cache key prefix for the application
     */
    public static final String CACHE_PREFIX = "traveloka";

    // ========== Cache Operation Constants ==========

    /**
     * Cache operation: unless (condition to skip caching)
     * Skip caching if result is null
     */
    public static final String UNLESS_NULL = "#result == null";

    /**
     * Cache operation: unless (condition to skip caching)
     * Skip caching if result is empty collection
     */
    public static final String UNLESS_EMPTY = "#result == null or #result.isEmpty()";

    /**
     * Cache operation: condition
     * Cache only if parameter is not null
     */
    public static final String CONDITION_NOT_NULL = "#p0 != null";

    // ========== Module-specific TTL Recommendations ==========

    /**
     * Recommended TTL for user sessions (15 minutes)
     */
    public static final long TTL_USER_SESSION = 900L;

    /**
     * Recommended TTL for user profiles (30 minutes)
     */
    public static final long TTL_USER_PROFILE = 1800L;

    /**
     * Recommended TTL for role permissions (1 hour)
     */
    public static final long TTL_ROLE_PERMISSIONS = 3600L;

    /**
     * Recommended TTL for sync events (5 minutes)
     */
    public static final long TTL_SYNC_EVENTS = 300L;

    /**
     * Recommended TTL for availability checks (5 minutes)
     */
    public static final long TTL_AVAILABILITY = 300L;

    /**
     * Recommended TTL for pricing (10 minutes)
     */
    public static final long TTL_PRICING = 600L;

    /**
     * Recommended TTL for reviews (1 hour)
     */
    public static final long TTL_REVIEWS = 3600L;

    /**
     * Recommended TTL for static content (24 hours)
     */
    public static final long TTL_STATIC_CONTENT = 86400L;
}