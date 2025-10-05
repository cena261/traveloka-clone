package com.cena.traveloka.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final String DEFAULT_SEPARATOR = "-";

    private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9-]+");

    private static final Pattern CONSECUTIVE_SEPARATOR_PATTERN = Pattern.compile("-{2,}");

    private static final Pattern EDGE_SEPARATOR_PATTERN = Pattern.compile("^-|-$");

    private SlugUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String generateSlug(String input) {
        return generateSlug(input, DEFAULT_SEPARATOR);
    }

    public static String generateSlug(String input, String separator) {
        Objects.requireNonNull(separator, "Separator cannot be null");
        if (separator.isEmpty()) {
            throw new IllegalArgumentException("Separator cannot be empty");
        }

        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        String slug = input.toLowerCase(Locale.ROOT);

        slug = removeDiacritics(slug);

        slug = slug.replaceAll("\\s+", separator);
        slug = slug.replaceAll("_+", separator);

        if (!separator.equals(DEFAULT_SEPARATOR)) {
            slug = slug.replaceAll("[^a-z0-9" + Pattern.quote(separator) + "]+", "");
        } else {
            slug = NON_ALPHANUMERIC_PATTERN.matcher(slug).replaceAll("");
        }

        slug = slug.replaceAll(Pattern.quote(separator) + "{2,}", separator);

        slug = slug.replaceAll("^" + Pattern.quote(separator) + "|" + Pattern.quote(separator) + "$", "");

        return slug;
    }

    public static String removeDiacritics(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;

        result = result.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        result = result.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        result = result.replaceAll("[ìíịỉĩ]", "i");
        result = result.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        result = result.replaceAll("[ùúụủũưừứựửữ]", "u");
        result = result.replaceAll("[ỳýỵỷỹ]", "y");
        result = result.replaceAll("đ", "d");

        result = result.replaceAll("[ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴ]", "A");
        result = result.replaceAll("[ÈÉẸẺẼÊỀẾỆỂỄ]", "E");
        result = result.replaceAll("[ÌÍỊỈĨ]", "I");
        result = result.replaceAll("[ÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠ]", "O");
        result = result.replaceAll("[ÙÚỤỦŨƯỪỨỰỬỮ]", "U");
        result = result.replaceAll("[ỲÝỴỶỸ]", "Y");
        result = result.replaceAll("Đ", "D");

        String normalized = Normalizer.normalize(result, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

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

    public static String truncateSlug(String slug, int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("Max length cannot be negative");
        }

        if (slug == null || slug.isEmpty() || slug.length() <= maxLength) {
            return slug;
        }

        String truncated = slug.substring(0, maxLength);

        int lastSeparator = truncated.lastIndexOf(DEFAULT_SEPARATOR);

        if (lastSeparator > 0) {
            truncated = truncated.substring(0, lastSeparator);
        }

        return EDGE_SEPARATOR_PATTERN.matcher(truncated).replaceAll("");
    }

    public static boolean isValidSlug(String slug) {
        if (slug == null || slug.isEmpty()) {
            return false;
        }

        return slug.matches("^[a-z0-9]+(-[a-z0-9]+)*$");
    }

    public static String normalizeSlug(String slug) {
        if (slug == null || slug.isEmpty()) {
            return "";
        }

        return generateSlug(slug);
    }

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

    public static String slugToTitle(String slug) {
        if (slug == null || slug.isEmpty()) {
            return "";
        }

        String title = slug.replace(DEFAULT_SEPARATOR, " ");

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