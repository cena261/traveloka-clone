package com.cena.traveloka.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Test class for DateTimeUtils timezone and formatting operations.
 * Tests timezone conversions, formatting, parsing, and business date calculations.
 *
 * CRITICAL: These tests MUST FAIL initially (TDD requirement).
 * DateTimeUtils implementation does not exist yet.
 */
class DateTimeUtilsTest {

    private static final ZoneId VIETNAM_TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final ZoneId UTC_TIMEZONE = ZoneId.of("UTC");
    private static final ZoneId SINGAPORE_TIMEZONE = ZoneId.of("Asia/Singapore");

    @Test
    void shouldGetCurrentUtcDateTime() {
        // Given: Current time
        ZonedDateTime beforeCall = ZonedDateTime.now(UTC_TIMEZONE);

        // When: Getting current UTC datetime
        ZonedDateTime currentUtc = DateTimeUtils.getCurrentUtc();

        // Then: Should return current UTC time
        assertThat(currentUtc.getZone()).isEqualTo(UTC_TIMEZONE);
        assertThat(currentUtc).isAfterOrEqualTo(beforeCall);
        assertThat(currentUtc).isCloseTo(beforeCall, within(1, ChronoUnit.SECONDS));
    }

    @Test
    void shouldGetCurrentVietnamDateTime() {
        // Given: Current time
        ZonedDateTime beforeCall = ZonedDateTime.now(VIETNAM_TIMEZONE);

        // When: Getting current Vietnam datetime
        ZonedDateTime currentVietnam = DateTimeUtils.getCurrentVietnam();

        // Then: Should return current Vietnam time
        assertThat(currentVietnam.getZone()).isEqualTo(VIETNAM_TIMEZONE);
        assertThat(currentVietnam).isAfterOrEqualTo(beforeCall);
        assertThat(currentVietnam).isCloseTo(beforeCall, within(1, ChronoUnit.SECONDS));
    }

    @Test
    void shouldConvertBetweenTimezones() {
        // Given: UTC datetime
        ZonedDateTime utcDateTime = ZonedDateTime.of(2025, 9, 27, 10, 30, 0, 0, UTC_TIMEZONE);

        // When: Converting to Vietnam timezone
        ZonedDateTime vietnamDateTime = DateTimeUtils.convertToTimezone(utcDateTime, VIETNAM_TIMEZONE);

        // Then: Should convert correctly (Vietnam is UTC+7)
        assertThat(vietnamDateTime.getZone()).isEqualTo(VIETNAM_TIMEZONE);
        assertThat(vietnamDateTime.getHour()).isEqualTo(17); // 10 + 7 = 17
        assertThat(vietnamDateTime.getMinute()).isEqualTo(30);
        assertThat(vietnamDateTime.toInstant()).isEqualTo(utcDateTime.toInstant());
    }

    @Test
    void shouldConvertToUtc() {
        // Given: Vietnam datetime
        ZonedDateTime vietnamDateTime = ZonedDateTime.of(2025, 9, 27, 17, 30, 0, 0, VIETNAM_TIMEZONE);

        // When: Converting to UTC
        ZonedDateTime utcDateTime = DateTimeUtils.convertToUtc(vietnamDateTime);

        // Then: Should convert correctly (Vietnam is UTC+7)
        assertThat(utcDateTime.getZone()).isEqualTo(UTC_TIMEZONE);
        assertThat(utcDateTime.getHour()).isEqualTo(10); // 17 - 7 = 10
        assertThat(utcDateTime.getMinute()).isEqualTo(30);
        assertThat(utcDateTime.toInstant()).isEqualTo(vietnamDateTime.toInstant());
    }

    @ParameterizedTest
    @CsvSource({
        "'2025-09-27T10:30:00Z', 'yyyy-MM-dd HH:mm:ss', '2025-09-27 10:30:00'",
        "'2025-09-27T10:30:00Z', 'dd/MM/yyyy', '27/09/2025'",
        "'2025-09-27T10:30:00Z', 'MMM dd, yyyy', 'Sep 27, 2025'",
        "'2025-09-27T10:30:00Z', 'EEEE, MMMM dd, yyyy', 'Saturday, September 27, 2025'",
        "'2025-09-27T10:30:00Z', 'HH:mm', '10:30'"
    })
    void shouldFormatDateTimeWithCustomPatterns(String isoDateTime, String pattern, String expected) {
        // Given: ZonedDateTime and format pattern
        ZonedDateTime dateTime = ZonedDateTime.parse(isoDateTime);

        // When: Formatting with custom pattern
        String formatted = DateTimeUtils.format(dateTime, pattern);

        // Then: Should format correctly
        assertThat(formatted).isEqualTo(expected);
    }

    @Test
    void shouldFormatWithTimezone() {
        // Given: UTC datetime
        ZonedDateTime utcDateTime = ZonedDateTime.parse("2025-09-27T10:30:00Z");

        // When: Formatting with Vietnam timezone
        String vietnamFormatted = DateTimeUtils.formatInTimezone(utcDateTime, "yyyy-MM-dd HH:mm:ss", VIETNAM_TIMEZONE);

        // Then: Should format in target timezone
        assertThat(vietnamFormatted).isEqualTo("2025-09-27 17:30:00"); // UTC+7
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2025-09-27T10:30:00Z",
        "2025-09-27T10:30:00+07:00",
        "2025-09-27 10:30:00",
        "27/09/2025 10:30",
        "Sep 27, 2025 10:30 AM"
    })
    void shouldParseVariousDateTimeFormats(String dateTimeString) {
        // When: Parsing datetime string
        ZonedDateTime parsed = DateTimeUtils.parseFlexible(dateTimeString);

        // Then: Should successfully parse
        assertThat(parsed).isNotNull();
        assertThat(parsed.getYear()).isEqualTo(2025);
        assertThat(parsed.getMonthValue()).isEqualTo(9);
        assertThat(parsed.getDayOfMonth()).isEqualTo(27);
    }

    @Test
    void shouldParseWithSpecificFormat() {
        // Given: Date string and format
        String dateString = "27/09/2025 17:30";
        String format = "dd/MM/yyyy HH:mm";

        // When: Parsing with specific format
        ZonedDateTime parsed = DateTimeUtils.parseWithFormat(dateString, format, VIETNAM_TIMEZONE);

        // Then: Should parse correctly
        assertThat(parsed.getYear()).isEqualTo(2025);
        assertThat(parsed.getMonthValue()).isEqualTo(9);
        assertThat(parsed.getDayOfMonth()).isEqualTo(27);
        assertThat(parsed.getHour()).isEqualTo(17);
        assertThat(parsed.getMinute()).isEqualTo(30);
        assertThat(parsed.getZone()).isEqualTo(VIETNAM_TIMEZONE);
    }

    @Test
    void shouldCalculateDurationBetweenDates() {
        // Given: Two datetimes
        ZonedDateTime start = ZonedDateTime.parse("2025-09-27T10:00:00Z");
        ZonedDateTime end = ZonedDateTime.parse("2025-09-27T15:30:00Z");

        // When: Calculating duration
        Duration duration = DateTimeUtils.calculateDuration(start, end);

        // Then: Should calculate correct duration
        assertThat(duration.toHours()).isEqualTo(5);
        assertThat(duration.toMinutes()).isEqualTo(330); // 5.5 hours = 330 minutes
    }

    @Test
    void shouldCalculateDaysBetweenDates() {
        // Given: Two dates
        LocalDate startDate = LocalDate.of(2025, 9, 27);
        LocalDate endDate = LocalDate.of(2025, 10, 5);

        // When: Calculating days between
        long daysBetween = DateTimeUtils.calculateDaysBetween(startDate, endDate);

        // Then: Should calculate correct number of days
        assertThat(daysBetween).isEqualTo(8);
    }

    @Test
    void shouldCheckIfDateIsInFuture() {
        // Given: Future and past dates
        ZonedDateTime futureDate = ZonedDateTime.now().plusDays(1);
        ZonedDateTime pastDate = ZonedDateTime.now().minusDays(1);
        ZonedDateTime currentDate = ZonedDateTime.now();

        // When: Checking if dates are in future
        boolean futureIsInFuture = DateTimeUtils.isInFuture(futureDate);
        boolean pastIsInFuture = DateTimeUtils.isInFuture(pastDate);
        boolean currentIsInFuture = DateTimeUtils.isInFuture(currentDate);

        // Then: Should correctly identify future dates
        assertThat(futureIsInFuture).isTrue();
        assertThat(pastIsInFuture).isFalse();
        assertThat(currentIsInFuture).isFalse(); // current moment is not considered future
    }

    @Test
    void shouldCheckIfDateIsInPast() {
        // Given: Future and past dates
        ZonedDateTime futureDate = ZonedDateTime.now().plusDays(1);
        ZonedDateTime pastDate = ZonedDateTime.now().minusDays(1);

        // When: Checking if dates are in past
        boolean futureIsInPast = DateTimeUtils.isInPast(futureDate);
        boolean pastIsInPast = DateTimeUtils.isInPast(pastDate);

        // Then: Should correctly identify past dates
        assertThat(futureIsInPast).isFalse();
        assertThat(pastIsInPast).isTrue();
    }

    @Test
    void shouldCheckIfDateIsToday() {
        // Given: Today, yesterday, and tomorrow dates
        ZonedDateTime today = ZonedDateTime.now(VIETNAM_TIMEZONE);
        ZonedDateTime yesterday = today.minusDays(1);
        ZonedDateTime tomorrow = today.plusDays(1);

        // When: Checking if dates are today
        boolean todayIsToday = DateTimeUtils.isToday(today, VIETNAM_TIMEZONE);
        boolean yesterdayIsToday = DateTimeUtils.isToday(yesterday, VIETNAM_TIMEZONE);
        boolean tomorrowIsToday = DateTimeUtils.isToday(tomorrow, VIETNAM_TIMEZONE);

        // Then: Should correctly identify today
        assertThat(todayIsToday).isTrue();
        assertThat(yesterdayIsToday).isFalse();
        assertThat(tomorrowIsToday).isFalse();
    }

    @Test
    void shouldGetStartAndEndOfDay() {
        // Given: A datetime
        ZonedDateTime dateTime = ZonedDateTime.of(2025, 9, 27, 15, 30, 45, 123456789, VIETNAM_TIMEZONE);

        // When: Getting start and end of day
        ZonedDateTime startOfDay = DateTimeUtils.getStartOfDay(dateTime);
        ZonedDateTime endOfDay = DateTimeUtils.getEndOfDay(dateTime);

        // Then: Should return correct start and end times
        assertThat(startOfDay.getHour()).isEqualTo(0);
        assertThat(startOfDay.getMinute()).isEqualTo(0);
        assertThat(startOfDay.getSecond()).isEqualTo(0);
        assertThat(startOfDay.getNano()).isEqualTo(0);

        assertThat(endOfDay.getHour()).isEqualTo(23);
        assertThat(endOfDay.getMinute()).isEqualTo(59);
        assertThat(endOfDay.getSecond()).isEqualTo(59);
        assertThat(endOfDay.getNano()).isEqualTo(999999999);
    }

    @Test
    void shouldGetStartAndEndOfWeek() {
        // Given: A datetime (Saturday, Sep 27, 2025)
        ZonedDateTime saturday = ZonedDateTime.of(2025, 9, 27, 15, 30, 0, 0, VIETNAM_TIMEZONE);

        // When: Getting start and end of week (Monday start)
        ZonedDateTime startOfWeek = DateTimeUtils.getStartOfWeek(saturday);
        ZonedDateTime endOfWeek = DateTimeUtils.getEndOfWeek(saturday);

        // Then: Should return Monday start and Sunday end
        assertThat(startOfWeek.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(endOfWeek.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(startOfWeek.getHour()).isEqualTo(0);
        assertThat(endOfWeek.getHour()).isEqualTo(23);
    }

    @Test
    void shouldGetStartAndEndOfMonth() {
        // Given: A datetime in the middle of month
        ZonedDateTime midMonth = ZonedDateTime.of(2025, 9, 15, 15, 30, 0, 0, VIETNAM_TIMEZONE);

        // When: Getting start and end of month
        ZonedDateTime startOfMonth = DateTimeUtils.getStartOfMonth(midMonth);
        ZonedDateTime endOfMonth = DateTimeUtils.getEndOfMonth(midMonth);

        // Then: Should return first and last day of month
        assertThat(startOfMonth.getDayOfMonth()).isEqualTo(1);
        assertThat(endOfMonth.getDayOfMonth()).isEqualTo(30); // September has 30 days
        assertThat(startOfMonth.getHour()).isEqualTo(0);
        assertThat(endOfMonth.getHour()).isEqualTo(23);
    }

    @Test
    void shouldAddBusinessDays() {
        // Given: A Friday
        LocalDate friday = LocalDate.of(2025, 9, 26); // Friday

        // When: Adding business days
        LocalDate nextBusinessDay = DateTimeUtils.addBusinessDays(friday, 1);
        LocalDate threeBusninessDaysLater = DateTimeUtils.addBusinessDays(friday, 3);

        // Then: Should skip weekends
        assertThat(nextBusinessDay.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY); // Skip weekend
        assertThat(threeBusninessDaysLater.getDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
    }

    @Test
    void shouldCalculateBusinessDaysBetween() {
        // Given: Monday to Friday
        LocalDate monday = LocalDate.of(2025, 9, 22); // Monday
        LocalDate friday = LocalDate.of(2025, 9, 26); // Friday

        // When: Calculating business days between
        long businessDays = DateTimeUtils.calculateBusinessDaysBetween(monday, friday);

        // Then: Should exclude weekends
        assertThat(businessDays).isEqualTo(4); // Mon, Tue, Wed, Thu (Friday is exclusive)
    }

    @Test
    void shouldCheckIfBusinessDay() {
        // Given: Various days of week
        LocalDate monday = LocalDate.of(2025, 9, 22); // Monday
        LocalDate saturday = LocalDate.of(2025, 9, 27); // Saturday
        LocalDate sunday = LocalDate.of(2025, 9, 28); // Sunday

        // When: Checking if business days
        boolean mondayIsBusiness = DateTimeUtils.isBusinessDay(monday);
        boolean saturdayIsBusiness = DateTimeUtils.isBusinessDay(saturday);
        boolean sundayIsBusiness = DateTimeUtils.isBusinessDay(sunday);

        // Then: Should correctly identify business days
        assertThat(mondayIsBusiness).isTrue();
        assertThat(saturdayIsBusiness).isFalse();
        assertThat(sundayIsBusiness).isFalse();
    }

    @Test
    void shouldFormatDuration() {
        // Given: Various durations
        Duration shortDuration = Duration.ofMinutes(45);
        Duration hourDuration = Duration.ofHours(2).plusMinutes(30);
        Duration dayDuration = Duration.ofDays(1).plusHours(5);

        // When: Formatting durations
        String shortFormatted = DateTimeUtils.formatDuration(shortDuration);
        String hourFormatted = DateTimeUtils.formatDuration(hourDuration);
        String dayFormatted = DateTimeUtils.formatDuration(dayDuration);

        // Then: Should format human-readable durations
        assertThat(shortFormatted).isEqualTo("45 minutes");
        assertThat(hourFormatted).isEqualTo("2 hours 30 minutes");
        assertThat(dayFormatted).isEqualTo("1 day 5 hours");
    }

    @Test
    void shouldCalculateAge() {
        // Given: Birth date
        LocalDate birthDate = LocalDate.of(1995, 5, 15);
        LocalDate currentDate = LocalDate.of(2025, 9, 27);

        // When: Calculating age
        int age = DateTimeUtils.calculateAge(birthDate, currentDate);

        // Then: Should calculate correct age
        assertThat(age).isEqualTo(30);
    }

    @Test
    void shouldGetTimezoneOffset() {
        // Given: Datetime and timezone
        ZonedDateTime utcDateTime = ZonedDateTime.of(2025, 9, 27, 10, 30, 0, 0, UTC_TIMEZONE);

        // When: Getting timezone offset
        ZoneOffset vietnamOffset = DateTimeUtils.getTimezoneOffset(VIETNAM_TIMEZONE, utcDateTime.toInstant());

        // Then: Should return correct offset
        assertThat(vietnamOffset.getTotalSeconds()).isEqualTo(7 * 3600); // UTC+7
    }

    @Test
    void shouldGetAvailableTimezones() {
        // When: Getting available timezones
        List<String> timezones = DateTimeUtils.getAvailableTimezones();

        // Then: Should include common timezones
        assertThat(timezones).contains("UTC", "Asia/Ho_Chi_Minh", "Asia/Singapore", "America/New_York");
        assertThat(timezones).hasSizeGreaterThan(100); // Many timezones available
    }

    @Test
    void shouldFormatRelativeTime() {
        // Given: Reference time and various test times
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime fiveMinutesAgo = now.minusMinutes(5);
        ZonedDateTime oneHourAgo = now.minusHours(1);
        ZonedDateTime yesterday = now.minusDays(1);
        ZonedDateTime inFiveMinutes = now.plusMinutes(5);

        // When: Formatting relative times
        String fiveMinutesAgoFormatted = DateTimeUtils.formatRelativeTime(fiveMinutesAgo, now);
        String oneHourAgoFormatted = DateTimeUtils.formatRelativeTime(oneHourAgo, now);
        String yesterdayFormatted = DateTimeUtils.formatRelativeTime(yesterday, now);
        String inFiveMinutesFormatted = DateTimeUtils.formatRelativeTime(inFiveMinutes, now);

        // Then: Should format relative times
        assertThat(fiveMinutesAgoFormatted).isEqualTo("5 minutes ago");
        assertThat(oneHourAgoFormatted).isEqualTo("1 hour ago");
        assertThat(yesterdayFormatted).isEqualTo("1 day ago");
        assertThat(inFiveMinutesFormatted).isEqualTo("in 5 minutes");
    }

    @Test
    void shouldHandleNullInputsGracefully() {
        // When: Handling null inputs
        // Then: Should handle nulls without throwing exceptions
        assertThatThrownBy(() -> DateTimeUtils.convertToTimezone(null, VIETNAM_TIMEZONE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("DateTime cannot be null");

        assertThatThrownBy(() -> DateTimeUtils.format(null, "yyyy-MM-dd"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("DateTime cannot be null");

        assertThatThrownBy(() -> DateTimeUtils.parseWithFormat("2025-09-27", null, UTC_TIMEZONE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Format pattern cannot be null");
    }

    @Test
    void shouldValidateDateTimeRanges() {
        // Given: Valid and invalid date ranges
        ZonedDateTime start = ZonedDateTime.now();
        ZonedDateTime validEnd = start.plusHours(2);
        ZonedDateTime invalidEnd = start.minusHours(1);

        // When: Validating date ranges
        boolean validRangeIsValid = DateTimeUtils.isValidDateRange(start, validEnd);
        boolean invalidRangeIsValid = DateTimeUtils.isValidDateRange(start, invalidEnd);

        // Then: Should correctly validate date ranges
        assertThat(validRangeIsValid).isTrue();
        assertThat(invalidRangeIsValid).isFalse();
    }

    @Test
    void shouldConvertToEpochMilliseconds() {
        // Given: ZonedDateTime
        ZonedDateTime dateTime = ZonedDateTime.of(2025, 9, 27, 10, 30, 0, 0, UTC_TIMEZONE);

        // When: Converting to epoch milliseconds
        long epochMillis = DateTimeUtils.toEpochMillis(dateTime);

        // Then: Should convert correctly
        assertThat(epochMillis).isEqualTo(dateTime.toInstant().toEpochMilli());
    }

    @Test
    void shouldCreateFromEpochMilliseconds() {
        // Given: Epoch milliseconds
        long epochMillis = System.currentTimeMillis();

        // When: Creating datetime from epoch
        ZonedDateTime dateTime = DateTimeUtils.fromEpochMillis(epochMillis, VIETNAM_TIMEZONE);

        // Then: Should create correct datetime
        assertThat(dateTime.toInstant().toEpochMilli()).isEqualTo(epochMillis);
        assertThat(dateTime.getZone()).isEqualTo(VIETNAM_TIMEZONE);
    }
}