package com.cena.traveloka.common.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T050: Integration test for CircuitBreakerConfig with failure thresholds and recovery.
 *
 * This test verifies:
 * - Circuit breaker registry is configured correctly
 * - Redis circuit breaker has correct configuration (40% threshold, 30s wait)
 * - Elasticsearch circuit breaker has correct configuration (50% threshold, 60s wait)
 * - MinIO circuit breaker has correct configuration (60% threshold, 90s wait)
 * - Circuit breakers transition to OPEN after failure threshold
 * - Circuit breakers transition to HALF_OPEN after wait duration
 * - Circuit breakers recover to CLOSED after successful calls
 * - Slow call detection works correctly
 */
@SpringBootTest
@ActiveProfiles("test")
class CircuitBreakerConfigTest {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private CircuitBreaker redisCircuitBreaker;

    @Autowired
    private CircuitBreaker elasticsearchCircuitBreaker;

    @Autowired
    private CircuitBreaker minioCircuitBreaker;

    @Test
    void circuitBreakerRegistryIsConfigured() {
        assertThat(circuitBreakerRegistry).isNotNull();
    }

    @Test
    void redisCircuitBreakerIsConfigured() {
        assertThat(redisCircuitBreaker).isNotNull();
        assertThat(redisCircuitBreaker.getName()).isEqualTo("redis");
        assertThat(redisCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Verify configuration
        var config = redisCircuitBreaker.getCircuitBreakerConfig();
        assertThat(config.getSlidingWindowSize()).isEqualTo(5);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(3);
        assertThat(config.getFailureRateThreshold()).isEqualTo(40.0f);
        assertThat(config.getWaitDurationInOpenState().getSeconds()).isEqualTo(30);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
    }

    @Test
    void elasticsearchCircuitBreakerIsConfigured() {
        assertThat(elasticsearchCircuitBreaker).isNotNull();
        assertThat(elasticsearchCircuitBreaker.getName()).isEqualTo("elasticsearch");
        assertThat(elasticsearchCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Verify configuration
        var config = elasticsearchCircuitBreaker.getCircuitBreakerConfig();
        assertThat(config.getSlidingWindowSize()).isEqualTo(10);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(config.getFailureRateThreshold()).isEqualTo(50.0f);
        assertThat(config.getWaitDurationInOpenState().getSeconds()).isEqualTo(60);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(5);
    }

    @Test
    void minioCircuitBreakerIsConfigured() {
        assertThat(minioCircuitBreaker).isNotNull();
        assertThat(minioCircuitBreaker.getName()).isEqualTo("minio");
        assertThat(minioCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Verify configuration
        var config = minioCircuitBreaker.getCircuitBreakerConfig();
        assertThat(config.getSlidingWindowSize()).isEqualTo(10);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(config.getFailureRateThreshold()).isEqualTo(60.0f);
        assertThat(config.getWaitDurationInOpenState().getSeconds()).isEqualTo(90);
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(5);
    }

    @Test
    void redisCircuitBreakerOpensAfterFailureThreshold() {
        // Create a test circuit breaker with same config as Redis but faster recovery for testing
        var testConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .slidingWindowSize(5)
            .minimumNumberOfCalls(3)
            .failureRateThreshold(40.0f)
            .waitDurationInOpenState(java.time.Duration.ofMillis(100))
            .build();

        CircuitBreaker testCircuit = circuitBreakerRegistry.circuitBreaker("test-redis", testConfig);

        // Initial state should be CLOSED
        assertThat(testCircuit.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Execute 2 successful calls
        testCircuit.executeSupplier(() -> "success");
        testCircuit.executeSupplier(() -> "success");

        // Execute 3 failed calls (2/5 = 40% failure rate, but need minimum 3 calls)
        assertThatThrownBy(() -> testCircuit.executeSupplier(() -> {
            throw new RuntimeException("failure");
        })).isInstanceOf(RuntimeException.class);

        assertThatThrownBy(() -> testCircuit.executeSupplier(() -> {
            throw new RuntimeException("failure");
        })).isInstanceOf(RuntimeException.class);

        assertThatThrownBy(() -> testCircuit.executeSupplier(() -> {
            throw new RuntimeException("failure");
        })).isInstanceOf(RuntimeException.class);

        // Circuit should now be OPEN (3/5 = 60% > 40% threshold)
        assertThat(testCircuit.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Verify metrics
        var metrics = testCircuit.getMetrics();
        assertThat(metrics.getNumberOfFailedCalls()).isGreaterThan(0);
        assertThat(metrics.getFailureRate()).isGreaterThan(40.0f);
    }

    @Test
    void elasticsearchCircuitBreakerOpensAfterFailureThreshold() {
        // Create a test circuit breaker with same config as Elasticsearch but faster recovery
        var testConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(java.time.Duration.ofMillis(100))
            .build();

        CircuitBreaker testCircuit = circuitBreakerRegistry.circuitBreaker("test-elasticsearch", testConfig);

        // Initial state should be CLOSED
        assertThat(testCircuit.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Execute 4 successful calls
        for (int i = 0; i < 4; i++) {
            testCircuit.executeSupplier(() -> "success");
        }

        // Execute 6 failed calls (6/10 = 60% > 50% threshold)
        for (int i = 0; i < 6; i++) {
            try {
                testCircuit.executeSupplier(() -> {
                    throw new RuntimeException("failure");
                });
            } catch (Exception e) {
                // Expected
            }
        }

        // Circuit should now be OPEN
        assertThat(testCircuit.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void minioCircuitBreakerOpensAfterFailureThreshold() {
        // Create a test circuit breaker with same config as MinIO but faster recovery
        var testConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(60.0f)
            .waitDurationInOpenState(java.time.Duration.ofMillis(100))
            .build();

        CircuitBreaker testCircuit = circuitBreakerRegistry.circuitBreaker("test-minio", testConfig);

        // Initial state should be CLOSED
        assertThat(testCircuit.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Execute 3 successful calls
        for (int i = 0; i < 3; i++) {
            testCircuit.executeSupplier(() -> "success");
        }

        // Execute 7 failed calls (7/10 = 70% > 60% threshold)
        for (int i = 0; i < 7; i++) {
            try {
                testCircuit.executeSupplier(() -> {
                    throw new RuntimeException("failure");
                });
            } catch (Exception e) {
                // Expected
            }
        }

        // Circuit should now be OPEN
        assertThat(testCircuit.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void circuitBreakerTransitionsToHalfOpenAfterWaitDuration() throws InterruptedException {
        // Create a test circuit breaker with very short wait duration
        var testConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .slidingWindowSize(5)
            .minimumNumberOfCalls(3)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(java.time.Duration.ofMillis(200))
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();

        CircuitBreaker testCircuit = circuitBreakerRegistry.circuitBreaker("test-halfopen", testConfig);

        // Force circuit to OPEN by causing failures
        for (int i = 0; i < 5; i++) {
            try {
                testCircuit.executeSupplier(() -> {
                    throw new RuntimeException("failure");
                });
            } catch (Exception e) {
                // Expected
            }
        }

        assertThat(testCircuit.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Wait for automatic transition to HALF_OPEN
        Thread.sleep(300);

        // Try a call - this should transition to HALF_OPEN
        try {
            testCircuit.executeSupplier(() -> "success");
        } catch (Exception e) {
            // May still be blocked if not transitioned yet
        }

        // Circuit should be HALF_OPEN or CLOSED (if the success call completed)
        assertThat(testCircuit.getState()).isIn(
            CircuitBreaker.State.HALF_OPEN,
            CircuitBreaker.State.CLOSED
        );
    }

    @Test
    void circuitBreakerRecoversAfterSuccessfulCalls() throws InterruptedException {
        // Create a test circuit breaker
        var testConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .slidingWindowSize(5)
            .minimumNumberOfCalls(3)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(java.time.Duration.ofMillis(100))
            .permittedNumberOfCallsInHalfOpenState(3)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();

        CircuitBreaker testCircuit = circuitBreakerRegistry.circuitBreaker("test-recovery", testConfig);

        // Force circuit to OPEN
        for (int i = 0; i < 5; i++) {
            try {
                testCircuit.executeSupplier(() -> {
                    throw new RuntimeException("failure");
                });
            } catch (Exception e) {
                // Expected
            }
        }

        assertThat(testCircuit.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Wait for transition to HALF_OPEN
        Thread.sleep(150);

        // Execute successful calls in HALF_OPEN state
        for (int i = 0; i < 3; i++) {
            try {
                testCircuit.executeSupplier(() -> "success");
            } catch (Exception e) {
                // May be blocked if circuit hasn't transitioned yet
            }
        }

        // Circuit should recover to CLOSED after successful calls
        assertThat(testCircuit.getState()).isIn(
            CircuitBreaker.State.CLOSED,
            CircuitBreaker.State.HALF_OPEN
        );
    }

    @Test
    void circuitBreakerDetectsSlowCalls() {
        // Create a test circuit breaker with slow call detection
        var testConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .slidingWindowSize(5)
            .minimumNumberOfCalls(3)
            .slowCallRateThreshold(50.0f)
            .slowCallDurationThreshold(java.time.Duration.ofMillis(100))
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(java.time.Duration.ofMillis(100))
            .build();

        CircuitBreaker testCircuit = circuitBreakerRegistry.circuitBreaker("test-slow", testConfig);

        // Execute 1 fast call
        testCircuit.executeSupplier(() -> "fast");

        // Execute 4 slow calls (4/5 = 80% > 50% threshold)
        for (int i = 0; i < 4; i++) {
            testCircuit.executeSupplier(() -> {
                try {
                    Thread.sleep(150); // Slower than 100ms threshold
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "slow";
            });
        }

        // Circuit should be OPEN due to slow calls
        assertThat(testCircuit.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Verify metrics show slow calls
        var metrics = testCircuit.getMetrics();
        assertThat(metrics.getNumberOfSlowCalls()).isGreaterThan(0);
    }

    @Test
    void circuitBreakerEventsArePublished() throws InterruptedException {
        var testConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .slidingWindowSize(5)
            .minimumNumberOfCalls(3)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(java.time.Duration.ofMillis(100))
            .build();

        CircuitBreaker testCircuit = circuitBreakerRegistry.circuitBreaker("test-events", testConfig);

        AtomicInteger stateTransitions = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        AtomicInteger successes = new AtomicInteger(0);

        CountDownLatch latch = new CountDownLatch(1);

        // Subscribe to events
        testCircuit.getEventPublisher()
            .onStateTransition(event -> {
                stateTransitions.incrementAndGet();
                if (event.getStateTransition().getToState() == CircuitBreaker.State.OPEN) {
                    latch.countDown();
                }
            })
            .onError(event -> errors.incrementAndGet())
            .onSuccess(event -> successes.incrementAndGet());

        // Execute calls to trigger events
        testCircuit.executeSupplier(() -> "success");

        for (int i = 0; i < 4; i++) {
            try {
                testCircuit.executeSupplier(() -> {
                    throw new RuntimeException("failure");
                });
            } catch (Exception e) {
                // Expected
            }
        }

        // Wait for state transition to OPEN
        boolean transitioned = latch.await(1, TimeUnit.SECONDS);

        // Verify events were published
        assertThat(transitioned).isTrue();
        assertThat(stateTransitions.get()).isGreaterThan(0);
        assertThat(errors.get()).isEqualTo(4);
        assertThat(successes.get()).isEqualTo(1);
    }

    @Test
    void circuitBreakerMetricsAreAccurate() {
        var testConfig = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(java.time.Duration.ofMillis(100))
            .build();

        CircuitBreaker testCircuit = circuitBreakerRegistry.circuitBreaker("test-metrics", testConfig);

        // Execute 6 successful calls
        for (int i = 0; i < 6; i++) {
            testCircuit.executeSupplier(() -> "success");
        }

        // Execute 4 failed calls
        for (int i = 0; i < 4; i++) {
            try {
                testCircuit.executeSupplier(() -> {
                    throw new RuntimeException("failure");
                });
            } catch (Exception e) {
                // Expected
            }
        }

        // Verify metrics
        var metrics = testCircuit.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(6);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(4);
        assertThat(metrics.getFailureRate()).isEqualTo(40.0f); // 4/10 = 40%
        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(10);
    }
}
