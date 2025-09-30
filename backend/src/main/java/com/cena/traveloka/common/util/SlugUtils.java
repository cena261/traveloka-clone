package com.cena.traveloka.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Utility class for generating URL-friendly slugs from strings.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Convert strings to URL-safe slugs</li>
 *   <li>Handle Vietnamese characters (diacritics removal)</li>
 *   <li>Normalize whitespace and special characters</li>
 *   <li>Support for custom separators</li>
 *   <li>Handle Unicode normalization</li>
 * </ul>
 *
 * <p>Slug format:</p>
 * <ul>
 *   <li>Lowercase only</li>
 *   <li>Alphanumeric characters and hyphens</li>
 *   <li>No leading/trailing hyphens</li>
 *   <li>No consecutive hyphens</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>
 * String slug = SlugUtils.generateSlug("Khách sạn Hà Nội");
 * // Returns: "khach-san-ha-noi"
 *
 * String slug = SlugUtils.generateSlug("Hello World!!!");
 * // Returns: "hello-world"
 * </pre>
 *
 * @since 1.0.0
 */
public final class SlugUtils {

    /**
     * Default separator for slug generation
     */
    private static final String DEFAULT_SEPARATOR = "-";

    /**
     * Pattern to match non-alphanumeric characters (excluding separator)
     */
    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9-]+");

    /**
     * Pattern to match consecutive separators
     */
    private static final Pattern CONSECUTIVE_SEPARATOR_PATTERN = Pattern.compile("-{2,}");

    /**
     * Pattern to match leading/trailing separators
     */
    private static final Pattern EDGE_SEPARATOR_PATTERN = Pattern.compile("^-|-$");

    /**
     * Private constructor to prevent instantiation
     */
    private SlugUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Generates a URL-friendly slug from the input string using default hyphen separator.
     *
     * <p>Process:</p>
     * <ol>
     *   <li>Convert to lowercase</li>
     *   <li>Remove Vietnamese diacritics</li>
     *   <li>Replace whitespace with hyphens</li>
     *   <li>Remove non-alphanumeric characters</li>
     *   <li>Remove consecutive hyphens</li>
     *   <li>Trim leading/trailing hyphens</li>
     * </ol>
     *
     * @param input The input string to convert
     * @return URL-friendly slug, or empty string if input is null/empty
     */
    public static String generateSlug(String input) {
        return generateSlug(input, DEFAULT_SEPARATOR);
    }

    /**
     * Generates a URL-friendly slug from the input string with custom separator.
     *
     * @param input The input string to convert
     * @param separator Custom separator character (e.g., "-", "_")
     * @return URL-friendly slug, or empty string if input is null/empty
     * @throws IllegalArgumentException if separator is null or empty
     */
    public static String generateSlug(String input, String separator) {
        Objects.requireNonNull(separator, "Separator cannot be null");
        if (separator.isEmpty()) {
            throw new IllegalArgumentException("Separator cannot be empty");
        }

        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        // Convert to lowercase
        String slug = input.toLowerCase(Locale.ROOT);

        // Remove Vietnamese diacritics
        slug = removeDiacritics(slug);

        // Replace whitespace and underscores with separator
        slug = slug.replaceAll("\\s+", separator);
        slug = slug.replaceAll("_+", separator);

        // Remove non-alphanumeric characters (except separator)
        if (!separator.equals(DEFAULT_SEPARATOR)) {
            slug = slug.replaceAll("[^a-z0-9" + Pattern.quote(separator) + "]+", "");
        } else {
            slug = NON_ALPHANUMERIC_PATTERN.matcher(slug).replaceAll("");
        }

        // Remove consecutive separators
        slug = slug.replaceAll(Pattern.quote(separator) + "{2,}", separator);

        // Remove leading/trailing separators
        slug = slug.replaceAll("^" + Pattern.quote(separator) + "|" + Pattern.quote(separator) + "$", "");

        return slug;
    }

    /**
     * Removes diacritical marks (accents) from characters.
     * Handles Vietnamese characters and other Unicode diacritics.
     *
     * @param input The input string
     * @return String with diacritics removed
     */
    public static String removeDiacritics(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Vietnamese character mappings
        String result = input;

        // Vietnamese lowercase vowels with diacritics
        result = result.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        result = result.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        result = result.replaceAll("[ìíịỉĩ]", "i");
        result = result.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        result = result.replaceAll("[ùúụủũưừứựửữ]", "u");
        result = result.replaceAll("[ỳýỵỷỹ]", "y");
        result = result.replaceAll("đ", "d");

        // Vietnamese uppercase vowels with diacritics
        result = result.replaceAll("[ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴ]", "A");
        result = result.replaceAll("[ÈÉẸẺẼÊỀẾỆỂỄ]", "E");
        result = result.replaceAll("[ÌÍỊỈĨ]", "I");
        result = result.replaceAll("[ÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠ]", "O");
        result = result.replaceAll("[ÙÚỤỦŨƯỪỨỰỬỮ]", "U");
        result = result.replaceAll("[ỲÝỴỶỸ]", "Y");
        result = result.replaceAll("Đ", "D");

        // Handle other Unicode diacritics using NFD normalization
        String normalized = Normalizer.normalize(result, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    /**
     * Generates a unique slug by appending a counter if the base slug already exists.
     * Useful when checking against existing slugs in a database.
     *
     * @param baseSlug The base slug to make unique
     * @param existingSlugs Array of existing slugs to check against
     * @return Unique slug with counter appended if necessary
     * @throws IllegalArgumentException if baseSlug is null or empty
     */
    public static String generateUniqueSlug(String baseSlug, String... existingSlugs) {
        Objects.requireNonNull(baseSlug, "Base slug cannot be null");
        if (baseSlug.isEmpty()) {
            throw new IllegalArgumentException("Base slug cannot be empty");
        }

        if (existingSlugs == null || existingSlugs.length == 0) {
            return baseSlug;
        }

        String uniqueSlug = baseSlug;
        int counter = 1;

        while (containsSlug(uniqueSlug, existingSlugs)) {
            uniqueSlug = baseSlug + DEFAULT_SEPARATOR + counter;
            counter++;
        }

        return uniqueSlug;
    }

    /**
     * Checks if a slug exists in an array of existing slugs.
     *
     * @param slug The slug to check
     * @param existingSlugs Array of existing slugs
     * @return true if slug exists in the array
     */
    private static boolean containsSlug(String slug, String... existingSlugs) {
        if (existingSlugs == null) {
            return false;
        }
        for (String existing : existingSlugs) {
            if (slug.equals(existing)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Truncates a slug to a maximum length while preserving word boundaries.
     *
     * @param slug The slug to truncate
     * @param maxLength Maximum length for the slug
     * @return Truncated slug
     * @throws IllegalArgumentException if maxLength is negative
     */
    public static String truncateSlug(String slug, int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("Max length cannot be negative");
        }

        if (slug == null || slug.isEmpty() || slug.length() <= maxLength) {
            return slug;
        }

        // Truncate to maxLength
        String truncated = slug.substring(0, maxLength);

        // Find the last separator to avoid cutting in the middle of a word
        int lastSeparator = truncated.lastIndexOf(DEFAULT_SEPARATOR);

        if (lastSeparator > 0) {
            truncated = truncated.substring(0, lastSeparator);
        }

        // Remove trailing separator if present
        return EDGE_SEPARATOR_PATTERN.matcher(truncated).replaceAll("");
    }

    /**
     * Validates if a string is a valid slug format.
     *
     * @param slug The string to validate
     * @return true if the string is a valid slug
     */
    public static boolean isValidSlug(String slug) {
        if (slug == null || slug.isEmpty()) {
            return false;
        }

        // Check if slug matches the pattern: lowercase alphanumeric with hyphens
        // No leading/trailing hyphens, no consecutive hyphens
        return slug.matches("^[a-z0-9]+(-[a-z0-9]+)*$");
    }

    /**
     * Normalizes a slug to ensure it follows slug conventions.
     * This is useful for cleaning up existing slugs.
     *
     * @param slug The slug to normalize
     * @return Normalized slug
     */
    public static String normalizeSlug(String slug) {
        if (slug == null || slug.isEmpty()) {
            return "";
        }

        // Apply slug generation to normalize
        return generateSlug(slug);
    }

    /**
     * Generates a slug from multiple parts, joining them with separators.
     *
     * @param parts Variable number of string parts to join
     * @return Combined slug from all parts
     */
    public static String generateSlugFromParts(String... parts) {
        if (parts == null || parts.length == 0) {
            return "";
        }

        StringBuilder combined = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                if (combined.length() > 0) {
                    combined.append(" ");
                }
                combined.append(part.trim());
            }
        }

        return generateSlug(combined.toString());
    }

    /**
     * Extracts title text from a slug by replacing separators with spaces
     * and capitalizing words.
     *
     * @param slug The slug to convert to title
     * @return Title-cased string derived from slug
     */
    public static String slugToTitle(String slug) {
        if (slug == null || slug.isEmpty()) {
            return "";
        }

        // Replace separators with spaces
        String title = slug.replace(DEFAULT_SEPARATOR, " ");

        // Capitalize first letter of each word
        String[] words = title.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
            }
        }

        return result.toString();
    }
}