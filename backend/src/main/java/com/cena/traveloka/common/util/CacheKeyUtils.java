package com.cena.traveloka.common.util;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Arrays;

public final class CacheKeyUtils {

    private static final String SEPARATOR = ":";

    private static final String WILDCARD = "*";

    private static final int MAX_KEY_LENGTH = 512;

    private CacheKeyUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String generateKey(String module, String entity, String id) {
        validateKeyPart(module, "Module");
        validateKeyPart(entity, "Entity");
        validateKeyPart(id, "ID");

        String key = String.join(SEPARATOR, module, entity, id);
        validateKeyLength(key);

        return key;
    }

    public static String generateKey(String module, String entity, UUID id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return generateKey(module, entity, id.toString());
    }

    public static String generateKey(String module, String entity, Long id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return generateKey(module, entity, id.toString());
    }

    public static String generateKey(String module, String entity, Integer id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return generateKey(module, entity, id.toString());
    }

    public static String generatePattern(String module, String entity) {
        validateKeyPart(module, "Module");
        validateKeyPart(entity, "Entity");

        return String.join(SEPARATOR, module, entity, WILDCARD);
    }

    public static String generateModulePattern(String module) {
        validateKeyPart(module, "Module");
        return module + SEPARATOR + WILDCARD;
    }

    public static String generateCompositeKey(String... parts) {
        if (parts == null || parts.length == 0) {
            throw new IllegalArgumentException("Key parts cannot be null or empty");
        }

        for (int i = 0; i < parts.length; i++) {
            validateKeyPart(parts[i], "Key part at index " + i);
        }

        String key = String.join(SEPARATOR, parts);
        validateKeyLength(key);

        return key;
    }

    public static String generateListKey(String module, String entity, String qualifier) {
        validateKeyPart(module, "Module");
        validateKeyPart(entity, "Entity");

        if (qualifier == null || qualifier.trim().isEmpty()) {
            return String.join(SEPARATOR, module, entity, "list");
        }

        return String.join(SEPARATOR, module, entity, "list", qualifier);
    }

    public static String generatePageKey(String module, String entity, int pageNumber, int pageSize) {
        validateKeyPart(module, "Module");
        validateKeyPart(entity, "Entity");

        if (pageNumber < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }

        return String.join(SEPARATOR, module, entity, "page", String.valueOf(pageNumber), String.valueOf(pageSize));
    }

    public static String generateSearchKey(String module, String entity, String searchQuery) {
        validateKeyPart(module, "Module");
        validateKeyPart(entity, "Entity");
        validateKeyPart(searchQuery, "Search query");

        int queryHash = Math.abs(searchQuery.hashCode());
        return String.join(SEPARATOR, module, entity, "search", String.valueOf(queryHash));
    }

    public static String[] parseKey(String cacheKey) {
        validateKeyPart(cacheKey, "Cache key");
        return cacheKey.split(SEPARATOR);
    }

    public static String extractModule(String cacheKey) {
        String[] parts = parseKey(cacheKey);
        if (parts.length < 1) {
            throw new IllegalArgumentException("Invalid cache key format");
        }
        return parts[0];
    }

    public static String extractEntity(String cacheKey) {
        String[] parts = parseKey(cacheKey);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid cache key format");
        }
        return parts[1];
    }

    public static String extractId(String cacheKey) {
        String[] parts = parseKey(cacheKey);
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid cache key format");
        }
        return parts[2];
    }

    public static boolean isValidKey(String cacheKey) {
        if (cacheKey == null || cacheKey.trim().isEmpty()) {
            return false;
        }

        String[] parts = cacheKey.split(SEPARATOR);

        if (parts.length < 3) {
            return false;
        }

        for (String part : parts) {
            if (part.trim().isEmpty()) {
                return false;
            }
        }

        return cacheKey.length() <= MAX_KEY_LENGTH;
    }

    private static void validateKeyPart(String part, String partName) {
        Objects.requireNonNull(part, partName + " cannot be null");
        if (part.trim().isEmpty()) {
            throw new IllegalArgumentException(partName + " cannot be empty");
        }
    }

    private static void validateKeyLength(String key) {
        if (key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Cache key exceeds maximum length of %d characters: %s", MAX_KEY_LENGTH, key)
            );
        }
    }

    public static String getSeparator() {
        return SEPARATOR;
    }

    public static String getWildcard() {
        return WILDCARD;
    }

    public static int getMaxKeyLength() {
        return MAX_KEY_LENGTH;
    }
}