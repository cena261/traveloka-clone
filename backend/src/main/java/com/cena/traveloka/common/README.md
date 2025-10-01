# Common Module - Shared Foundation Layer

## Overview

The Common Module provides shared foundation components for the Traveloka Clone backend application. This module contains cross-cutting concerns like configuration, exception handling, health monitoring, and circuit breaker patterns.

## Components Implemented

### 🔧 Configuration Classes

#### 1. **CircuitBreakerConfig** (`config/CircuitBreakerConfig.java`)
Resilience4j circuit breaker configuration for external services with failure thresholds and recovery mechanisms.

**Features:**
- Redis circuit breaker: 40% failure threshold, 30s wait time
- Elasticsearch circuit breaker: 50% failure threshold, 60s wait time
- MinIO circuit breaker: 60% failure threshold, 90s wait time
- Automatic state transition monitoring
- Event logging for debugging

**Usage:**
```java
@Autowired
private CircuitBreaker redisCircuitBreaker;

// Circuit breaker is automatically applied by ExternalServiceCircuitBreaker
```

#### 2. **AsyncConfig** (`config/AsyncConfig.java`)
Thread pool configuration for asynchronous task execution.

**Thread Pools:**
- Main async executor: core=10, max=50, queue=1000
- Email executor: core=2, max=10, queue=100
- Notification executor: core=3, max=15, queue=200
- Search executor: core=5, max=20, queue=500
- File executor: core=2, max=8, queue=50

**Usage:**
```java
@Async("taskExecutor")
public CompletableFuture<Void> processInBackground() {
    // Background task
    return CompletableFuture.completedFuture(null);
}
```

#### 3. **CorsConfig** (`config/CorsConfig.java`)
CORS configuration with environment-based allowed origins.

**Configuration:**
- Allowed origins from environment variable: `CORS_ALLOWED_ORIGINS`
- Default dev origins: `http://localhost:3000,http://localhost:3001,http://localhost:4200`
- Supports all HTTP methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
- Allows credentials

### 🛡️ Circuit Breaker Service

#### **ExternalServiceCircuitBreaker** (`service/ExternalServiceCircuitBreaker.java`)
Wrapper service for executing external service calls with circuit breaker protection.

**Methods:**
- `executeRedis(Supplier<T>, T fallback)` - Execute Redis operation with fallback
- `executeElasticsearch(Supplier<T>, T fallback)` - Execute Elasticsearch operation
- `executeMinio(Supplier<T>, T fallback)` - Execute MinIO operation
- `executeRedisVoid(Runnable)` - Execute Redis operation without return value
- Circuit state inspection methods

**Usage Example:**
```java
@Autowired
private ExternalServiceCircuitBreaker circuitBreakerService;

// Execute with automatic fallback
String value = circuitBreakerService.executeRedis(
    () -> redisTemplate.opsForValue().get(key),
    null // fallback value
);

// Check circuit state
if (circuitBreakerService.isRedisCircuitOpen()) {
    log.warn("Redis circuit is open, using database");
}
```

### 🏥 Health Monitoring

#### **CommonHealthIndicator** (`health/CommonHealthIndicator.java`)
Spring Boot Actuator health indicator for external services.

**Health Status:**
- **UP**: All services operational
- **DEGRADED**: Some services unavailable but system functional
- **DOWN**: Critical services unavailable

**Monitored Services:**
- Redis (cache)
- Elasticsearch (search)
- MinIO (object storage)

**Endpoint:** `/actuator/health`

**Response Example:**
```json
{
  "status": "UP",
  "components": {
    "commonHealth": {
      "status": "UP",
      "details": {
        "redis": {
          "status": "UP",
          "circuitBreakerState": "CLOSED",
          "message": "Redis is healthy and responding"
        },
        "elasticsearch": {
          "status": "UP",
          "circuitBreakerState": "CLOSED",
          "message": "Elasticsearch circuit is healthy"
        },
        "minio": {
          "status": "UP",
          "circuitBreakerState": "CLOSED",
          "message": "MinIO circuit is healthy"
        }
      }
    }
  }
}
```

## Configuration Files

### **logback-spring.xml**
Structured logging configuration with:
- Console appender with color-coded output
- Rolling file appenders (100MB files, 30-day retention)
- Separate error log file (50MB files, 90-day retention)
- Async appenders for performance
- Profile-specific logging levels (dev/test/prod)

### **application-dev.yml**
Development environment configuration with:
- HikariCP connection pool (min=5, max=20)
- Redis configuration
- MinIO configuration
- Keycloak OAuth2 settings
- Database settings

## Architecture Patterns

### Circuit Breaker Pattern

The circuit breaker pattern prevents cascading failures:

1. **CLOSED**: Normal operation, requests pass through
2. **OPEN**: Failure threshold exceeded, requests blocked
3. **HALF_OPEN**: Testing recovery, limited requests allowed

**State Transitions:**
```
CLOSED --[failures > threshold]--> OPEN
OPEN --[wait duration]--> HALF_OPEN
HALF_OPEN --[success]--> CLOSED
HALF_OPEN --[failure]--> OPEN
```

### Health Check Pattern

Health indicators provide:
- Real-time service status
- Circuit breaker states
- Graceful degradation information
- Detailed error messages

## Dependencies

### Required
- Spring Boot 3.5.5
- Resilience4j 2.2.0
- Spring Boot Actuator
- Redis (Jedis client)
- Logback

### Optional
- Elasticsearch 8.x (for search features)
- MinIO (for object storage)

## Testing

### Integration Tests

All components include integration tests using TestContainers:

- `CircuitBreakerConfigTest` - Circuit breaker configuration and behavior
- `CommonHealthIndicatorTest` - Health indicator responses
- `DatabaseConfigIntegrationTest` - HikariCP connection pool
- `RedisCacheIntegrationTest` - Redis operations
- `ElasticsearchIntegrationTest` - Elasticsearch client
- `MinIOIntegrationTest` - MinIO client

**Run tests:**
```bash
mvn test -Dtest="com.cena.traveloka.common.**"
```

## Configuration Properties

### Circuit Breaker Settings

**Redis** (Fast cache operations):
```yaml
resilience4j:
  circuitbreaker:
    instances:
      redis:
        slidingWindowSize: 5
        minimumNumberOfCalls: 3
        failureRateThreshold: 40
        slowCallDurationThreshold: 500ms
        waitDurationInOpenState: 30s
```

**Elasticsearch** (Search operations):
```yaml
resilience4j:
  circuitbreaker:
    instances:
      elasticsearch:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        slowCallDurationThreshold: 3s
        waitDurationInOpenState: 60s
```

**MinIO** (File operations):
```yaml
resilience4j:
  circuitbreaker:
    instances:
      minio:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 60
        slowCallDurationThreshold: 10s
        waitDurationInOpenState: 90s
```

### Async Executor Settings

```yaml
spring:
  task:
    execution:
      pool:
        core-size: 10
        max-size: 50
        queue-capacity: 1000
      thread-name-prefix: "AsyncExecutor-"
```

## Best Practices

### 1. Using Circuit Breakers

**DO:**
```java
// Use the wrapper service
String result = circuitBreakerService.executeRedis(
    () -> redisTemplate.opsForValue().get(key),
    defaultValue
);
```

**DON'T:**
```java
// Direct access without circuit breaker
String result = redisTemplate.opsForValue().get(key); // ❌ No protection
```

### 2. Health Check Monitoring

Monitor health endpoint regularly:
```bash
# Check application health
curl http://localhost:8080/api/v1/actuator/health

# Check specific component
curl http://localhost:8080/api/v1/actuator/health/commonHealth
```

### 3. Async Processing

Use appropriate executor for task type:
```java
@Async("emailTaskExecutor")
public CompletableFuture<Void> sendEmail() { ... }

@Async("searchTaskExecutor")
public CompletableFuture<List<Result>> searchAsync() { ... }

@Async("fileTaskExecutor")
public CompletableFuture<String> uploadFile() { ... }
```

## Troubleshooting

### Circuit Breaker Issues

**Problem:** Circuit breaker stuck in OPEN state

**Solution:**
```java
// Check circuit state
CircuitBreaker.State state = circuitBreakerService.getRedisCircuitState();

// Manual reset (use with caution)
if (state == CircuitBreaker.State.OPEN) {
    circuitBreakerService.resetRedisCircuit();
}
```

### Health Check Failures

**Problem:** Health endpoint shows DEGRADED

**Solution:**
1. Check circuit breaker states in health response
2. Verify external service connectivity
3. Review logs for specific error messages
4. Check if services are actually down or just slow

### Async Executor Exhaustion

**Problem:** Tasks queuing up or being rejected

**Solution:**
1. Monitor thread pool metrics
2. Increase pool size if needed
3. Check for slow/blocking operations in async methods
4. Consider using different executors for different task types

## Migration Notes

### From Older Versions

If migrating from an older version:

1. Update Hibernate dialect:
   ```yaml
   # Old (deprecated)
   database-platform: org.hibernate.spatial.dialect.postgis.PostgisPG95Dialect

   # New
   database-platform: org.hibernate.dialect.PostgreSQLDialect
   ```

2. Update Logback rolling policy:
   ```xml
   <!-- Old (deprecated) -->
   <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
     <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">

   <!-- New -->
   <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
   ```

## Contributing

When adding new components to the Common Module:

1. **Follow existing patterns** - Use similar structure to existing configs
2. **Add tests** - All components must have integration tests
3. **Document thoroughly** - Add JavaDoc and update this README
4. **Consider impact** - Changes affect all business modules
5. **Review with team** - Common module changes require approval

## Support

For issues or questions:
- Check logs in `logs/traveloka-backend.log`
- Review health endpoint: `/actuator/health`
- Check circuit breaker metrics
- Consult team documentation

## License

Internal use only - Traveloka Clone Project
