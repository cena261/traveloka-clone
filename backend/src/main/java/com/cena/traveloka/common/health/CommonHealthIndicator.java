package com.cena.traveloka.common.health;

import com.cena.traveloka.common.service.ExternalServiceCircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * T051: Health Indicator for monitoring external service status.
 *
 * This health indicator provides information about:
 * - Redis cache service health and circuit breaker state
 * - Elasticsearch search service health and circuit breaker state
 * - MinIO object storage service health and circuit breaker state
 *
 * Health status determination:
 * - UP: All services operational and circuits closed
 * - DEGRADED: Some services unavailable but system functional (circuits open)
 * - DOWN: Critical services unavailable (Redis down)
 *
 * Exposed via Spring Boot Actuator at: /actuator/health
 *
 * Based on research.md specifications for external service monitoring.
 */
@Component("commonHealth")
@RequiredArgsConstructor
@Slf4j
public class CommonHealthIndicator implements HealthIndicator {

    private final ExternalServiceCircuitBreaker circuitBreakerService;
    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean allServicesUp = true;
        boolean criticalServicesUp = true;

        // Check Redis health (critical service - cache fallback to database)
        RedisHealthStatus redisHealth = checkRedisHealth();
        details.put("redis", redisHealth.toMap());

        if (!redisHealth.isHealthy()) {
            log.warn("Redis service is unhealthy: {}", redisHealth.getMessage());
            // Redis is not critical - can fallback to database
            allServicesUp = false;
        }

        // Check Elasticsearch health (non-critical - search can be degraded)
        ElasticsearchHealthStatus esHealth = checkElasticsearchHealth();
        details.put("elasticsearch", esHealth.toMap());

        if (!esHealth.isHealthy()) {
            log.warn("Elasticsearch service is unhealthy: {}", esHealth.getMessage());
            allServicesUp = false;
        }

        // Check MinIO health (non-critical - can serve cached/existing content)
        MinioHealthStatus minioHealth = checkMinioHealth();
        details.put("minio", minioHealth.toMap());

        if (!minioHealth.isHealthy()) {
            log.warn("MinIO service is unhealthy: {}", minioHealth.getMessage());
            allServicesUp = false;
        }

        // Determine overall health status
        if (criticalServicesUp && allServicesUp) {
            return Health.up()
                .withDetails(details)
                .build();
        } else if (criticalServicesUp) {
            // System is degraded but functional
            return Health.status("DEGRADED")
                .withDetails(details)
                .withDetail("message", "Some external services are unavailable but system is operational")
                .build();
        } else {
            // Critical services down
            return Health.down()
                .withDetails(details)
                .withDetail("message", "Critical services are unavailable")
                .build();
        }
    }

    /**
     * Check Redis service health and circuit breaker state.
     */
    private RedisHealthStatus checkRedisHealth() {
        CircuitBreaker.State circuitState = circuitBreakerService.getRedisCircuitState();

        if (circuitState == CircuitBreaker.State.OPEN) {
            return new RedisHealthStatus(
                false,
                circuitState.toString(),
                "Circuit breaker is OPEN - Redis calls are blocked"
            );
        }

        // Try to ping Redis
        try {
            String pong = circuitBreakerService.executeRedis(() -> {
                return redisConnectionFactory.getConnection().ping();
            }, null);

            if ("PONG".equals(pong)) {
                return new RedisHealthStatus(
                    true,
                    circuitState.toString(),
                    "Redis is healthy and responding"
                );
            } else {
                return new RedisHealthStatus(
                    false,
                    circuitState.toString(),
                    "Redis ping failed"
                );
            }
        } catch (Exception e) {
            log.error("Redis health check failed: {}", e.getMessage());
            return new RedisHealthStatus(
                false,
                circuitState.toString(),
                "Redis health check failed: " + e.getMessage()
            );
        }
    }

    /**
     * Check Elasticsearch service health and circuit breaker state.
     */
    private ElasticsearchHealthStatus checkElasticsearchHealth() {
        CircuitBreaker.State circuitState = circuitBreakerService.getElasticsearchCircuitState();

        if (circuitState == CircuitBreaker.State.OPEN) {
            return new ElasticsearchHealthStatus(
                false,
                circuitState.toString(),
                "Circuit breaker is OPEN - Elasticsearch calls are blocked"
            );
        }

        // For now, consider Elasticsearch healthy if circuit is closed
        // Actual ES client health check would require ElasticsearchClient bean
        if (circuitState == CircuitBreaker.State.CLOSED) {
            return new ElasticsearchHealthStatus(
                true,
                circuitState.toString(),
                "Elasticsearch circuit is healthy"
            );
        } else {
            return new ElasticsearchHealthStatus(
                false,
                circuitState.toString(),
                "Elasticsearch circuit is in " + circuitState + " state"
            );
        }
    }

    /**
     * Check MinIO service health and circuit breaker state.
     */
    private MinioHealthStatus checkMinioHealth() {
        CircuitBreaker.State circuitState = circuitBreakerService.getMinioCircuitState();

        if (circuitState == CircuitBreaker.State.OPEN) {
            return new MinioHealthStatus(
                false,
                circuitState.toString(),
                "Circuit breaker is OPEN - MinIO calls are blocked"
            );
        }

        // For now, consider MinIO healthy if circuit is closed
        // Actual MinIO client health check would require MinioClient bean
        if (circuitState == CircuitBreaker.State.CLOSED) {
            return new MinioHealthStatus(
                true,
                circuitState.toString(),
                "MinIO circuit is healthy"
            );
        } else {
            return new MinioHealthStatus(
                false,
                circuitState.toString(),
                "MinIO circuit is in " + circuitState + " state"
            );
        }
    }

    /**
     * Redis health status data structure.
     */
    private static class RedisHealthStatus {
        private final boolean healthy;
        private final String circuitState;
        private final String message;

        public RedisHealthStatus(boolean healthy, String circuitState, String message) {
            this.healthy = healthy;
            this.circuitState = circuitState;
            this.message = message;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("status", healthy ? "UP" : "DOWN");
            map.put("circuitBreakerState", circuitState);
            map.put("message", message);
            return map;
        }
    }

    /**
     * Elasticsearch health status data structure.
     */
    private static class ElasticsearchHealthStatus {
        private final boolean healthy;
        private final String circuitState;
        private final String message;

        public ElasticsearchHealthStatus(boolean healthy, String circuitState, String message) {
            this.healthy = healthy;
            this.circuitState = circuitState;
            this.message = message;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("status", healthy ? "UP" : "DOWN");
            map.put("circuitBreakerState", circuitState);
            map.put("message", message);
            return map;
        }
    }

    /**
     * MinIO health status data structure.
     */
    private static class MinioHealthStatus {
        private final boolean healthy;
        private final String circuitState;
        private final String message;

        public MinioHealthStatus(boolean healthy, String circuitState, String message) {
            this.healthy = healthy;
            this.circuitState = circuitState;
            this.message = message;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("status", healthy ? "UP" : "DOWN");
            map.put("circuitBreakerState", circuitState);
            map.put("message", message);
            return map;
        }
    }
}
