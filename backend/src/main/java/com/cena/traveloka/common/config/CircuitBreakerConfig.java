package com.cena.traveloka.common.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker Configuration for external service resilience.
 *
 * Configures circuit breakers for:
 * - Redis cache operations
 * - Elasticsearch search operations
 * - MinIO object storage operations
 *
 * Circuit breaker pattern prevents cascading failures by:
 * 1. Opening circuit after failure threshold exceeded
 * 2. Allowing system to recover during open state
 * 3. Testing with half-open state before full recovery
 *
 * Based on research.md specifications for external service fault tolerance.
 */
@Configuration
@Slf4j
public class CircuitBreakerConfig {

    /**
     * Circuit Breaker Registry with custom configurations for external services.
     *
     * Default configuration:
     * - Sliding window size: 10 calls
     * - Failure rate threshold: 50%
     * - Wait duration in open state: 60 seconds
     * - Permitted calls in half-open: 5
     * - Slow call threshold: 2 seconds
     * - Slow call rate threshold: 50%
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig defaultConfig =
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50.0f)
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(5)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(
                    Exception.class
                )
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);

        // Register event listeners for monitoring
        registry.circuitBreaker("default").getEventPublisher()
            .onStateTransition(event ->
                log.info("Circuit Breaker state transition: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onError(event ->
                log.warn("Circuit Breaker error: {}", event.getThrowable().getMessage()))
            .onSuccess(event ->
                log.debug("Circuit Breaker success: {}", event.getElapsedDuration()));

        return registry;
    }

    /**
     * Circuit Breaker for Redis cache operations.
     *
     * Configuration optimized for cache operations:
     * - Faster recovery (30 seconds wait)
     * - Lower failure threshold (40%)
     * - Smaller sliding window (5 calls)
     *
     * Cache failures should fail fast and allow fallback to database.
     */
    @Bean
    public CircuitBreaker redisCircuitBreaker(CircuitBreakerRegistry registry) {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config =
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(5)
                .minimumNumberOfCalls(3)
                .failureRateThreshold(40.0f)
                .slowCallRateThreshold(40.0f)
                .slowCallDurationThreshold(Duration.ofMillis(500))
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        CircuitBreaker circuitBreaker = registry.circuitBreaker("redis", config);

        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.info("Redis Circuit Breaker: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()));

        return circuitBreaker;
    }

    /**
     * Circuit Breaker for Elasticsearch operations.
     *
     * Configuration for search operations:
     * - Standard recovery time (60 seconds)
     * - Standard failure threshold (50%)
     * - Longer slow call threshold (3 seconds for complex queries)
     *
     * Search operations can be more forgiving of slow responses.
     */
    @Bean
    public CircuitBreaker elasticsearchCircuitBreaker(CircuitBreakerRegistry registry) {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config =
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50.0f)
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(5)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        CircuitBreaker circuitBreaker = registry.circuitBreaker("elasticsearch", config);

        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.info("Elasticsearch Circuit Breaker: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()));

        return circuitBreaker;
    }

    /**
     * Circuit Breaker for MinIO object storage operations.
     *
     * Configuration for file operations:
     * - Longer recovery time (90 seconds)
     * - Higher failure threshold (60%)
     * - Very long slow call threshold (10 seconds for large files)
     *
     * File operations are expected to take longer and should be more resilient.
     */
    @Bean
    public CircuitBreaker minioCircuitBreaker(CircuitBreakerRegistry registry) {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config =
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(60.0f)
                .slowCallRateThreshold(60.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(10))
                .waitDurationInOpenState(Duration.ofSeconds(90))
                .permittedNumberOfCallsInHalfOpenState(5)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        CircuitBreaker circuitBreaker = registry.circuitBreaker("minio", config);

        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.info("MinIO Circuit Breaker: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()));

        return circuitBreaker;
    }
}
