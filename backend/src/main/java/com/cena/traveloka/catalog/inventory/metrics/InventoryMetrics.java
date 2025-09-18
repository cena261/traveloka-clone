package com.cena.traveloka.catalog.inventory.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryMetrics {

    private final MeterRegistry meterRegistry;

    // Partner metrics
    public void incrementPartnerCreated() {
        Counter.builder("inventory.partner.created")
                .description("Number of partners created")
                .register(meterRegistry)
                .increment();
    }

    public void incrementPartnerActivated() {
        Counter.builder("inventory.partner.activated")
                .description("Number of partners activated")
                .register(meterRegistry)
                .increment();
    }

    // Property metrics
    public void incrementPropertyCreated() {
        Counter.builder("inventory.property.created")
                .description("Number of properties created")
                .register(meterRegistry)
                .increment();
    }

    public void incrementPropertyActivated() {
        Counter.builder("inventory.property.activated")
                .description("Number of properties activated")
                .register(meterRegistry)
                .increment();
    }

    // Search metrics
    public void recordSearchTime(long timeInMs) {
        Timer.builder("inventory.search.duration")
                .description("Property search duration")
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(timeInMs));
    }

    // Image upload metrics
    public void incrementImageUploaded() {
        Counter.builder("inventory.image.uploaded")
                .description("Number of images uploaded")
                .register(meterRegistry)
                .increment();
    }

    // Error metrics
    public void incrementError(String errorType) {
        Counter.builder("inventory.error")
                .description("Number of errors")
                .tag("type", errorType)
                .register(meterRegistry)
                .increment();
    }

    // Cache metrics
    public void incrementCacheHit(String cacheName) {
        Counter.builder("inventory.cache.hit")
                .description("Cache hits")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .increment();
    }

    public void incrementCacheMiss(String cacheName) {
        Counter.builder("inventory.cache.miss")
                .description("Cache misses")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .increment();
    }
}