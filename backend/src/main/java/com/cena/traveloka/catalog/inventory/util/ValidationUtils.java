package com.cena.traveloka.catalog.inventory.util;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^\\+?[1-9]\\d{1,14}$"
    );

    private static final Pattern PROPERTY_CODE_PATTERN = Pattern.compile(
        InventoryConstants.PROPERTY_CODE_PATTERN
    );

    private static final Pattern ROOM_CODE_PATTERN = Pattern.compile(
        InventoryConstants.ROOM_CODE_PATTERN
    );

    /**
     * Validates email format
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validates phone number format (international format)
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && PHONE_PATTERN.matcher(phoneNumber.trim()).matches();
    }

    /**
     * Validates property code format
     */
    public static boolean isValidPropertyCode(String propertyCode) {
        return propertyCode != null && PROPERTY_CODE_PATTERN.matcher(propertyCode.trim()).matches();
    }

    /**
     * Validates room code format
     */
    public static boolean isValidRoomCode(String roomCode) {
        return roomCode != null && ROOM_CODE_PATTERN.matcher(roomCode.trim()).matches();
    }

    /**
     * Validates geographic coordinates
     */
    public static boolean isValidLatitude(Double latitude) {
        return latitude != null && latitude >= -90.0 && latitude <= 90.0;
    }

    public static boolean isValidLongitude(Double longitude) {
        return longitude != null && longitude >= -180.0 && longitude <= 180.0;
    }

    /**
     * Validates star rating
     */
    public static boolean isValidStarRating(Integer starRating) {
        return starRating != null && starRating >= 1 && starRating <= 5;
    }

    /**
     * Validates commission rate
     */
    public static boolean isValidCommissionRate(Double commissionRate) {
        return commissionRate != null && commissionRate >= 0.0 && commissionRate <= 100.0;
    }

    /**
     * Validates occupancy values
     */
    public static boolean isValidAdultCapacity(Integer adultCapacity) {
        return adultCapacity != null &&
               adultCapacity >= InventoryConstants.MIN_ADULT_CAPACITY &&
               adultCapacity <= InventoryConstants.MAX_ADULT_CAPACITY;
    }

    public static boolean isValidChildCapacity(Integer childCapacity) {
        return childCapacity != null &&
               childCapacity >= 0 &&
               childCapacity <= InventoryConstants.MAX_CHILD_CAPACITY;
    }

    public static boolean isValidInfantCapacity(Integer infantCapacity) {
        return infantCapacity != null &&
               infantCapacity >= 0 &&
               infantCapacity <= InventoryConstants.MAX_INFANT_CAPACITY;
    }

    public static boolean isValidTotalOccupancy(Integer maxOccupancy) {
        return maxOccupancy != null &&
               maxOccupancy >= 1 &&
               maxOccupancy <= InventoryConstants.MAX_TOTAL_OCCUPANCY;
    }

    /**
     * Validates image file type
     */
    public static boolean isValidImageType(String contentType) {
        if (contentType == null) return false;

        for (String allowedType : InventoryConstants.ALLOWED_IMAGE_TYPES) {
            if (allowedType.equals(contentType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates file size
     */
    public static boolean isValidImageSize(long sizeBytes) {
        return sizeBytes > 0 && sizeBytes <= InventoryConstants.MAX_IMAGE_SIZE;
    }

    /**
     * Sanitizes string input
     */
    public static String sanitizeString(String input) {
        if (input == null) return null;
        return input.trim().replaceAll("\\s+", " ");
    }

    /**
     * Validates search radius
     */
    public static boolean isValidSearchRadius(double radiusKm) {
        return radiusKm > 0 && radiusKm <= InventoryConstants.MAX_SEARCH_RADIUS_KM;
    }
}