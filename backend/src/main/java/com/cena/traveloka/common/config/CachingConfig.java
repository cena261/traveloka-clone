package com.cena.traveloka.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = true)
public class CachingConfig {

    @Value("${app.cache.default-ttl:1h}")
    private Duration defaultTtl;

    @Value("${app.cache.null-values:false}")
    private boolean cacheNullValues;

    @Value("${app.cache.key-prefix:traveloka}")
    private String keyPrefix;

    @Value("${app.cache.use-key-prefix:true}")
    private boolean useKeyPrefix;

    @Value("${app.cache.enable-statistics:true}")
    private boolean enableStatistics;

    @Value("${app.cache.ttl.users:30m}")
    private Duration usersCacheTtl;

    @Value("${app.cache.ttl.hotels:2h}")
    private Duration hotelsCacheTtl;

    @Value("${app.cache.ttl.flights:1h}")
    private Duration flightsCacheTtl;

    @Value("${app.cache.ttl.locations:24h}")
    private Duration locationsCacheTtl;

    @Value("${app.cache.ttl.configs:12h}")
    private Duration configsCacheTtl;

    @Value("${app.cache.ttl.sessions:15m}")
    private Duration sessionsCacheTtl;

    @Value("${app.cache.ttl.search-results:10m}")
    private Duration searchResultsCacheTtl;

    @Value("${app.cache.ttl.static-data:7d}")
    private Duration staticDataCacheTtl;

    @Bean
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(defaultTtl)
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        if (!cacheNullValues) {
            defaultCacheConfig = defaultCacheConfig.disableCachingNullValues();
        }

        if (useKeyPrefix) {
            defaultCacheConfig = defaultCacheConfig.prefixCacheNameWith(keyPrefix + ":");
        }

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put("users", createCacheConfig(usersCacheTtl));
        cacheConfigurations.put("user-profiles", createCacheConfig(usersCacheTtl));
        cacheConfigurations.put("user-preferences", createCacheConfig(usersCacheTtl));

        cacheConfigurations.put("hotels", createCacheConfig(hotelsCacheTtl));
        cacheConfigurations.put("hotel-details", createCacheConfig(hotelsCacheTtl));
        cacheConfigurations.put("hotel-amenities", createCacheConfig(hotelsCacheTtl));
        cacheConfigurations.put("hotel-reviews", createCacheConfig(hotelsCacheTtl));

        cacheConfigurations.put("flights", createCacheConfig(flightsCacheTtl));
        cacheConfigurations.put("flight-schedules", createCacheConfig(flightsCacheTtl));
        cacheConfigurations.put("airlines", createCacheConfig(flightsCacheTtl));
        cacheConfigurations.put("airports", createCacheConfig(locationsCacheTtl));

        cacheConfigurations.put("locations", createCacheConfig(locationsCacheTtl));
        cacheConfigurations.put("cities", createCacheConfig(locationsCacheTtl));
        cacheConfigurations.put("countries", createCacheConfig(locationsCacheTtl));
        cacheConfigurations.put("regions", createCacheConfig(locationsCacheTtl));

        cacheConfigurations.put("app-configs", createCacheConfig(configsCacheTtl));
        cacheConfigurations.put("feature-flags", createCacheConfig(configsCacheTtl));
        cacheConfigurations.put("system-settings", createCacheConfig(configsCacheTtl));

        cacheConfigurations.put("user-sessions", createCacheConfig(sessionsCacheTtl));
        cacheConfigurations.put("search-results", createCacheConfig(searchResultsCacheTtl));
        cacheConfigurations.put("search-filters", createCacheConfig(searchResultsCacheTtl));

        cacheConfigurations.put("static-data", createCacheConfig(staticDataCacheTtl));
        cacheConfigurations.put("reference-data", createCacheConfig(staticDataCacheTtl));
        cacheConfigurations.put("currency-rates", createCacheConfig(Duration.ofHours(6)));
        cacheConfigurations.put("translations", createCacheConfig(staticDataCacheTtl));

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultCacheConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build();
    }

    private RedisCacheConfiguration createCacheConfig(Duration ttl) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(ttl)
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        if (!cacheNullValues) {
            config = config.disableCachingNullValues();
        }

        if (useKeyPrefix) {
            config = config.prefixCacheNameWith(keyPrefix + ":");
        }

        return config;
    }
}