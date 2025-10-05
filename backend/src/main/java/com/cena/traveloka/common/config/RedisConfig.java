package com.cena.traveloka.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Value("${spring.data.redis.username:}")
    private String username;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.timeout:2000ms}")
    private Duration timeout;

    @Value("${app.redis.pool.max-total:20}")
    private int maxTotal;

    @Value("${app.redis.pool.max-idle:10}")
    private int maxIdle;

    @Value("${app.redis.pool.min-idle:2}")
    private int minIdle;

    @Value("${app.redis.pool.max-wait-millis:3000}")
    private long maxWaitMillis;

    @Value("${app.redis.pool.test-on-borrow:true}")
    private boolean testOnBorrow;

    @Value("${app.redis.pool.test-on-return:false}")
    private boolean testOnReturn;

    @Value("${app.redis.pool.test-while-idle:true}")
    private boolean testWhileIdle;

    @Value("${app.redis.pool.time-between-eviction-runs:30000}")
    private long timeBetweenEvictionRuns;

    @Value("${app.redis.pool.num-tests-per-eviction-run:3}")
    private int numTestsPerEvictionRun;

    @Value("${app.redis.pool.min-evictable-idle-time:60000}")
    private long minEvictableIdleTime;

    @Value("${app.redis.pool.soft-min-evictable-idle-time:30000}")
    private long softMinEvictableIdleTime;

    @Value("${app.redis.pool.block-when-exhausted:true}")
    private boolean blockWhenExhausted;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(host);
        redisConfig.setPort(port);
        redisConfig.setDatabase(database);

        if (username != null && !username.trim().isEmpty()) {
            redisConfig.setUsername(username);
        }

        if (password != null && !password.trim().isEmpty()) {
            redisConfig.setPassword(password);
        }

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxWait(Duration.ofMillis(maxWaitMillis));

        poolConfig.setTestOnBorrow(testOnBorrow);
        poolConfig.setTestOnReturn(testOnReturn);
        poolConfig.setTestWhileIdle(testWhileIdle);

        poolConfig.setTimeBetweenEvictionRuns(Duration.ofMillis(timeBetweenEvictionRuns));
        poolConfig.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        poolConfig.setMinEvictableIdleTime(Duration.ofMillis(minEvictableIdleTime));
        poolConfig.setSoftMinEvictableIdleTime(Duration.ofMillis(softMinEvictableIdleTime));
        poolConfig.setBlockWhenExhausted(blockWhenExhausted);

        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(redisConfig);
        jedisConnectionFactory.setTimeout((int) timeout.toMillis());

        return jedisConnectionFactory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);

        template.setEnableTransactionSupport(true);

        template.setDefaultSerializer(jsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

}