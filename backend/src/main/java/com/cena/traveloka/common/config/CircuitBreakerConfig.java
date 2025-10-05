package com.cena.traveloka.common.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class CircuitBreakerConfig {

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
