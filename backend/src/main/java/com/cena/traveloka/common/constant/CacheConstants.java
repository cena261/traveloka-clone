package com.cena.traveloka.common.constant;

public final class CacheConstants {

    private CacheConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }


    public static final String USER_PROFILES = "user_profiles";

    public static final String USER_SESSIONS = "user_sessions";

    public static final String USER_ROLES = "user_roles";

    public static final String AUTH_TOKENS = "auth_tokens";


    public static final String HOTEL_DETAILS = "hotel_details";

    public static final String HOTEL_ROOMS = "hotel_rooms";

    public static final String FLIGHT_DETAILS = "flight_details";

    public static final String ACCOMMODATIONS = "accommodations";


    public static final String LOCATIONS = "locations";

    public static final String CITIES = "cities";

    public static final String COUNTRIES = "countries";

    public static final String COORDINATES = "coordinates";


    public static final String IMAGE_URLS = "image_urls";

    public static final String MEDIA_METADATA = "media_metadata";


    public static final String PRICING = "pricing";

    public static final String PROMOTIONS = "promotions";

    public static final String DYNAMIC_PRICING = "dynamic_pricing";


    public static final String AVAILABILITY = "availability";

    public static final String ROOM_AVAILABILITY = "room_availability";

    public static final String FLIGHT_AVAILABILITY = "flight_availability";


    public static final String BOOKING_DETAILS = "booking_details";

    public static final String BOOKING_HISTORY = "booking_history";

    public static final String PENDING_BOOKINGS = "pending_bookings";


    public static final String PAYMENT_METHODS = "payment_methods";

    public static final String PAYMENT_TRANSACTIONS = "payment_transactions";


    public static final String REVIEWS = "reviews";

    public static final String REVIEW_RATINGS = "review_ratings";

    public static final String REVIEW_SUMMARIES = "review_summaries";


    public static final String NOTIFICATION_TEMPLATES = "notification_templates";

    public static final String NOTIFICATION_PREFERENCES = "notification_preferences";


    public static final String ANALYTICS_REPORTS = "analytics_reports";

    public static final String STATISTICS = "statistics";


    public static final String CONFIGURATIONS = "configurations";

    public static final String LOOKUPS = "lookups";

    public static final String SEARCH_RESULTS = "search_results";


    public static final long TTL_SHORT = 300L;

    public static final long TTL_MEDIUM = 1800L;

    public static final long TTL_LONG = 3600L;

    public static final long TTL_EXTRA_LONG = 86400L;

    public static final long TTL_MANUAL = -1L;


    public static final int DEFAULT_PAGE_SIZE = 20;

    public static final int MAX_PAGE_SIZE = 100;

    public static final int MAX_ENTRIES_PER_CACHE = 10000;

    public static final String KEY_SEPARATOR = ":";

    public static final String CACHE_PREFIX = "traveloka";


    public static final String UNLESS_NULL = "#result == null";

    public static final String UNLESS_EMPTY = "#result == null or #result.isEmpty()";

    public static final String CONDITION_NOT_NULL = "#p0 != null";


    public static final long TTL_USER_SESSION = 900L;

    public static final long TTL_USER_PROFILE = 1800L;

    public static final long TTL_ROLE_PERMISSIONS = 3600L;

    public static final long TTL_SYNC_EVENTS = 300L;

    public static final long TTL_AVAILABILITY = 300L;

    public static final long TTL_PRICING = 600L;

    public static final long TTL_REVIEWS = 3600L;

    public static final long TTL_STATIC_CONTENT = 86400L;
}