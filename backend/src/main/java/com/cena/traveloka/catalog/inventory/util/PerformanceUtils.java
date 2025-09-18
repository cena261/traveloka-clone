package com.cena.traveloka.catalog.inventory.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@UtilityClass
@Slf4j
public class PerformanceUtils {

    /**
     * Executes a task asynchronously and logs performance metrics
     */
    public static <T> CompletableFuture<T> executeAsync(String taskName, Supplier<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                log.debug("Starting async task: {}", taskName);
                T result = task.get();
                long duration = System.currentTimeMillis() - startTime;
                log.debug("Completed async task: {} in {}ms", taskName, duration);
                return result;
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("Failed async task: {} after {}ms - {}", taskName, duration, e.getMessage());
                throw new RuntimeException("Async task failed: " + taskName, e);
            }
        });
    }

    /**
     * Measures execution time of a task
     */
    public static <T> T measureExecutionTime(String taskName, Supplier<T> task) {
        long startTime = System.currentTimeMillis();
        try {
            T result = task.get();
            long duration = System.currentTimeMillis() - startTime;

            if (duration > InventoryConstants.SEARCH_PERFORMANCE_THRESHOLD_MS) {
                log.warn("Slow operation detected: {} took {}ms (threshold: {}ms)",
                        taskName, duration, InventoryConstants.SEARCH_PERFORMANCE_THRESHOLD_MS);
            } else {
                log.debug("Operation completed: {} in {}ms", taskName, duration);
            }

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Operation failed: {} after {}ms - {}", taskName, duration, e.getMessage());
            throw e;
        }
    }

    /**
     * Creates a cache key with proper formatting
     */
    public static String createCacheKey(String prefix, Object... params) {
        StringBuilder keyBuilder = new StringBuilder(prefix);
        for (Object param : params) {
            keyBuilder.append("-").append(param != null ? param.toString() : "null");
        }
        return keyBuilder.toString();
    }

    /**
     * Validates search parameters for performance optimization
     */
    public static void validateSearchParameters(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be non-negative");
        }
        if (size <= 0 || size > InventoryConstants.MAX_SEARCH_RESULTS) {
            throw new IllegalArgumentException(
                String.format("Page size must be between 1 and %d", InventoryConstants.MAX_SEARCH_RESULTS)
            );
        }
    }

    /**
     * Optimizes search radius for performance
     */
    public static double optimizeSearchRadius(double requestedRadius) {
        if (requestedRadius <= 0) {
            return InventoryConstants.DEFAULT_SEARCH_RADIUS_KM;
        }
        if (requestedRadius > InventoryConstants.MAX_SEARCH_RADIUS_KM) {
            log.warn("Search radius {} km exceeds maximum {}, using maximum",
                    requestedRadius, InventoryConstants.MAX_SEARCH_RADIUS_KM);
            return InventoryConstants.MAX_SEARCH_RADIUS_KM;
        }
        return requestedRadius;
    }

    /**
     * Calculates optimal batch size for bulk operations
     */
    public static int calculateOptimalBatchSize(int totalItems) {
        if (totalItems <= 10) return totalItems;
        if (totalItems <= 100) return 10;
        if (totalItems <= 1000) return 50;
        return 100; // Maximum batch size
    }
}