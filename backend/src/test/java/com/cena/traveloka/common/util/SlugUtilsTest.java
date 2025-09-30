package com.cena.traveloka.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.List;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test class for SlugUtils URL-safe string generation.
 * Tests slug generation, validation, and various text transformations for URLs.
 *
 * CRITICAL: These tests MUST FAIL initially (TDD requirement).
 * SlugUtils implementation does not exist yet.
 */
class SlugUtilsTest {

    @ParameterizedTest
    @CsvSource({
        "'Hello World', 'hello-world'",
        "'Ho Chi Minh City', 'ho-chi-minh-city'",
        "'Best Hotels in Vietnam', 'best-hotels-in-vietnam'",
        "'Top 10 Restaurants', 'top-10-restaurants'",
        "'Travel & Tourism', 'travel-tourism'",
        "'5-Star Hotel', '5-star-hotel'",
        "'Hotel/Resort Booking', 'hotel-resort-booking'"
    })
    void shouldGenerateBasicSlugs(String input, String expected) {
        // When: Generating slug from input
        String slug = SlugUtils.generateSlug(input);

        // Then: Should create URL-safe slug
        assertThat(slug).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "'Café in Hà Nội', 'cafe-in-ha-noi'", // Vietnamese accents
        "'Hôtel à Paris', 'hotel-a-paris'", // French accents
        "'Straße in München', 'strasse-in-munchen'", // German umlauts
        "'北京酒店', 'bei-jing-jiu-dian'", // Chinese characters (if supported)
        "'मुंबई होटल', 'mumbai-hotel'", // Hindi (if supported)
        "'Москва отель', 'moskva-otel'" // Cyrillic (if supported)
    })
    void shouldHandleUnicodeCharacters(String input, String expected) {
        // When: Generating slug with unicode characters
        String slug = SlugUtils.generateSlug(input);

        // Then: Should transliterate and create clean slug
        assertThat(slug).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "'Hello    World', 'hello-world'", // multiple spaces
        "'  Leading and trailing  ', 'leading-and-trailing'", // trim spaces
        "'Multiple---Dashes', 'multiple-dashes'", // multiple dashes
        "'Mixed___Underscores', 'mixed-underscores'", // underscores to dashes
        "'Dots.In.Title', 'dots-in-title'", // dots to dashes
        "'Special!@#$%Characters', 'special-characters'" // special characters removed
    })
    void shouldNormalizeWhitespaceAndSpecialCharacters(String input, String expected) {
        // When: Generating slug with various formatting issues
        String slug = SlugUtils.generateSlug(input);

        // Then: Should normalize whitespace and special characters
        assertThat(slug).isEqualTo(expected);
    }

    @Test
    void shouldHandleMaxLength() {
        // Given: Long input text
        String longInput = "This is a very long title that should be truncated to fit within the maximum allowed length for URL slugs";

        // When: Generating slug with max length
        String slug = SlugUtils.generateSlug(longInput, 50);

        // Then: Should truncate at word boundary
        assertThat(slug).hasSize(50);
        assertThat(slug).doesNotEndWith("-"); // Should not end with dash
        assertThat(slug).startsWith("this-is-a-very-long-title-that-should-be");
    }

    @Test
    void shouldEnsureUniqueness() {
        // Given: Base slug and existing slugs
        String baseSlug = "hotel-review";
        List<String> existingSlugs = Arrays.asList(
            "hotel-review",
            "hotel-review-1",
            "hotel-review-2"
        );

        // When: Generating unique slug
        String uniqueSlug = SlugUtils.generateUniqueSlug(baseSlug, existingSlugs);

        // Then: Should create unique slug with suffix
        assertThat(uniqueSlug).isEqualTo("hotel-review-3");
        assertThat(existingSlugs).doesNotContain(uniqueSlug);
    }

    @Test
    void shouldGenerateSlugFromMultipleWords() {
        // Given: Multiple words
        String[] words = {"best", "luxury", "hotels", "vietnam"};

        // When: Generating slug from words
        String slug = SlugUtils.generateSlugFromWords(words);

        // Then: Should join words with dashes
        assertThat(slug).isEqualTo("best-luxury-hotels-vietnam");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "hello-world",
        "hotel-123",
        "travel-and-tourism",
        "5-star-resort",
        "ho-chi-minh-city"
    })
    void shouldValidateValidSlugs(String slug) {
        // When: Validating valid slugs
        boolean isValid = SlugUtils.isValidSlug(slug);

        // Then: Should return true for valid slugs
        assertThat(isValid).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Hello World", // contains spaces
        "hotel_review", // contains underscores
        "hotel.review", // contains dots
        "UPPERCASE", // contains uppercase
        "hotel--review", // double dashes
        "-leading-dash", // starts with dash
        "trailing-dash-", // ends with dash
        "special!characters", // special characters
        ""
    })
    @NullAndEmptySource
    void shouldRejectInvalidSlugs(String slug) {
        // When: Validating invalid slugs
        boolean isValid = SlugUtils.isValidSlug(slug);

        // Then: Should return false for invalid slugs
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldGenerateSlugFromUrl() {
        // Given: URL with title
        String url = "https://example.com/article/best-hotels-in-hanoi";

        // When: Extracting slug from URL
        String slug = SlugUtils.extractSlugFromUrl(url);

        // Then: Should extract the slug part
        assertThat(slug).isEqualTo("best-hotels-in-hanoi");
    }

    @Test
    void shouldConvertCamelCaseToSlug() {
        // Given: CamelCase strings
        String camelCase1 = "bestHotelsInVietnam";
        String camelCase2 = "HotelBookingSystem";
        String camelCase3 = "XMLHttpRequest";

        // When: Converting to slugs
        String slug1 = SlugUtils.camelCaseToSlug(camelCase1);
        String slug2 = SlugUtils.camelCaseToSlug(camelCase2);
        String slug3 = SlugUtils.camelCaseToSlug(camelCase3);

        // Then: Should convert correctly
        assertThat(slug1).isEqualTo("best-hotels-in-vietnam");
        assertThat(slug2).isEqualTo("hotel-booking-system");
        assertThat(slug3).isEqualTo("xml-http-request");
    }

    @Test
    void shouldConvertSnakeCaseToSlug() {
        // Given: snake_case strings
        String snakeCase1 = "best_hotels_in_vietnam";
        String snakeCase2 = "hotel_booking_system";
        String snakeCase3 = "user_profile_settings";

        // When: Converting to slugs
        String slug1 = SlugUtils.snakeCaseToSlug(snakeCase1);
        String slug2 = SlugUtils.snakeCaseToSlug(snakeCase2);
        String slug3 = SlugUtils.snakeCaseToSlug(snakeCase3);

        // Then: Should convert underscores to dashes
        assertThat(slug1).isEqualTo("best-hotels-in-vietnam");
        assertThat(slug2).isEqualTo("hotel-booking-system");
        assertThat(slug3).isEqualTo("user-profile-settings");
    }

    @Test
    void shouldGenerateSlugWithCustomSeparator() {
        // Given: Input text and custom separator
        String input = "Best Hotels in Vietnam";
        String separator = "_";

        // When: Generating slug with custom separator
        String slug = SlugUtils.generateSlugWithSeparator(input, separator);

        // Then: Should use custom separator
        assertThat(slug).isEqualTo("best_hotels_in_vietnam");
    }

    @Test
    void shouldPreserveNumbers() {
        // Given: Text with numbers
        String input = "Top 10 Hotels in 2025";

        // When: Generating slug
        String slug = SlugUtils.generateSlug(input);

        // Then: Should preserve numbers
        assertThat(slug).isEqualTo("top-10-hotels-in-2025");
    }

    @Test
    void shouldHandleConsecutiveSpecialCharacters() {
        // Given: Text with consecutive special characters
        String input = "Hotel & Resort!!! Amazing*** Experience???";

        // When: Generating slug
        String slug = SlugUtils.generateSlug(input);

        // Then: Should clean up consecutive special characters
        assertThat(slug).isEqualTo("hotel-resort-amazing-experience");
    }

    @Test
    void shouldGenerateSlugForSeoFriendlyUrls() {
        // Given: Title for SEO-friendly URL
        String title = "Ultimate Guide to Booking Hotels in Ho Chi Minh City";
        String category = "travel-guides";

        // When: Generating SEO-friendly slug
        String seoSlug = SlugUtils.generateSeoSlug(title, category);

        // Then: Should create SEO-optimized slug
        assertThat(seoSlug).isEqualTo("travel-guides/ultimate-guide-to-booking-hotels-in-ho-chi-minh-city");
    }

    @Test
    void shouldAddTimestampToSlug() {
        // Given: Base slug
        String baseSlug = "hotel-review";

        // When: Adding timestamp
        String timestampedSlug = SlugUtils.addTimestampToSlug(baseSlug);

        // Then: Should append timestamp
        assertThat(timestampedSlug).startsWith("hotel-review-");
        assertThat(timestampedSlug).matches("hotel-review-\\d{10}"); // Unix timestamp
    }

    @Test
    void shouldGenerateSlugWithRandomSuffix() {
        // Given: Base slug
        String baseSlug = "hotel-booking";

        // When: Adding random suffix
        String randomSlug = SlugUtils.addRandomSuffix(baseSlug, 6);

        // Then: Should append random string
        assertThat(randomSlug).startsWith("hotel-booking-");
        assertThat(randomSlug).hasSize(baseSlug.length() + 1 + 6); // base + dash + suffix
        assertThat(randomSlug).matches("hotel-booking-[a-z0-9]{6}");
    }

    @Test
    void shouldRemoveStopWords() {
        // Given: Text with stop words
        String input = "The Best Hotels and Resorts in the City";

        // When: Generating slug without stop words
        String slug = SlugUtils.generateSlugWithoutStopWords(input);

        // Then: Should remove common stop words
        assertThat(slug).isEqualTo("best-hotels-resorts-city");
    }

    @Test
    void shouldHandleEmptyAndNullInput() {
        // When: Handling null and empty inputs
        String nullSlug = SlugUtils.generateSlug(null);
        String emptySlug = SlugUtils.generateSlug("");
        String whitespaceSlug = SlugUtils.generateSlug("   ");

        // Then: Should handle gracefully
        assertThat(nullSlug).isEqualTo("");
        assertThat(emptySlug).isEqualTo("");
        assertThat(whitespaceSlug).isEqualTo("");
    }

    @Test
    void shouldReverseSlugToTitle() {
        // Given: Slug
        String slug = "best-hotels-in-vietnam";

        // When: Converting slug back to title
        String title = SlugUtils.slugToTitle(slug);

        // Then: Should create readable title
        assertThat(title).isEqualTo("Best Hotels In Vietnam");
    }

    @Test
    void shouldGenerateSlugForMultipleLanguages() {
        // Given: Multilingual text
        String englishTitle = "Best Hotels in Vietnam";
        String vietnameseTitle = "Khách sạn tốt nhất ở Việt Nam";

        // When: Generating slugs
        String englishSlug = SlugUtils.generateSlug(englishTitle);
        String vietnameseSlug = SlugUtils.generateSlug(vietnameseTitle);

        // Then: Should handle both languages
        assertThat(englishSlug).isEqualTo("best-hotels-in-vietnam");
        assertThat(vietnameseSlug).isEqualTo("khach-san-tot-nhat-o-viet-nam");
    }

    @Test
    void shouldValidateSlugLength() {
        // Given: Slugs of various lengths
        String shortSlug = "abc";
        String normalSlug = "best-hotels-in-vietnam";
        String longSlug = "a".repeat(200);

        // When: Validating slug lengths
        boolean shortIsValid = SlugUtils.isValidSlugLength(shortSlug, 5, 100);
        boolean normalIsValid = SlugUtils.isValidSlugLength(normalSlug, 5, 100);
        boolean longIsValid = SlugUtils.isValidSlugLength(longSlug, 5, 100);

        // Then: Should validate length constraints
        assertThat(shortIsValid).isFalse(); // too short
        assertThat(normalIsValid).isTrue(); // within range
        assertThat(longIsValid).isFalse(); // too long
    }

    @Test
    void shouldGenerateHierarchicalSlugs() {
        // Given: Hierarchical components
        String[] hierarchy = {"travel", "destinations", "asia", "vietnam", "ho-chi-minh-city"};

        // When: Generating hierarchical slug
        String hierarchicalSlug = SlugUtils.generateHierarchicalSlug(hierarchy);

        // Then: Should create path-like slug
        assertThat(hierarchicalSlug).isEqualTo("travel/destinations/asia/vietnam/ho-chi-minh-city");
    }

    @Test
    void shouldSanitizeForFilenames() {
        // Given: Text that needs to be filename-safe
        String input = "Hotel Report 2025: Q3/Q4 Analysis (Final).pdf";

        // When: Sanitizing for filename
        String filename = SlugUtils.sanitizeForFilename(input);

        // Then: Should create safe filename
        assertThat(filename).isEqualTo("hotel-report-2025-q3-q4-analysis-final-pdf");
    }

    @Test
    void shouldThrowExceptionForInvalidParameters() {
        // When/Then: Should throw exception for invalid parameters
        assertThatThrownBy(() -> SlugUtils.generateSlug("test", -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Max length cannot be negative");

        assertThatThrownBy(() -> SlugUtils.generateSlugWithSeparator("test", ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Separator cannot be empty");

        assertThatThrownBy(() -> SlugUtils.addRandomSuffix("test", 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Suffix length must be positive");
    }

    @Test
    void shouldOptimizeSlugForSearch() {
        // Given: Title with keywords
        String title = "5-Star Luxury Hotels with Swimming Pool and Spa";
        String[] keywords = {"luxury", "hotels", "spa"};

        // When: Optimizing slug for search
        String optimizedSlug = SlugUtils.optimizeSlugForSearch(title, keywords);

        // Then: Should prioritize keywords
        assertThat(optimizedSlug).isEqualTo("luxury-hotels-spa-5-star-swimming-pool");
    }

    @Test
    void shouldGenerateSlugVariations() {
        // Given: Base title
        String title = "Best Hotels in Vietnam";

        // When: Generating variations
        List<String> variations = SlugUtils.generateSlugVariations(title, 3);

        // Then: Should create multiple variations
        assertThat(variations).hasSize(3);
        assertThat(variations).contains("best-hotels-in-vietnam");
        assertThat(variations).allMatch(SlugUtils::isValidSlug);
    }

    @Test
    void shouldCheckSlugAvailability() {
        // Given: Existing slugs in database
        List<String> existingSlugs = Arrays.asList(
            "best-hotels",
            "luxury-resorts",
            "travel-guide"
        );

        // When: Checking availability
        boolean existingIsAvailable = SlugUtils.isSlugAvailable("best-hotels", existingSlugs);
        boolean newIsAvailable = SlugUtils.isSlugAvailable("new-hotel-review", existingSlugs);

        // Then: Should correctly check availability
        assertThat(existingIsAvailable).isFalse();
        assertThat(newIsAvailable).isTrue();
    }
}