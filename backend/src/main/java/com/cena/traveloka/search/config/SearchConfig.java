package com.cena.traveloka.search.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@Slf4j
public class SearchConfig implements CachingConfigurer {

    @Bean
    public RedisTemplate<String, Object> searchRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        log.info("Search Redis template configured successfully");
        return template;
    }

    @Bean
    @Override
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)) // Default 5-minute TTL
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Search results cache - 5 minutes TTL
        cacheConfigurations.put("searchResults", defaultConfig
                .entryTtl(Duration.ofMinutes(5)));

        // Auto-complete suggestions cache - 30 minutes TTL
        cacheConfigurations.put("searchSuggestions", defaultConfig
                .entryTtl(Duration.ofMinutes(30)));

        // Popular destinations cache - 1 hour TTL
        cacheConfigurations.put("popularDestinations", defaultConfig
                .entryTtl(Duration.ofHours(1)));

        // Filter options cache - 15 minutes TTL
        cacheConfigurations.put("searchFilters", defaultConfig
                .entryTtl(Duration.ofMinutes(15)));

        // Property details cache - 10 minutes TTL
        cacheConfigurations.put("propertyDetails", defaultConfig
                .entryTtl(Duration.ofMinutes(10)));

        // Location-based search cache - 2 minutes TTL (more dynamic)
        cacheConfigurations.put("locationSearch", defaultConfig
                .entryTtl(Duration.ofMinutes(2)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();

        log.info("Search cache manager configured with {} cache types", cacheConfigurations.size());
        return cacheManager;
    }

    @Bean
    public SearchCacheKeyGenerator searchCacheKeyGenerator() {
        return new SearchCacheKeyGenerator();
    }

    /**
     * Custom cache key generator for search operations
     * Generates hierarchical cache keys based on search parameters
     */
    public static class SearchCacheKeyGenerator implements org.springframework.cache.interceptor.KeyGenerator {

        @Override
        public Object generate(Object target, java.lang.reflect.Method method, Object... params) {
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append(target.getClass().getSimpleName())
                    .append(":")
                    .append(method.getName());

            for (Object param : params) {
                if (param != null) {
                    keyBuilder.append(":")
                            .append(param.toString().hashCode());
                }
            }

            return keyBuilder.toString();
        }
    }

    /**
     * Search-specific cache configuration
     */
    @Bean
    public SearchCacheConfig searchCacheConfig() {
        return SearchCacheConfig.builder()
                .searchResultsTtl(Duration.ofMinutes(5))
                .suggestionsTtl(Duration.ofMinutes(30))
                .popularDestinationsTtl(Duration.ofHours(1))
                .filtersTtl(Duration.ofMinutes(15))
                .locationSearchTtl(Duration.ofMinutes(2))
                .maxCacheSize(10000L)
                .enableCacheMetrics(true)
                .build();
    }

    @lombok.Builder
    @lombok.Data
    public static class SearchCacheConfig {
        private Duration searchResultsTtl;
        private Duration suggestionsTtl;
        private Duration popularDestinationsTtl;
        private Duration filtersTtl;
        private Duration locationSearchTtl;
        private Long maxCacheSize;
        private Boolean enableCacheMetrics;
    }
}
