package com.cena.traveloka.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Utility class for date and time operations with timezone-aware conversions and UTC handling.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Timezone conversions between different zones</li>
 *   <li>UTC normalization for database storage</li>
 *   <li>Date/time formatting and parsing</li>
 *   <li>Business day calculations</li>
 *   <li>Asia/Ho_Chi_Minh (Vietnam) timezone utilities</li>
 * </ul>
 *
 * <p>All entity timestamps are stored in UTC and converted to local timezones when needed.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * ZonedDateTime utcTime = DateTimeUtils.nowUtc();
 * ZonedDateTime vnTime = DateTimeUtils.convertToTimezone(utcTime, "Asia/Ho_Chi_Minh");
 * String formatted = DateTimeUtils.formatDateTime(vnTime, "dd/MM/yyyy HH:mm");
 * </pre>
 *
 * @since 1.0.0
 */
public final class DateTimeUtils {

    /**
     * Vietnam timezone constant
     */
    public static final ZoneId VIETNAM_TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /**
     * UTC timezone constant
     */
    public static final ZoneId UTC_TIMEZONE = ZoneOffset.UTC;

    /**
     * ISO 8601 date-time formatter (yyyy-MM-dd'T'HH:mm:ss'Z')
     */
    public static final DateTimeFormatter ISO_DATETIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(UTC_TIMEZONE);

    /**
     * Standard date formatter (dd/MM/yyyy)
     */
    public static final DateTimeFormatter STANDARD_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Standard date-time formatter (dd/MM/yyyy HH:mm:ss)
     */
    public static final DateTimeFormatter STANDARD_DATETIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Private constructor to prevent instantiation
     */
    private DateTimeUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets the current date-time in UTC timezone.
     *
     * @return Current ZonedDateTime in UTC
     */
    public static ZonedDateTime nowUtc() {
        return ZonedDateTime.now(UTC_TIMEZONE);
    }

    /**
     * Gets the current date-time in Vietnam timezone.
     *
     * @return Current ZonedDateTime in Asia/Ho_Chi_Minh
     */
    public static ZonedDateTime nowVietnam() {
        return ZonedDateTime.now(VIETNAM_TIMEZONE);
    }

    /**
     * Converts a ZonedDateTime to the specified timezone.
     *
     * @param dateTime The ZonedDateTime to convert
     * @param targetTimezone Target timezone ID (e.g., "Asia/Ho_Chi_Minh", "UTC")
     * @return ZonedDateTime in the target timezone
     * @throws IllegalArgumentException if dateTime is null or timezone is invalid
     */
    public static ZonedDateTime convertToTimezone(ZonedDateTime dateTime, String targetTimezone) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        Objects.requireNonNull(targetTimezone, "Target timezone cannot be null");

        try {
            ZoneId targetZone = ZoneId.of(targetTimezone);
            return dateTime.withZoneSameInstant(targetZone);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid timezone: " + targetTimezone, e);
        }
    }

    /**
     * Converts a ZonedDateTime to UTC timezone.
     *
     * @param dateTime The ZonedDateTime to convert
     * @return ZonedDateTime in UTC
     * @throws IllegalArgumentException if dateTime is null
     */
    public static ZonedDateTime convertToUtc(ZonedDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.withZoneSameInstant(UTC_TIMEZONE);
    }

    /**
     * Converts a ZonedDateTime to Vietnam timezone.
     *
     * @param dateTime The ZonedDateTime to convert
     * @return ZonedDateTime in Asia/Ho_Chi_Minh
     * @throws IllegalArgumentException if dateTime is null
     */
    public static ZonedDateTime convertToVietnam(ZonedDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.withZoneSameInstant(VIETNAM_TIMEZONE);
    }

    /**
     * Formats a ZonedDateTime using the specified pattern.
     *
     * @param dateTime The ZonedDateTime to format
     * @param pattern The format pattern (e.g., "dd/MM/yyyy HH:mm:ss")
     * @return Formatted date-time string
     * @throws IllegalArgumentException if dateTime or pattern is null
     */
    public static String formatDateTime(ZonedDateTime dateTime, String pattern) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        Objects.requireNonNull(pattern, "Pattern cannot be null");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return dateTime.format(formatter);
    }

    /**
     * Formats a ZonedDateTime using ISO 8601 format.
     *
     * @param dateTime The ZonedDateTime to format
     * @return ISO 8601 formatted string (yyyy-MM-dd'T'HH:mm:ss'Z')
     * @throws IllegalArgumentException if dateTime is null
     */
    public static String formatIsoDateTime(ZonedDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.withZoneSameInstant(UTC_TIMEZONE).format(ISO_DATETIME_FORMATTER);
    }

    /**
     * Parses a date-time string using the specified pattern.
     *
     * @param dateTimeStr The date-time string to parse
     * @param pattern The format pattern
     * @return Parsed ZonedDateTime
     * @throws IllegalArgumentException if dateTimeStr or pattern is null
     * @throws DateTimeParseException if parsing fails
     */
    public static ZonedDateTime parseDateTime(String dateTimeStr, String pattern) {
        Objects.requireNonNull(dateTimeStr, "DateTime string cannot be null");
        Objects.requireNonNull(pattern, "Pattern cannot be null");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, formatter);
        return ZonedDateTime.of(localDateTime, UTC_TIMEZONE);
    }

    /**
     * Parses an ISO 8601 formatted date-time string.
     *
     * @param isoDateTimeStr ISO 8601 formatted string
     * @return Parsed ZonedDateTime in UTC
     * @throws IllegalArgumentException if isoDateTimeStr is null
     * @throws DateTimeParseException if parsing fails
     */
    public static ZonedDateTime parseIsoDateTime(String isoDateTimeStr) {
        Objects.requireNonNull(isoDateTimeStr, "ISO DateTime string cannot be null");
        return ZonedDateTime.parse(isoDateTimeStr, ISO_DATETIME_FORMATTER);
    }

    /**
     * Converts LocalDateTime to ZonedDateTime in UTC.
     *
     * @param localDateTime The LocalDateTime to convert
     * @return ZonedDateTime in UTC
     * @throws IllegalArgumentException if localDateTime is null
     */
    public static ZonedDateTime toUtcZonedDateTime(LocalDateTime localDateTime) {
        Objects.requireNonNull(localDateTime, "LocalDateTime cannot be null");
        return ZonedDateTime.of(localDateTime, UTC_TIMEZONE);
    }

    /**
     * Converts LocalDateTime to ZonedDateTime in the specified timezone.
     *
     * @param localDateTime The LocalDateTime to convert
     * @param timezone Target timezone ID
     * @return ZonedDateTime in the specified timezone
     * @throws IllegalArgumentException if localDateTime or timezone is null or invalid
     */
    public static ZonedDateTime toZonedDateTime(LocalDateTime localDateTime, String timezone) {
        Objects.requireNonNull(localDateTime, "LocalDateTime cannot be null");
        Objects.requireNonNull(timezone, "Timezone cannot be null");

        try {
            ZoneId zoneId = ZoneId.of(timezone);
            return ZonedDateTime.of(localDateTime, zoneId);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid timezone: " + timezone, e);
        }
    }

    /**
     * Checks if a date is in the past.
     *
     * @param dateTime The ZonedDateTime to check
     * @return true if the date is before now (UTC)
     * @throws IllegalArgumentException if dateTime is null
     */
    public static boolean isPast(ZonedDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.isBefore(nowUtc());
    }

    /**
     * Checks if a date is in the future.
     *
     * @param dateTime The ZonedDateTime to check
     * @return true if the date is after now (UTC)
     * @throws IllegalArgumentException if dateTime is null
     */
    public static boolean isFuture(ZonedDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.isAfter(nowUtc());
    }

    /**
     * Calculates the number of days between two dates.
     *
     * @param startDate Start date
     * @param endDate End date
     * @return Number of days between the dates (can be negative)
     * @throws IllegalArgumentException if either date is null
     */
    public static long daysBetween(ZonedDateTime startDate, ZonedDateTime endDate) {
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(endDate, "End date cannot be null");

        return Duration.between(startDate.toLocalDate().atStartOfDay(),
                               endDate.toLocalDate().atStartOfDay()).toDays();
    }

    /**
     * Adds days to a ZonedDateTime.
     *
     * @param dateTime The ZonedDateTime to modify
     * @param days Number of days to add (can be negative)
     * @return New ZonedDateTime with days added
     * @throws IllegalArgumentException if dateTime is null
     */
    public static ZonedDateTime addDays(ZonedDateTime dateTime, long days) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.plusDays(days);
    }

    /**
     * Adds hours to a ZonedDateTime.
     *
     * @param dateTime The ZonedDateTime to modify
     * @param hours Number of hours to add (can be negative)
     * @return New ZonedDateTime with hours added
     * @throws IllegalArgumentException if dateTime is null
     */
    public static ZonedDateTime addHours(ZonedDateTime dateTime, long hours) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.plusHours(hours);
    }

    /**
     * Gets the start of day (00:00:00) for a ZonedDateTime.
     *
     * @param dateTime The ZonedDateTime
     * @return ZonedDateTime at start of day
     * @throws IllegalArgumentException if dateTime is null
     */
    public static ZonedDateTime startOfDay(ZonedDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.toLocalDate().atStartOfDay(dateTime.getZone());
    }

    /**
     * Gets the end of day (23:59:59.999999999) for a ZonedDateTime.
     *
     * @param dateTime The ZonedDateTime
     * @return ZonedDateTime at end of day
     * @throws IllegalArgumentException if dateTime is null
     */
    public static ZonedDateTime endOfDay(ZonedDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.toLocalDate().atTime(LocalTime.MAX).atZone(dateTime.getZone());
    }

    /**
     * Checks if a date is a weekend (Saturday or Sunday).
     *
     * @param dateTime The ZonedDateTime to check
     * @return true if the date is Saturday or Sunday
     * @throws IllegalArgumentException if dateTime is null
     */
    public static boolean isWeekend(ZonedDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * Checks if a date is a weekday (Monday to Friday).
     *
     * @param dateTime The ZonedDateTime to check
     * @return true if the date is a weekday
     * @throws IllegalArgumentException if dateTime is null
     */
    public static boolean isWeekday(ZonedDateTime dateTime) {
        return !isWeekend(dateTime);
    }

    /**
     * Converts milliseconds since epoch to ZonedDateTime in UTC.
     *
     * @param epochMilli Milliseconds since Unix epoch
     * @return ZonedDateTime in UTC
     */
    public static ZonedDateTime fromEpochMilli(long epochMilli) {
        return Instant.ofEpochMilli(epochMilli).atZone(UTC_TIMEZONE);
    }

    /**
     * Converts ZonedDateTime to milliseconds since epoch.
     *
     * @param dateTime The ZonedDateTime to convert
     * @return Milliseconds since Unix epoch
     * @throws IllegalArgumentException if dateTime is null
     */
    public static long toEpochMilli(ZonedDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.toInstant().toEpochMilli();
    }
}