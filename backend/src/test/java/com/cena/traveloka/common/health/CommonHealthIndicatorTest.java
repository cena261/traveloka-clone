package com.cena.traveloka.common.health;

import com.cena.traveloka.common.service.ExternalServiceCircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * T052: Test for CommonHealthIndicator to verify correct responses to service states.
 *
 * This test verifies:
 * - Health indicator returns UP when all services are healthy
 * - Health indicator returns DEGRADED when non-critical services fail
 * - Health indicator returns DOWN when critical services fail
 * - Health indicator detects circuit breaker OPEN states
 * - Health indicator includes detailed status for each service
 * - Redis ping check works correctly
 * - Circuit breaker states are properly reported
 */
@ExtendWith(MockitoExtension.class)
class CommonHealthIndicatorTest {

    @Mock
    private ExternalServiceCircuitBreaker circuitBreakerService;

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @Mock
    private RedisConnection redisConnection;

    private CommonHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new CommonHealthIndicator(circuitBreakerService, redisConnectionFactory);
    }

    @Test
    void healthReturnsUpWhenAllServicesHealthy() {
        // Given: All circuit breakers are CLOSED and Redis is responding
        when(circuitBreakerService.getRedisCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getElasticsearchCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getMinioCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(circuitBreakerService.executeRedis(any(), any())).thenReturn("PONG");

        // When: Health check is performed
        Health health = healthIndicator.health();

        // Then: Overall status should be UP
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKeys("redis", "elasticsearch", "minio");
    }

    @Test
    void healthReturnsDegradedWhenElasticsearchCircuitOpen() {
        // Given: Elasticsearch circuit is OPEN, others are healthy
        when(circuitBreakerService.getRedisCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getElasticsearchCircuitState()).thenReturn(CircuitBreaker.State.OPEN);
        when(circuitBreakerService.getMinioCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(circuitBreakerService.executeRedis(any(), any())).thenReturn("PONG");

        // When: Health check is performed
        Health health = healthIndicator.health();

        // Then: Overall status should be DEGRADED
        assertThat(health.getStatus()).isEqualTo(new Status("DEGRADED"));
        assertThat(health.getDetails()).containsKey("message");
        assertThat(health.getDetails().get("message").toString())
            .contains("Some external services are unavailable");

        // Verify Elasticsearch details show it's down
        @SuppressWarnings("unchecked")
        var esDetails = (java.util.Map<String, Object>) health.getDetails().get("elasticsearch");
        assertThat(esDetails.get("status")).isEqualTo("DOWN");
        assertThat(esDetails.get("circuitBreakerState")).isEqualTo("OPEN");
    }

    @Test
    void healthReturnsDegradedWhenMinioCircuitOpen() {
        // Given: MinIO circuit is OPEN, others are healthy
        when(circuitBreakerService.getRedisCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getElasticsearchCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getMinioCircuitState()).thenReturn(CircuitBreaker.State.OPEN);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(circuitBreakerService.executeRedis(any(), any())).thenReturn("PONG");

        // When: Health check is performed
        Health health = healthIndicator.health();

        // Then: Overall status should be DEGRADED
        assertThat(health.getStatus()).isEqualTo(new Status("DEGRADED"));

        // Verify MinIO details show it's down
        @SuppressWarnings("unchecked")
        var minioDetails = (java.util.Map<String, Object>) health.getDetails().get("minio");
        assertThat(minioDetails.get("status")).isEqualTo("DOWN");
        assertThat(minioDetails.get("circuitBreakerState")).isEqualTo("OPEN");
    }

    @Test
    void healthReturnsDegradedWhenRedisCircuitOpen() {
        // Given: Redis circuit is OPEN (non-critical since can fallback to DB)
        when(circuitBreakerService.getRedisCircuitState()).thenReturn(CircuitBreaker.State.OPEN);
        when(circuitBreakerService.getElasticsearchCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getMinioCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);

        // When: Health check is performed
        Health health = healthIndicator.health();

        // Then: Overall status should be DEGRADED (Redis down but system functional)
        assertThat(health.getStatus()).isEqualTo(new Status("DEGRADED"));

        // Verify Redis details show it's down
        @SuppressWarnings("unchecked")
        var redisDetails = (java.util.Map<String, Object>) health.getDetails().get("redis");
        assertThat(redisDetails.get("status")).isEqualTo("DOWN");
        assertThat(redisDetails.get("circuitBreakerState")).isEqualTo("OPEN");
        assertThat(redisDetails.get("message").toString())
            .contains("Circuit breaker is OPEN");
    }

    @Test
    void healthReturnsDegradedWhenMultipleServicesDown() {
        // Given: Both Elasticsearch and MinIO circuits are OPEN
        when(circuitBreakerService.getRedisCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getElasticsearchCircuitState()).thenReturn(CircuitBreaker.State.OPEN);
        when(circuitBreakerService.getMinioCircuitState()).thenReturn(CircuitBreaker.State.OPEN);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(circuitBreakerService.executeRedis(any(), any())).thenReturn("PONG");

        // When: Health check is performed
        Health health = healthIndicator.health();

        // Then: Overall status should be DEGRADED
        assertThat(health.getStatus()).isEqualTo(new Status("DEGRADED"));

        // Verify both services show as down
        @SuppressWarnings("unchecked")
        var esDetails = (java.util.Map<String, Object>) health.getDetails().get("elasticsearch");
        assertThat(esDetails.get("status")).isEqualTo("DOWN");

        @SuppressWarnings("unchecked")
        var minioDetails = (java.util.Map<String, Object>) health.getDetails().get("minio");
        assertThat(minioDetails.get("status")).isEqualTo("DOWN");
    }

    @Test
    void healthIncludesRedisDetailsWhenHealthy() {
        // Given: Redis is healthy
        when(circuitBreakerService.getRedisCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getElasticsearchCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getMinioCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(circuitBreakerService.executeRedis(any(), any())).thenReturn("PONG");

        // When: Health check is performed
        Health health = healthIndicator.health();

        // Then: Redis details should show healthy status
        @SuppressWarnings("unchecked")
        var redisDetails = (java.util.Map<String, Object>) health.getDetails().get("redis");
        assertThat(redisDetails.get("status")).isEqualTo("UP");
        assertThat(redisDetails.get("circuitBreakerState")).isEqualTo("CLOSED");
        assertThat(redisDetails.get("message").toString())
            .contains("Redis is healthy");
    }

    @Test
    void healthIncludesElasticsearchDetailsWhenHealthy() {
        // Given: Elasticsearch is healthy
        when(circuitBreakerService.getRedisCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getElasticsearchCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getMinioCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(circuitBreakerService.executeRedis(any(), any())).thenReturn("PONG");

        // When: Health check is performed
        Health health = healthIndicator.health();

        // Then: Elasticsearch details should show healthy status
        @SuppressWarnings("unchecked")
        var esDetails = (java.util.Map<String, Object>) health.getDetails().get("elasticsearch");
        assertThat(esDetails.get("status")).isEqualTo("UP");
        assertThat(esDetails.get("circuitBreakerState")).isEqualTo("CLOSED");
        assertThat(esDetails.get("message").toString())
            .contains("Elasticsearch circuit is healthy");
    }

    @Test
    void healthIncludesMinioDetailsWhenHealthy() {
        // Given: MinIO is healthy
        when(circuitBreakerService.getRedisCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getElasticsearchCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getMinioCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(circuitBreakerService.executeRedis(any(), any())).thenReturn("PONG");

        // When: Health check is performed
        Health health = healthIndicator.health();

        // Then: MinIO details should show healthy status
        @SuppressWarnings("unchecked")
        var minioDetails = (java.util.Map<String, Object>) health.getDetails().get("minio");
        assertThat(minioDetails.get("status")).isEqualTo("UP");
        assertThat(minioDetails.get("circuitBreakerState")).isEqualTo("CLOSED");
        assertThat(minioDetails.get("message").toString())
            .contains("MinIO circuit is healthy");
    }

    @Test
    void healthHandlesRedisConnectionFailure() {
        // Given: Redis connection fails
        when(circuitBreakerService.getRedisCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getElasticsearchCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getMinioCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.executeRedis(any(), any())).thenReturn(null); // Simulate failure

        // When: Health check is performed
        Health health = healthIndicator.health();

        // Then: Should return DEGRADED status
        assertThat(health.getStatus()).isEqualTo(new Status("DEGRADED"));

        // Verify Redis details show failure
        @SuppressWarnings("unchecked")
        var redisDetails = (java.util.Map<String, Object>) health.getDetails().get("redis");
        assertThat(redisDetails.get("status")).isEqualTo("DOWN");
        assertThat(redisDetails.get("message").toString())
            .contains("Redis ping failed");
    }

    @Test
    void healthReportsHalfOpenCircuitState() {
        // Given: Elasticsearch circuit is in HALF_OPEN state
        when(circuitBreakerService.getRedisCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getElasticsearchCircuitState()).thenReturn(CircuitBreaker.State.HALF_OPEN);
        when(circuitBreakerService.getMinioCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(circuitBreakerService.executeRedis(any(), any())).thenReturn("PONG");

        // When: Health check is performed
        Health health = healthIndicator.health();

        // Then: Should report HALF_OPEN state
        @SuppressWarnings("unchecked")
        var esDetails = (java.util.Map<String, Object>) health.getDetails().get("elasticsearch");
        assertThat(esDetails.get("circuitBreakerState")).isEqualTo("HALF_OPEN");
        assertThat(esDetails.get("status")).isEqualTo("DOWN");
        assertThat(esDetails.get("message").toString())
            .contains("HALF_OPEN");
    }

    @Test
    void healthDetailsContainAllRequiredFields() {
        // Given: All services healthy
        when(circuitBreakerService.getRedisCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getElasticsearchCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getMinioCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(circuitBreakerService.executeRedis(any(), any())).thenReturn("PONG");

        // When: Health check is performed
        Health health = healthIndicator.health();

        // Then: Each service detail should have required fields
        @SuppressWarnings("unchecked")
        var redisDetails = (java.util.Map<String, Object>) health.getDetails().get("redis");
        assertThat(redisDetails).containsKeys("status", "circuitBreakerState", "message");

        @SuppressWarnings("unchecked")
        var esDetails = (java.util.Map<String, Object>) health.getDetails().get("elasticsearch");
        assertThat(esDetails).containsKeys("status", "circuitBreakerState", "message");

        @SuppressWarnings("unchecked")
        var minioDetails = (java.util.Map<String, Object>) health.getDetails().get("minio");
        assertThat(minioDetails).containsKeys("status", "circuitBreakerState", "message");
    }

    @Test
    void healthStatusMessagesAreDescriptive() {
        // Given: Mixed service states
        when(circuitBreakerService.getRedisCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreakerService.getElasticsearchCircuitState()).thenReturn(CircuitBreaker.State.OPEN);
        when(circuitBreakerService.getMinioCircuitState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(circuitBreakerService.executeRedis(any(), any())).thenReturn("PONG");

        // When: Health check is performed
        Health health = healthIndicator.health();

        // Then: Messages should be descriptive
        @SuppressWarnings("unchecked")
        var redisDetails = (java.util.Map<String, Object>) health.getDetails().get("redis");
        assertThat(redisDetails.get("message")).isNotNull();
        assertThat(redisDetails.get("message").toString()).isNotBlank();

        @SuppressWarnings("unchecked")
        var esDetails = (java.util.Map<String, Object>) health.getDetails().get("elasticsearch");
        assertThat(esDetails.get("message")).isNotNull();
        assertThat(esDetails.get("message").toString()).isNotBlank();
        assertThat(esDetails.get("message").toString())
            .contains("Circuit breaker is OPEN");
    }
}
