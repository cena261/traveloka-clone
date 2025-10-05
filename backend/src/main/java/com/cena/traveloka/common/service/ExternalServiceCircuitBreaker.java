package com.cena.traveloka.common.service;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalServiceCircuitBreaker {

    private final CircuitBreaker redisCircuitBreaker;
    private final CircuitBreaker elasticsearchCircuitBreaker;
    private final CircuitBreaker minioCircuitBreaker;

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

    public <T> CompletableFuture<T> executeElasticsearchAsync(Supplier<T> supplier, T fallback) {
        return CompletableFuture.supplyAsync(() -> executeElasticsearch(supplier, fallback));
    }

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

    public <T> CompletableFuture<T> executeMinioAsync(Supplier<T> supplier, T fallback) {
        return CompletableFuture.supplyAsync(() -> executeMinio(supplier, fallback));
    }

    public boolean isRedisCircuitOpen() {
        return redisCircuitBreaker.getState() == CircuitBreaker.State.OPEN;
    }

    public boolean isElasticsearchCircuitOpen() {
        return elasticsearchCircuitBreaker.getState() == CircuitBreaker.State.OPEN;
    }

    public boolean isMinioCircuitOpen() {
        return minioCircuitBreaker.getState() == CircuitBreaker.State.OPEN;
    }

    public CircuitBreaker.State getRedisCircuitState() {
        return redisCircuitBreaker.getState();
    }

    public CircuitBreaker.State getElasticsearchCircuitState() {
        return elasticsearchCircuitBreaker.getState();
    }

    public CircuitBreaker.State getMinioCircuitState() {
        return minioCircuitBreaker.getState();
    }

    public void resetRedisCircuit() {
        log.info("Manually resetting Redis circuit breaker");
        redisCircuitBreaker.transitionToClosedState();
    }

    public void resetElasticsearchCircuit() {
        log.info("Manually resetting Elasticsearch circuit breaker");
        elasticsearchCircuitBreaker.transitionToClosedState();
    }

    public void resetMinioCircuit() {
        log.info("Manually resetting MinIO circuit breaker");
        minioCircuitBreaker.transitionToClosedState();
    }
}
