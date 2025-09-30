package com.cena.traveloka.common.util;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Utility class for generating consistent cache keys following the pattern: module:entity:id.
 *
 * <p>Cache key pattern:</p>
 * <ul>
 *   <li>Format: "module:entity:id" (e.g., "user:profile:12345")</li>
 *   <li>Module: Business module name (user, booking, hotel, etc.)</li>
 *   <li>Entity: Entity type within the module (profile, session, details, etc.)</li>
 *   <li>ID: Unique identifier (UUID, Long, String, etc.)</li>
 * </ul>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Consistent cache key generation across modules</li>
 *   <li>Support for various ID types (UUID, Long, String)</li>
 *   <li>Pattern-based keys for wildcards and searches</li>
 *   <li>Validation to ensure key consistency</li>
 * </ul>
 *
 * <p>Usage examples:</p>
 * <pre>
 * // Basic key generation
 * String key = CacheKeyUtils.generateKey("user", "profile", "12345");
 * // Returns: "user:profile:12345"
 *
 * // With UUID
 * UUID userId = UUID.randomUUID();
 * String key = CacheKeyUtils.generateKey("user", "session", userId);
 * // Returns: "user:session:550e8400-e29b-41d4-a716-446655440000"
 *
 * // Pattern for wildcards
 * String pattern = CacheKeyUtils.generatePattern("user", "profile");
 * // Returns: "user:profile:*"
 * </pre>
 *
 * @since 1.0.0
 */
public final class CacheKeyUtils {

    /**
     * Cache key separator character
     */
    private static final String SEPARATOR = ":";

    /**
     * Wildcard character for pattern matching
     */
    private static final String WILDCARD = "*";

    /**
     * Maximum length for cache keys (recommended Redis limit)
     */
    private static final int MAX_KEY_LENGTH = 512;

    /**
     * Private constructor to prevent instantiation
     */
    private CacheKeyUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Generates a cache key following the pattern: module:entity:id.
     *
     * @param module Business module name (e.g., "user", "booking", "hotel")
     * @param entity Entity type within the module (e.g., "profile", "details")
     * @param id Unique identifier
     * @return Generated cache key
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public static String generateKey(String module, String entity, String id) {
        validateKeyPart(module, "Module");
        validateKeyPart(entity, "Entity");
        validateKeyPart(id, "ID");

        String key = String.join(SEPARATOR, module, entity, id);
        validateKeyLength(key);

        return key;
    }

    /**
     * Generates a cache key with UUID identifier.
     *
     * @param module Business module name
     * @param entity Entity type within the module
     * @param id UUID identifier
     * @return Generated cache key
     * @throws IllegalArgumentException if any parameter is null or if module/entity is empty
     */
    public static String generateKey(String module, String entity, UUID id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return generateKey(module, entity, id.toString());
    }

    /**
     * Generates a cache key with Long identifier.
     *
     * @param module Business module name
     * @param entity Entity type within the module
     * @param id Long identifier
     * @return Generated cache key
     * @throws IllegalArgumentException if any parameter is null or if module/entity is empty
     */
    public static String generateKey(String module, String entity, Long id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return generateKey(module, entity, id.toString());
    }

    /**
     * Generates a cache key with Integer identifier.
     *
     * @param module Business module name
     * @param entity Entity type within the module
     * @param id Integer identifier
     * @return Generated cache key
     * @throws IllegalArgumentException if any parameter is null or if module/entity is empty
     */
    public static String generateKey(String module, String entity, Integer id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return generateKey(module, entity, id.toString());
    }

    /**
     * Generates a cache key pattern for wildcard searches.
     * Pattern format: "module:entity:*"
     *
     * @param module Business module name
     * @param entity Entity type within the module
     * @return Cache key pattern with wildcard
     * @throws IllegalArgumentException if module or entity is null or empty
     */
    public static String generatePattern(String module, String entity) {
        validateKeyPart(module, "Module");
        validateKeyPart(entity, "Entity");

        return String.join(SEPARATOR, module, entity, WILDCARD);
    }

    /**
     * Generates a cache key pattern for module-level wildcard searches.
     * Pattern format: "module:*"
     *
     * @param module Business module name
     * @return Cache key pattern with wildcard
     * @throws IllegalArgumentException if module is null or empty
     */
    public static String generateModulePattern(String module) {
        validateKeyPart(module, "Module");
        return module + SEPARATOR + WILDCARD;
    }

    /**
     * Generates a composite cache key with multiple parts.
     * Useful for hierarchical or complex cache structures.
     *
     * @param parts Variable number of key parts to join
     * @return Generated cache key
     * @throws IllegalArgumentException if parts array is null, empty, or contains null/empty elements
     */
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

    /**
     * Generates a cache key for a list or collection.
     * Format: "module:entity:list" or "module:entity:list:qualifier"
     *
     * @param module Business module name
     * @param entity Entity type within the module
     * @param qualifier Optional qualifier for the list (can be null)
     * @return Cache key for list
     * @throws IllegalArgumentException if module or entity is null or empty
     */
    public static String generateListKey(String module, String entity, String qualifier) {
        validateKeyPart(module, "Module");
        validateKeyPart(entity, "Entity");

        if (qualifier == null || qualifier.trim().isEmpty()) {
            return String.join(SEPARATOR, module, entity, "list");
        }

        return String.join(SEPARATOR, module, entity, "list", qualifier);
    }

    /**
     * Generates a cache key for pagination.
     * Format: "module:entity:page:pageNumber:pageSize"
     *
     * @param module Business module name
     * @param entity Entity type within the module
     * @param pageNumber Page number (0-based)
     * @param pageSize Number of items per page
     * @return Cache key for paginated data
     * @throws IllegalArgumentException if module or entity is null/empty, or if page parameters are invalid
     */
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

    /**
     * Generates a cache key for search results.
     * Format: "module:entity:search:queryHash"
     *
     * @param module Business module name
     * @param entity Entity type within the module
     * @param searchQuery Search query string
     * @return Cache key for search results
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public static String generateSearchKey(String module, String entity, String searchQuery) {
        validateKeyPart(module, "Module");
        validateKeyPart(entity, "Entity");
        validateKeyPart(searchQuery, "Search query");

        // Generate a hash of the search query to keep the key length manageable
        int queryHash = Math.abs(searchQuery.hashCode());
        return String.join(SEPARATOR, module, entity, "search", String.valueOf(queryHash));
    }

    /**
     * Parses a cache key into its component parts.
     *
     * @param cacheKey The cache key to parse
     * @return Array of key parts [module, entity, id, ...]
     * @throws IllegalArgumentException if cacheKey is null or empty
     */
    public static String[] parseKey(String cacheKey) {
        validateKeyPart(cacheKey, "Cache key");
        return cacheKey.split(SEPARATOR);
    }

    /**
     * Extracts the module name from a cache key.
     *
     * @param cacheKey The cache key to parse
     * @return Module name
     * @throws IllegalArgumentException if cacheKey is null or empty or has invalid format
     */
    public static String extractModule(String cacheKey) {
        String[] parts = parseKey(cacheKey);
        if (parts.length < 1) {
            throw new IllegalArgumentException("Invalid cache key format");
        }
        return parts[0];
    }

    /**
     * Extracts the entity type from a cache key.
     *
     * @param cacheKey The cache key to parse
     * @return Entity type
     * @throws IllegalArgumentException if cacheKey is null or empty or has invalid format
     */
    public static String extractEntity(String cacheKey) {
        String[] parts = parseKey(cacheKey);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid cache key format");
        }
        return parts[1];
    }

    /**
     * Extracts the ID from a cache key.
     *
     * @param cacheKey The cache key to parse
     * @return ID string
     * @throws IllegalArgumentException if cacheKey is null or empty or has invalid format
     */
    public static String extractId(String cacheKey) {
        String[] parts = parseKey(cacheKey);
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid cache key format");
        }
        return parts[2];
    }

    /**
     * Validates that a cache key matches the expected pattern.
     *
     * @param cacheKey The cache key to validate
     * @return true if the key is valid
     */
    public static boolean isValidKey(String cacheKey) {
        if (cacheKey == null || cacheKey.trim().isEmpty()) {
            return false;
        }

        String[] parts = cacheKey.split(SEPARATOR);

        // Minimum valid key has at least 3 parts: module:entity:id
        if (parts.length < 3) {
            return false;
        }

        // Check that no part is empty
        for (String part : parts) {
            if (part.trim().isEmpty()) {
                return false;
            }
        }

        // Check key length
        return cacheKey.length() <= MAX_KEY_LENGTH;
    }

    /**
     * Validates a key part is not null or empty.
     *
     * @param part The key part to validate
     * @param partName Name of the part for error message
     * @throws IllegalArgumentException if part is null or empty
     */
    private static void validateKeyPart(String part, String partName) {
        Objects.requireNonNull(part, partName + " cannot be null");
        if (part.trim().isEmpty()) {
            throw new IllegalArgumentException(partName + " cannot be empty");
        }
    }

    /**
     * Validates that a cache key doesn't exceed maximum length.
     *
     * @param key The key to validate
     * @throws IllegalArgumentException if key exceeds maximum length
     */
    private static void validateKeyLength(String key) {
        if (key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Cache key exceeds maximum length of %d characters: %s", MAX_KEY_LENGTH, key)
            );
        }
    }

    /**
     * Gets the separator used for cache keys.
     *
     * @return Cache key separator
     */
    public static String getSeparator() {
        return SEPARATOR;
    }

    /**
     * Gets the wildcard character used for pattern matching.
     *
     * @return Wildcard character
     */
    public static String getWildcard() {
        return WILDCARD;
    }

    /**
     * Gets the maximum allowed cache key length.
     *
     * @return Maximum key length
     */
    public static int getMaxKeyLength() {
        return MAX_KEY_LENGTH;
    }
}