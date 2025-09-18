package com.cena.traveloka.search.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@Jacksonized
public class RateLimitConfig {

    // Configuration metadata
    private String configId;
    private String configName;
    private String configVersion;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastModified;
    private Boolean isActive;

    // Rate limiting rules
    private List<RateLimitRule> rateLimitRules;

    // Throttling configuration
    private ThrottlingConfig throttlingConfig;

    // Quota management
    private List<QuotaConfig> quotaConfigs;

    // Burst handling
    private BurstConfig burstConfig;

    // Adaptive rate limiting
    private AdaptiveConfig adaptiveConfig;

    // Circuit breaker configuration
    private CircuitBreakerConfig circuitBreakerConfig;

    @Data
    @Builder
    @Jacksonized
    public static class RateLimitRule {
        private String ruleId;
        private String ruleName;
        private String description;
        private RateLimitScope scope;
        private RateLimitType type;
        private Integer requestLimit;
        private Long timeWindowSeconds;
        private String timeWindowUnit; // SECOND, MINUTE, HOUR, DAY
        private Integer priority; // Higher priority rules are evaluated first
        private List<String> applicableEndpoints;
        private List<String> exemptUsers;
        private List<String> exemptIPs;
        private Map<String, Object> conditions;
        private PenaltyAction penaltyAction;
        private Boolean isEnabled;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ThrottlingConfig {
        private Boolean enableThrottling;
        private ThrottlingStrategy strategy;
        private Integer maxConcurrentRequests;
        private Long requestDelayMs;
        private Integer queueSize;
        private Long queueTimeoutMs;
        private Boolean enablePrioritization;
        private Map<String, Integer> priorityLevels;
        private String overflowAction; // reject, queue, delay
    }

    @Data
    @Builder
    @Jacksonized
    public static class QuotaConfig {
        private String quotaId;
        private String quotaName;
        private QuotaScope scope;
        private Long quotaLimit;
        private String quotaUnit; // requests, bandwidth, cpu_time
        private String resetPeriod; // DAILY, WEEKLY, MONTHLY
        private OffsetDateTime resetTime;
        private Boolean enableOverage;
        private Integer overageLimit;
        private String overageAction; // charge, warn, block
        private List<String> applicableUserTiers;
    }

    @Data
    @Builder
    @Jacksonized
    public static class BurstConfig {
        private Boolean enableBurstHandling;
        private Integer burstLimit;
        private Long burstWindowSeconds;
        private Integer burstTokenRefillRate;
        private String burstStrategy; // token_bucket, leaky_bucket, sliding_window
        private Long burstCooldownMs;
        private Boolean enableBurstDetection;
        private Double burstThreshold;
    }

    @Data
    @Builder
    @Jacksonized
    public static class AdaptiveConfig {
        private Boolean enableAdaptiveRateLimiting;
        private String adaptationStrategy; // load_based, response_time_based, error_rate_based
        private Map<String, Object> adaptationTriggers;
        private Integer adaptationIntervalSeconds;
        private Double scalingFactor; // How much to scale limits (0.5 = 50%, 2.0 = 200%)
        private Integer minRateLimit;
        private Integer maxRateLimit;
        private Boolean enablePredictiveScaling;
        private Map<String, Object> predictionModel;
    }

    @Data
    @Builder
    @Jacksonized
    public static class CircuitBreakerConfig {
        private Boolean enableCircuitBreaker;
        private Integer failureThreshold;
        private Long timeWindowMs;
        private Long retryTimeoutMs;
        private String failureCondition; // error_rate, response_time, availability
        private Double failureThresholdPercentage;
        private Integer minimumRequests;
        private String circuitBreakerState; // CLOSED, OPEN, HALF_OPEN
    }

    @Data
    @Builder
    @Jacksonized
    public static class PenaltyAction {
        private String actionType; // warn, throttle, block, captcha
        private Long penaltyDurationSeconds;
        private String penaltySeverity; // light, moderate, severe
        private Boolean escalateOnRepeat;
        private Map<String, Object> actionParameters;
        private String notificationTemplate;
    }

    public enum RateLimitScope {
        GLOBAL,           // Apply to all requests
        PER_IP,          // Apply per IP address
        PER_USER,        // Apply per authenticated user
        PER_SESSION,     // Apply per session
        PER_API_KEY,     // Apply per API key
        PER_ENDPOINT,    // Apply per endpoint
        PER_USER_AGENT,  // Apply per user agent
        CUSTOM           // Custom scope based on rules
    }

    public enum RateLimitType {
        FIXED_WINDOW,    // Fixed time window rate limiting
        SLIDING_WINDOW,  // Sliding time window rate limiting
        TOKEN_BUCKET,    // Token bucket algorithm
        LEAKY_BUCKET,    // Leaky bucket algorithm
        ADAPTIVE,        // Adaptive rate limiting
        WEIGHTED         // Weighted rate limiting based on request cost
    }

    public enum QuotaScope {
        USER,
        ORGANIZATION,
        API_KEY,
        SUBSCRIPTION_TIER,
        GLOBAL
    }

    public enum ThrottlingStrategy {
        IMMEDIATE_REJECT,  // Reject requests immediately when limit exceeded
        QUEUE_AND_DELAY,   // Queue requests and process with delay
        PRIORITY_BASED,    // Process based on request priority
        ADAPTIVE_DELAY,    // Adaptive delay based on load
        CIRCUIT_BREAKER   // Use circuit breaker pattern
    }

    // Rate limit status tracking
    @Data
    @Builder
    @Jacksonized
    public static class RateLimitStatus {
        private String identifier; // IP, user ID, etc.
        private String scope;
        private Integer currentCount;
        private Integer limit;
        private Long windowStartTime;
        private Long windowEndTime;
        private Long resetTime;
        private String status; // OK, WARNING, EXCEEDED, BLOCKED
        private List<String> appliedRules;
    }

    // Rate limit response information
    @Data
    @Builder
    @Jacksonized
    public static class RateLimitResponse {
        private Boolean isAllowed;
        private Integer remainingRequests;
        private Long resetTime;
        private Long retryAfter;
        private String limitType;
        private String limitScope;
        private Map<String, String> responseHeaders;
        private String blockReason;
        private List<String> appliedRules;
    }

    // Performance and monitoring
    @Data
    @Builder
    @Jacksonized
    public static class RateLimitMetrics {
        private Long totalRequests;
        private Long allowedRequests;
        private Long blockedRequests;
        private Long throttledRequests;
        private Double blockRate;
        private Double throttleRate;
        private Map<String, Long> requestsByScope;
        private Map<String, Long> requestsByEndpoint;
        private OffsetDateTime lastUpdated;
    }
}