package com.cena.traveloka.common.service;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Wrapper service for executing external service calls with circuit breaker protection.
 *
 * This service provides a unified interface for executing operations against
 * external services (Redis, Elasticsearch, MinIO) with automatic circuit breaker
 * protection and fallback handling.
 *
 * Usage example:
 * <pre>
 * String result = circuitBreakerService.executeRedis(
 *     () -> redisTemplate.opsForValue().get(key),
 *     null // fallback value
 * );
 * </pre>
 *
 * Based on research.md specifications for external service resilience.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalServiceCircuitBreaker {

    private final CircuitBreaker redisCircuitBreaker;
    private final CircuitBreaker elasticsearchCircuitBreaker;
    private final CircuitBreaker minioCircuitBreaker;

    /**
     * Execute a Redis operation with circuit breaker protection.
     *
     * @param supplier The Redis operation to execute
     * @param fallback Fallback value if circuit is open or operation fails
     * @param <T> Return type
     * @return Result of operation or fallback value
     */
    public <T> T executeRedis(Supplier<T> supplier, T fallback) {
        try {
            Supplier<T> decoratedSupplier = CircuitBreaker
                .decorateSupplier(redisCircuitBreaker, supplier);
            return decoratedSupplier.get();
        } catch (CallNotPermittedException e) {
            log.warn("Redis circuit breaker is OPEN, using fallback");
            return fallback;
        } catch (Exception e) {
            log.error("Redis operation failed: {}", e.getMessage(), e);
            return fallback;
        }
    }

    /**
     * Execute a Redis operation with circuit breaker protection (void return).
     *
     * @param runnable The Redis operation to execute
     * @return true if successful, false if circuit is open or operation fails
     */
    public boolean executeRedisVoid(Runnable runnable) {
        try {
            Runnable decoratedRunnable = CircuitBreaker
                .decorateRunnable(redisCircuitBreaker, runnable);
            decoratedRunnable.run();
            return true;
        } catch (CallNotPermittedException e) {
            log.warn("Redis circuit breaker is OPEN, operation skipped");
            return false;
        } catch (Exception e) {
            log.error("Redis operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Execute an Elasticsearch operation with circuit breaker protection.
     *
     * @param supplier The Elasticsearch operation to execute
     * @param fallback Fallback value if circuit is open or operation fails
     * @param <T> Return type
     * @return Result of operation or fallback value
     */
    public <T> T executeElasticsearch(Supplier<T> supplier, T fallback) {
        try {
            Supplier<T> decoratedSupplier = CircuitBreaker
                .decorateSupplier(elasticsearchCircuitBreaker, supplier);
            return decoratedSupplier.get();
        } catch (CallNotPermittedException e) {
            log.warn("Elasticsearch circuit breaker is OPEN, using fallback");
            return fallback;
        } catch (Exception e) {
            log.error("Elasticsearch operation failed: {}", e.getMessage(), e);
            return fallback;
        }
    }

    /**
     * Execute an Elasticsearch operation asynchronously with circuit breaker protection.
     *
     * @param supplier The Elasticsearch operation to execute
     * @param fallback Fallback value if circuit is open or operation fails
     * @param <T> Return type
     * @return CompletableFuture with result or fallback
     */
    public <T> CompletableFuture<T> executeElasticsearchAsync(Supplier<T> supplier, T fallback) {
        return CompletableFuture.supplyAsync(() -> executeElasticsearch(supplier, fallback));
    }

    /**
     * Execute a MinIO operation with circuit breaker protection.
     *
     * @param supplier The MinIO operation to execute
     * @param fallback Fallback value if circuit is open or operation fails
     * @param <T> Return type
     * @return Result of operation or fallback value
     */
    public <T> T executeMinio(Supplier<T> supplier, T fallback) {
        try {
            Supplier<T> decoratedSupplier = CircuitBreaker
                .decorateSupplier(minioCircuitBreaker, supplier);
            return decoratedSupplier.get();
        } catch (CallNotPermittedException e) {
            log.warn("MinIO circuit breaker is OPEN, using fallback");
            return fallback;
        } catch (Exception e) {
            log.error("MinIO operation failed: {}", e.getMessage(), e);
            return fallback;
        }
    }

    /**
     * Execute a MinIO operation with circuit breaker protection (void return).
     *
     * @param runnable The MinIO operation to execute
     * @return true if successful, false if circuit is open or operation fails
     */
    public boolean executeMinioVoid(Runnable runnable) {
        try {
            Runnable decoratedRunnable = CircuitBreaker
                .decorateRunnable(minioCircuitBreaker, runnable);
            decoratedRunnable.run();
            return true;
        } catch (CallNotPermittedException e) {
            log.warn("MinIO circuit breaker is OPEN, operation skipped");
            return false;
        } catch (Exception e) {
            log.error("MinIO operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Execute a MinIO operation asynchronously with circuit breaker protection.
     *
     * @param supplier The MinIO operation to execute
     * @param fallback Fallback value if circuit is open or operation fails
     * @param <T> Return type
     * @return CompletableFuture with result or fallback
     */
    public <T> CompletableFuture<T> executeMinioAsync(Supplier<T> supplier, T fallback) {
        return CompletableFuture.supplyAsync(() -> executeMinio(supplier, fallback));
    }

    /**
     * Check if Redis circuit breaker is currently open.
     *
     * @return true if circuit is open (service unavailable)
     */
    public boolean isRedisCircuitOpen() {
        return redisCircuitBreaker.getState() == CircuitBreaker.State.OPEN;
    }

    /**
     * Check if Elasticsearch circuit breaker is currently open.
     *
     * @return true if circuit is open (service unavailable)
     */
    public boolean isElasticsearchCircuitOpen() {
        return elasticsearchCircuitBreaker.getState() == CircuitBreaker.State.OPEN;
    }

    /**
     * Check if MinIO circuit breaker is currently open.
     *
     * @return true if circuit is open (service unavailable)
     */
    public boolean isMinioCircuitOpen() {
        return minioCircuitBreaker.getState() == CircuitBreaker.State.OPEN;
    }

    /**
     * Get current state of Redis circuit breaker.
     *
     * @return Circuit breaker state (CLOSED, OPEN, HALF_OPEN)
     */
    public CircuitBreaker.State getRedisCircuitState() {
        return redisCircuitBreaker.getState();
    }

    /**
     * Get current state of Elasticsearch circuit breaker.
     *
     * @return Circuit breaker state (CLOSED, OPEN, HALF_OPEN)
     */
    public CircuitBreaker.State getElasticsearchCircuitState() {
        return elasticsearchCircuitBreaker.getState();
    }

    /**
     * Get current state of MinIO circuit breaker.
     *
     * @return Circuit breaker state (CLOSED, OPEN, HALF_OPEN)
     */
    public CircuitBreaker.State getMinioCircuitState() {
        return minioCircuitBreaker.getState();
    }

    /**
     * Manually transition Redis circuit breaker to CLOSED state.
     * Use with caution - typically circuit breakers should recover automatically.
     */
    public void resetRedisCircuit() {
        log.info("Manually resetting Redis circuit breaker");
        redisCircuitBreaker.transitionToClosedState();
    }

    /**
     * Manually transition Elasticsearch circuit breaker to CLOSED state.
     * Use with caution - typically circuit breakers should recover automatically.
     */
    public void resetElasticsearchCircuit() {
        log.info("Manually resetting Elasticsearch circuit breaker");
        elasticsearchCircuitBreaker.transitionToClosedState();
    }

    /**
     * Manually transition MinIO circuit breaker to CLOSED state.
     * Use with caution - typically circuit breakers should recover automatically.
     */
    public void resetMinioCircuit() {
        log.info("Manually resetting MinIO circuit breaker");
        minioCircuitBreaker.transitionToClosedState();
    }
}
