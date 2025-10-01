package com.cena.traveloka.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T042: Integration test for Redis Cache Configuration.
 *
 * This test verifies:
 * - Redis container starts successfully
 * - Redis connection can be established
 * - Cache operations (set, get, delete) work correctly
 * - Manual cache eviction (no default TTL as per specifications)
 * - Cache key patterns are followed
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class RedisCacheIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void redisConnectionFactoryIsConfigured() {
        assertThat(redisConnectionFactory).isNotNull();
    }

    @Test
    void canConnectToRedis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.ping()).isEqualTo("PONG");
        }
    }

    @Test
    void redisTemplateIsConfigured() {
        assertThat(redisTemplate).isNotNull();
    }

    @Test
    void canSetAndGetValueFromCache() {
        String key = "test:key:123";
        String value = "test-value";

        // Set value
        redisTemplate.opsForValue().set(key, value);

        // Get value
        Object retrievedValue = redisTemplate.opsForValue().get(key);
        assertThat(retrievedValue).isEqualTo(value);

        // Cleanup
        redisTemplate.delete(key);
    }

    @Test
    void canDeleteKeyFromCache() {
        String key = "test:key:456";
        String value = "test-value-to-delete";

        // Set value
        redisTemplate.opsForValue().set(key, value);
        assertThat(redisTemplate.hasKey(key)).isTrue();

        // Delete key
        Boolean deleted = redisTemplate.delete(key);
        assertThat(deleted).isTrue();
        assertThat(redisTemplate.hasKey(key)).isFalse();
    }

    @Test
    void cacheKeyPatternIsFollowed() {
        // Test module:entity:id pattern as per specifications
        String cacheKey = "user:profile:12345";

        redisTemplate.opsForValue().set(cacheKey, "user-data");
        Object value = redisTemplate.opsForValue().get(cacheKey);

        assertThat(value).isEqualTo("user-data");

        // Cleanup
        redisTemplate.delete(cacheKey);
    }

    @Test
    void manualCacheEvictionWorks() {
        // Per specifications: manual cache eviction, no default TTL
        String key = "test:manual:eviction";
        String value = "evict-me";

        redisTemplate.opsForValue().set(key, value);
        assertThat(redisTemplate.hasKey(key)).isTrue();

        // Manual eviction
        redisTemplate.delete(key);
        assertThat(redisTemplate.hasKey(key)).isFalse();
    }

    @Test
    void canSetValueWithExplicitTTL() {
        // Even though default is no TTL, explicit TTL should work
        String key = "test:ttl:789";
        String value = "expires-soon";

        redisTemplate.opsForValue().set(key, value, 1, TimeUnit.SECONDS);
        assertThat(redisTemplate.hasKey(key)).isTrue();

        // Wait for expiration
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(redisTemplate.hasKey(key)).isFalse();
    }

    @Test
    void canStoreComplexObjects() {
        String key = "test:object:complex";
        TestDto testDto = new TestDto("test-id", "test-name");

        redisTemplate.opsForValue().set(key, testDto);
        Object retrieved = redisTemplate.opsForValue().get(key);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved).isInstanceOf(TestDto.class);
        assertThat(((TestDto) retrieved).getId()).isEqualTo("test-id");
        assertThat(((TestDto) retrieved).getName()).isEqualTo("test-name");

        // Cleanup
        redisTemplate.delete(key);
    }

    @Test
    void canCheckIfKeyExists() {
        String key = "test:exists:check";

        assertThat(redisTemplate.hasKey(key)).isFalse();

        redisTemplate.opsForValue().set(key, "exists");
        assertThat(redisTemplate.hasKey(key)).isTrue();

        redisTemplate.delete(key);
        assertThat(redisTemplate.hasKey(key)).isFalse();
    }

    /**
     * Simple DTO for testing complex object serialization
     */
    static class TestDto implements java.io.Serializable {
        private String id;
        private String name;

        public TestDto() {}

        public TestDto(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
