package com.cena.traveloka.catalog.inventory.util;

public final class InventoryConstants {

    private InventoryConstants() {
        // Utility class
    }

    // Cache names
    public static final String CACHE_PARTNERS = "partners";
    public static final String CACHE_PROPERTIES = "properties";
    public static final String CACHE_PROPERTY_IMAGES = "propertyImages";
    public static final String CACHE_ROOM_TYPES = "roomTypes";
    public static final String CACHE_ROOM_UNITS = "roomUnits";
    public static final String CACHE_AMENITIES = "amenities";
    public static final String CACHE_PROPERTY_AMENITIES = "propertyAmenities";

    // Default values
    public static final String DEFAULT_CURRENCY = "VND";
    public static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";
    public static final int DEFAULT_STAR_RATING = 3;
    public static final int DEFAULT_COMMISSION_RATE = 15;

    // Validation patterns
    public static final String PROPERTY_CODE_PATTERN = "^[A-Z0-9][A-Z0-9_-]{2,19}$";
    public static final String ROOM_CODE_PATTERN = "^[A-Za-z0-9\\-]+$";
    public static final String AMENITY_CODE_PATTERN = "^[A-Z0-9_]+$";

    // File upload limits
    public static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final int MAX_IMAGES_PER_UPLOAD = 20;
    public static final String[] ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/webp"};

    // Search limits
    public static final int MAX_SEARCH_RADIUS_KM = 100;
    public static final int DEFAULT_SEARCH_RADIUS_KM = 25;
    public static final int MAX_SEARCH_RESULTS = 100;
    public static final int DEFAULT_PAGE_SIZE = 20;

    // Business rules
    public static final int MIN_ADULT_CAPACITY = 1;
    public static final int MAX_ADULT_CAPACITY = 8;
    public static final int MAX_CHILD_CAPACITY = 4;
    public static final int MAX_INFANT_CAPACITY = 2;
    public static final int MAX_TOTAL_OCCUPANCY = 10;

    // Performance thresholds
    public static final long SEARCH_PERFORMANCE_THRESHOLD_MS = 500;
    public static final int HIGH_PERFORMANCE_MIN_BOOKINGS = 100;
    public static final double HIGH_PERFORMANCE_MIN_RATING = 4.0;

    // Contract expiration warning
    public static final int CONTRACT_EXPIRATION_WARNING_DAYS = 30;

    // Error messages
    public static final String ERROR_PARTNER_NOT_FOUND = "Partner not found";
    public static final String ERROR_PROPERTY_NOT_FOUND = "Property not found";
    public static final String ERROR_ROOM_TYPE_NOT_FOUND = "Room type not found";
    public static final String ERROR_ROOM_UNIT_NOT_FOUND = "Room unit not found";
    public static final String ERROR_AMENITY_NOT_FOUND = "Amenity not found";
    public static final String ERROR_IMAGE_NOT_FOUND = "Image not found";
}