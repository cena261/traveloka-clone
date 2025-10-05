package com.cena.traveloka.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public final class DateTimeUtils {

    public static final ZoneId VIETNAM_TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public static final ZoneId UTC_TIMEZONE = ZoneOffset.UTC;

    public static final DateTimeFormatter ISO_DATETIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(UTC_TIMEZONE);

    public static final DateTimeFormatter STANDARD_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static final DateTimeFormatter STANDARD_DATETIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private DateTimeUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static ZonedDateTime nowUtc() {
        return ZonedDateTime.now(UTC_TIMEZONE);
    }

    public static ZonedDateTime nowVietnam() {
        return ZonedDateTime.now(VIETNAM_TIMEZONE);
    }

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

    public static ZonedDateTime convertToUtc(ZonedDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.withZoneSameInstant(UTC_TIMEZONE);
    }

    public static ZonedDateTime convertToVietnam(ZonedDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.withZoneSameInstant(VIETNAM_TIMEZONE);
    }

    public static String formatDateTime(ZonedDateTime dateTime, String pattern) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        Objects.requireNonNull(pattern, "Pattern cannot be null");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return dateTime.format(formatter);
    }

    public static String formatIsoDateTime(ZonedDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.withZoneSameInstant(UTC_TIMEZONE).format(ISO_DATETIME_FORMATTER);
    }

    public static ZonedDateTime parseDateTime(String dateTimeStr, String pattern) {
        Objects.requireNonNull(dateTimeStr, "DateTime string cannot be null");
        Objects.requireNonNull(pattern, "Pattern cannot be null");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, formatter);
        return ZonedDateTime.of(localDateTime, UTC_TIMEZONE);
    }

    public static ZonedDateTime parseIsoDateTime(String isoDateTimeStr) {
        Objects.requireNonNull(isoDateTimeStr, "ISO DateTime string cannot be null");
        return ZonedDateTime.parse(isoDateTimeStr, ISO_DATETIME_FORMATTER);
    }

    public static ZonedDateTime toUtcZonedDateTime(LocalDateTime localDateTime) {
        Objects.requireNonNull(localDateTime, "LocalDateTime cannot be null");
        return ZonedDateTime.of(localDateTime, UTC_TIMEZONE);
    }

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

    public static boolean isPast(ZonedDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.isBefore(nowUtc());
    }

    public static boolean isFuture(ZonedDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.isAfter(nowUtc());
    }

    public static long daysBetween(ZonedDateTime startDate, ZonedDateTime endDate) {
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(endDate, "End date cannot be null");

        return Duration.between(startDate.toLocalDate().atStartOfDay(),
                               endDate.toLocalDate().atStartOfDay()).toDays();
    }

    public static ZonedDateTime addDays(ZonedDateTime dateTime, long days) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.plusDays(days);
    }

    public static ZonedDateTime addHours(ZonedDateTime dateTime, long hours) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.plusHours(hours);
    }

    public static ZonedDateTime startOfDay(ZonedDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.toLocalDate().atStartOfDay(dateTime.getZone());
    }

    public static ZonedDateTime endOfDay(ZonedDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.toLocalDate().atTime(LocalTime.MAX).atZone(dateTime.getZone());
    }

    public static boolean isWeekend(ZonedDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    public static boolean isWeekday(ZonedDateTime dateTime) {
        return !isWeekend(dateTime);
    }

    public static ZonedDateTime fromEpochMilli(long epochMilli) {
        return Instant.ofEpochMilli(epochMilli).atZone(UTC_TIMEZONE);
    }

    public static long toEpochMilli(ZonedDateTime dateTime) {
        Objects.requireNonNull(dateTime, "DateTime cannot be null");
        return dateTime.toInstant().toEpochMilli();
    }
}