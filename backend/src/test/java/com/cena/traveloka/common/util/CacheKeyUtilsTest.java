package com.cena.traveloka.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test class for CacheKeyUtils cache key generation and validation.
 * Tests hierarchical key generation, parameter encoding, and key validation.
 *
 * CRITICAL: These tests MUST FAIL initially (TDD requirement).
 * CacheKeyUtils implementation does not exist yet.
 */
class CacheKeyUtilsTest {

    @Test
    void shouldGenerateSimpleCacheKey() {
        // Given: Simple cache key components
        String prefix = "user";
        String id = "123";

        // When: Generating simple cache key
        String cacheKey = CacheKeyUtils.generateKey(prefix, id);

        // Then: Should create formatted cache key
        assertThat(cacheKey).isEqualTo("user:123");
    }

    @Test
    void shouldGenerateHierarchicalCacheKey() {
        // Given: Hierarchical cache key components
        String[] components = {"users", "profile", "123", "settings"};

        // When: Generating hierarchical cache key
        String cacheKey = CacheKeyUtils.generateKey(components);

        // Then: Should create hierarchical cache key
        assertThat(cacheKey).isEqualTo("users:profile:123:settings");
    }

    @Test
    void shouldGenerateKeyWithParameters() {
        // Given: Cache key with parameters
        String prefix = "search";
        Map<String, Object> parameters = Map.of(
            "query", "hotels",
            "location", "hanoi",
            "checkin", "2025-10-01",
            "guests", 2
        );

        // When: Generating parameterized cache key
        String cacheKey = CacheKeyUtils.generateKeyWithParams(prefix, parameters);

        // Then: Should create parameterized cache key (sorted for consistency)
        assertThat(cacheKey).isEqualTo("search:checkin=2025-10-01:guests=2:location=hanoi:query=hotels");
    }

    @Test
    void shouldGenerateUserSpecificCacheKey() {
        // Given: User-specific cache parameters
        String userId = "user-456";
        String resource = "bookings";
        String action = "list";

        // When: Generating user-specific cache key
        String cacheKey = CacheKeyUtils.generateUserKey(userId, resource, action);

        // Then: Should create user-scoped cache key
        assertThat(cacheKey).isEqualTo("user:user-456:bookings:list");
    }

    @Test
    void shouldGenerateSessionSpecificCacheKey() {
        // Given: Session-specific cache parameters
        String sessionId = "sess-789";
        String data = "cart";

        // When: Generating session-specific cache key
        String cacheKey = CacheKeyUtils.generateSessionKey(sessionId, data);

        // Then: Should create session-scoped cache key
        assertThat(cacheKey).isEqualTo("session:sess-789:cart");
    }

    @ParameterizedTest
    @CsvSource({
        "'user:123', 'user:123'", // already valid
        "'User Profile', 'user_profile'", // spaces to underscores
        "'user-profile!', 'user_profile_'", // special chars to underscores
        "'user::profile', 'user_profile'", // double colons removed
        "'user/profile\\data', 'user_profile_data'", // slashes to underscores
        "'UPPER_case', 'upper_case'" // lowercase conversion
    })
    void shouldSanitizeCacheKeys(String input, String expected) {
        // When: Sanitizing cache key
        String sanitized = CacheKeyUtils.sanitizeKey(input);

        // Then: Should return sanitized cache key
        assertThat(sanitized).isEqualTo(expected);
    }

    @Test
    void shouldGenerateHashedCacheKey() {
        // Given: Long cache key that needs hashing
        String longKey = "very:long:cache:key:with:many:components:that:exceeds:normal:length:limits";

        // When: Generating hashed cache key
        String hashedKey = CacheKeyUtils.generateHashedKey(longKey);

        // Then: Should create hashed cache key with prefix
        assertThat(hashedKey).startsWith("hash:");
        assertThat(hashedKey).hasSize(69); // "hash:" + 64 char SHA-256 hash
        assertThat(hashedKey).matches("hash:[a-f0-9]{64}");
    }

    @Test
    void shouldGeneratePatternBasedCacheKey() {
        // Given: Cache key pattern and values
        String pattern = "hotel:{hotelId}:room:{roomType}:date:{date}";
        Map<String, Object> values = Map.of(
            "hotelId", "hotel-123",
            "roomType", "deluxe",
            "date", "2025-10-01"
        );

        // When: Generating pattern-based cache key
        String cacheKey = CacheKeyUtils.generateFromPattern(pattern, values);

        // Then: Should replace placeholders with values
        assertThat(cacheKey).isEqualTo("hotel:hotel-123:room:deluxe:date:2025-10-01");
    }

    @Test
    void shouldValidateCacheKeyFormat() {
        // Given: Various cache key formats
        String validKey = "user:123:profile";
        String invalidKeyWithSpaces = "user 123 profile";
        String invalidKeyWithSpecialChars = "user@123#profile";
        String emptyKey = "";
        String tooLongKey = "a".repeat(300);

        // When: Validating cache key formats
        boolean validIsValid = CacheKeyUtils.isValidKey(validKey);
        boolean spacesIsValid = CacheKeyUtils.isValidKey(invalidKeyWithSpaces);
        boolean specialCharsIsValid = CacheKeyUtils.isValidKey(invalidKeyWithSpecialChars);
        boolean emptyIsValid = CacheKeyUtils.isValidKey(emptyKey);
        boolean tooLongIsValid = CacheKeyUtils.isValidKey(tooLongKey);

        // Then: Should correctly validate cache key formats
        assertThat(validIsValid).isTrue();
        assertThat(spacesIsValid).isFalse();
        assertThat(specialCharsIsValid).isFalse();
        assertThat(emptyIsValid).isFalse();
        assertThat(tooLongIsValid).isFalse();
    }

    @Test
    void shouldGenerateWildcardPattern() {
        // Given: Cache key prefix for pattern matching
        String prefix = "user:123";

        // When: Generating wildcard pattern
        String pattern = CacheKeyUtils.generateWildcardPattern(prefix);

        // Then: Should create wildcard pattern for key matching
        assertThat(pattern).isEqualTo("user:123:*");
    }

    @Test
    void shouldExtractKeyComponents() {
        // Given: Structured cache key
        String cacheKey = "user:123:profile:settings";

        // When: Extracting key components
        List<String> components = CacheKeyUtils.extractComponents(cacheKey);

        // Then: Should return individual components
        assertThat(components).containsExactly("user", "123", "profile", "settings");
    }

    @Test
    void shouldGenerateExpirationAwareCacheKey() {
        // Given: Cache key with expiration information
        String baseKey = "user:123:data";
        long ttlSeconds = 3600; // 1 hour

        // When: Generating expiration-aware cache key
        String cacheKey = CacheKeyUtils.generateKeyWithTtl(baseKey, ttlSeconds);

        // Then: Should include TTL information in key
        assertThat(cacheKey).isEqualTo("user:123:data:ttl:3600");
    }

    @Test
    void shouldGenerateVersionedCacheKey() {
        // Given: Cache key with version
        String baseKey = "api:config";
        String version = "v2.1";

        // When: Generating versioned cache key
        String cacheKey = CacheKeyUtils.generateVersionedKey(baseKey, version);

        // Then: Should include version in key
        assertThat(cacheKey).isEqualTo("api:config:v:v2.1");
    }

    @Test
    void shouldGenerateTaggedCacheKey() {
        // Given: Cache key with tags
        String baseKey = "product:123";
        List<String> tags = Arrays.asList("electronics", "laptop", "sale");

        // When: Generating tagged cache key
        String cacheKey = CacheKeyUtils.generateTaggedKey(baseKey, tags);

        // Then: Should include sorted tags in key
        assertThat(cacheKey).isEqualTo("product:123:tags:electronics,laptop,sale");
    }

    @Test
    void shouldHandleNullAndEmptyParameters() {
        // When: Generating keys with null/empty parameters
        String nullKey = CacheKeyUtils.generateKey((String[]) null);
        String emptyKey = CacheKeyUtils.generateKey();
        String nullParamsKey = CacheKeyUtils.generateKeyWithParams("test", null);
        String emptyParamsKey = CacheKeyUtils.generateKeyWithParams("test", Collections.emptyMap());

        // Then: Should handle null/empty gracefully
        assertThat(nullKey).isEqualTo("");
        assertThat(emptyKey).isEqualTo("");
        assertThat(nullParamsKey).isEqualTo("test");
        assertThat(emptyParamsKey).isEqualTo("test");
    }

    @Test
    void shouldGenerateNamespacedCacheKey() {
        // Given: Namespace and key components
        String namespace = "traveloka";
        String[] components = {"user", "123", "bookings"};

        // When: Generating namespaced cache key
        String cacheKey = CacheKeyUtils.generateNamespacedKey(namespace, components);

        // Then: Should include namespace prefix
        assertThat(cacheKey).isEqualTo("traveloka:user:123:bookings");
    }

    @Test
    void shouldGenerateEnvironmentSpecificCacheKey() {
        // Given: Environment and key components
        String environment = "prod";
        String baseKey = "config:api:rates";

        // When: Generating environment-specific cache key
        String cacheKey = CacheKeyUtils.generateEnvironmentKey(environment, baseKey);

        // Then: Should include environment prefix
        assertThat(cacheKey).isEqualTo("env:prod:config:api:rates");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "user:123:profile",
        "search:query:hotels",
        "session:abc123:cart",
        "api:v1:config",
        "hotel:456:rooms:available"
    })
    void shouldParseValidCacheKeys(String cacheKey) {
        // When: Parsing cache key
        CacheKeyUtils.ParsedKey parsed = CacheKeyUtils.parseKey(cacheKey);

        // Then: Should successfully parse key components
        assertThat(parsed).isNotNull();
        assertThat(parsed.getComponents()).isNotEmpty();
        assertThat(String.join(":", parsed.getComponents())).isEqualTo(cacheKey);
    }

    @Test
    void shouldGenerateCompositeKey() {
        // Given: Multiple key parts
        Map<String, Object> keyParts = Map.of(
            "service", "booking",
            "operation", "search",
            "userId", "123",
            "timestamp", "2025-09-27"
        );

        // When: Generating composite key
        String compositeKey = CacheKeyUtils.generateCompositeKey(keyParts);

        // Then: Should create consistent composite key (sorted)
        assertThat(compositeKey).isEqualTo("operation=search:service=booking:timestamp=2025-09-27:userId=123");
    }

    @Test
    void shouldCalculateKeySize() {
        // Given: Various cache keys
        String shortKey = "user:123";
        String longKey = "very:long:cache:key:with:many:components";

        // When: Calculating key sizes
        int shortKeySize = CacheKeyUtils.calculateKeySize(shortKey);
        int longKeySize = CacheKeyUtils.calculateKeySize(longKey);

        // Then: Should return correct byte sizes
        assertThat(shortKeySize).isEqualTo(8); // "user:123" = 8 bytes
        assertThat(longKeySize).isEqualTo(longKey.length());
    }

    @Test
    void shouldGenerateKeyWithChecksum() {
        // Given: Base cache key
        String baseKey = "user:123:profile";

        // When: Generating key with checksum
        String keyWithChecksum = CacheKeyUtils.generateKeyWithChecksum(baseKey);

        // Then: Should append checksum to key
        assertThat(keyWithChecksum).startsWith("user:123:profile:checksum:");
        assertThat(keyWithChecksum).matches("user:123:profile:checksum:[a-f0-9]+");
    }

    @Test
    void shouldThrowExceptionForInvalidKeyGeneration() {
        // When/Then: Should throw exception for invalid parameters
        assertThatThrownBy(() -> CacheKeyUtils.generateKeyWithParams(null, Map.of("key", "value")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Prefix cannot be null");

        assertThatThrownBy(() -> CacheKeyUtils.generateFromPattern(null, Map.of("key", "value")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Pattern cannot be null");

        assertThatThrownBy(() -> CacheKeyUtils.generateNamespacedKey("", "component"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Namespace cannot be empty");
    }

    @Test
    void shouldEscapeSpecialCharactersInValues() {
        // Given: Values with special characters
        Map<String, Object> parameters = Map.of(
            "query", "hotels:resorts",
            "location", "ho chi minh city",
            "special", "price>100&rating>=4"
        );

        // When: Generating cache key with special characters
        String cacheKey = CacheKeyUtils.generateKeyWithParams("search", parameters);

        // Then: Should escape special characters in values
        assertThat(cacheKey).contains("location=ho_chi_minh_city");
        assertThat(cacheKey).contains("query=hotels_resorts");
        assertThat(cacheKey).contains("special=price_100_rating__4");
    }

    @Test
    void shouldSupportCustomSeparators() {
        // Given: Custom separator
        String separator = "-";
        String[] components = {"user", "123", "profile"};

        // When: Generating key with custom separator
        String cacheKey = CacheKeyUtils.generateKeyWithSeparator(separator, components);

        // Then: Should use custom separator
        assertThat(cacheKey).isEqualTo("user-123-profile");
    }

    @Test
    void shouldGenerateKeyForComplexObjects() {
        // Given: Complex object representation
        Object complexObject = new TestCacheObject("test", 123, Arrays.asList("tag1", "tag2"));

        // When: Generating cache key for complex object
        String cacheKey = CacheKeyUtils.generateKeyForObject("data", complexObject);

        // Then: Should create deterministic key for object
        assertThat(cacheKey).startsWith("data:obj:");
        assertThat(cacheKey).contains("test");
        assertThat(cacheKey).contains("123");
    }

    /**
     * Test object for complex cache key generation
     */
    public static class TestCacheObject {
        private final String name;
        private final Integer value;
        private final List<String> tags;

        public TestCacheObject(String name, Integer value, List<String> tags) {
            this.name = name;
            this.value = value;
            this.tags = tags;
        }

        public String getName() { return name; }
        public Integer getValue() { return value; }
        public List<String> getTags() { return tags; }
    }
}