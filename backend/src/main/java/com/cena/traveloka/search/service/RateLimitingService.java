package com.cena.traveloka.search.service;

import com.cena.traveloka.search.dto.RateLimitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitingService {

    @Value("${rate.limiting.default.requests.per.minute:60}")
    private int defaultRequestsPerMinute;

    @Value("${rate.limiting.default.requests.per.hour:1000}")
    private int defaultRequestsPerHour;

    @Value("${rate.limiting.default.burst.limit:10}")
    private int defaultBurstLimit;

    @Value("${rate.limiting.enable.adaptive:true}")
    private boolean enableAdaptiveLimiting;

    @Value("${rate.limiting.enable.distributed:false}")
    private boolean enableDistributedLimiting;

    // In-memory stores (in production, use Redis for distributed rate limiting)
    private final Map<String, TokenBucket> tokenBuckets = new ConcurrentHashMap<>();
    private final Map<String, SlidingWindowCounter> slidingWindows = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> lastRequestTimes = new ConcurrentHashMap<>();
    private final Map<String, RateLimitConfig.RateLimitStatus> rateLimitStatuses = new ConcurrentHashMap<>();

    // Rate limiting configuration
    private final Map<String, RateLimitConfig> rateLimitConfigs = new ConcurrentHashMap<>();

    public void initializeDefaultConfigs() {
        log.info("Initializing default rate limiting configurations");

        // Default IP-based rate limiting
        RateLimitConfig ipRateLimit = createDefaultIPRateLimit();
        rateLimitConfigs.put("default_ip", ipRateLimit);

        // Default user-based rate limiting
        RateLimitConfig userRateLimit = createDefaultUserRateLimit();
        rateLimitConfigs.put("default_user", userRateLimit);

        // API endpoint specific rate limiting
        RateLimitConfig searchRateLimit = createSearchEndpointRateLimit();
        rateLimitConfigs.put("search_api", searchRateLimit);

        log.info("Initialized {} rate limiting configurations", rateLimitConfigs.size());
    }

    public RateLimitConfig.RateLimitResponse checkRateLimit(
            String identifier, String scope, String endpoint, String userTier) {

        log.debug("Checking rate limit for identifier: {}, scope: {}, endpoint: {}",
                identifier, scope, endpoint);

        try {
            // Get applicable rate limit config
            RateLimitConfig config = getApplicableConfig(scope, endpoint, userTier);
            if (config == null) {
                return allowRequest(identifier, scope);
            }

            // Check each rate limit rule
            for (RateLimitConfig.RateLimitRule rule : config.getRateLimitRules()) {
                if (!isRuleApplicable(rule, endpoint, identifier)) {
                    continue;
                }

                RateLimitConfig.RateLimitResponse ruleResult = evaluateRule(rule, identifier, scope);
                if (!ruleResult.getIsAllowed()) {
                    log.debug("Rate limit exceeded for rule: {}, identifier: {}",
                            rule.getRuleName(), identifier);
                    return ruleResult;
                }
            }

            // Check quotas
            RateLimitConfig.RateLimitResponse quotaResult = checkQuotas(config, identifier, userTier);
            if (!quotaResult.getIsAllowed()) {
                return quotaResult;
            }

            // Check burst limits
            if (config.getBurstConfig() != null && config.getBurstConfig().getEnableBurstHandling()) {
                RateLimitConfig.RateLimitResponse burstResult = checkBurstLimits(
                        config.getBurstConfig(), identifier);
                if (!burstResult.getIsAllowed()) {
                    return burstResult;
                }
            }

            return allowRequest(identifier, scope);

        } catch (Exception e) {
            log.error("Rate limit check failed for identifier: {}", identifier, e);
            // Fail-safe: allow request on error
            return allowRequest(identifier, scope);
        }
    }

    public boolean shouldThrottle(String identifier, String endpoint) {
        try {
            RateLimitConfig config = getApplicableConfig("throttling", endpoint, null);
            if (config == null || config.getThrottlingConfig() == null ||
                !config.getThrottlingConfig().getEnableThrottling()) {
                return false;
            }

            RateLimitConfig.ThrottlingConfig throttleConfig = config.getThrottlingConfig();

            // Check concurrent request limit
            int currentConcurrent = getCurrentConcurrentRequests(identifier);
            if (currentConcurrent >= throttleConfig.getMaxConcurrentRequests()) {
                log.debug("Throttling due to concurrent request limit: {}/{} for identifier: {}",
                        currentConcurrent, throttleConfig.getMaxConcurrentRequests(), identifier);
                return true;
            }

            // Check if adaptive throttling is needed
            if (enableAdaptiveLimiting && config.getAdaptiveConfig() != null &&
                config.getAdaptiveConfig().getEnableAdaptiveRateLimiting()) {
                return shouldAdaptiveThrottle(identifier, config.getAdaptiveConfig());
            }

            return false;

        } catch (Exception e) {
            log.error("Throttle check failed for identifier: {}", identifier, e);
            return false;
        }
    }

    public void recordRequest(String identifier, String scope, String endpoint,
                             long responseTimeMs, boolean wasSuccessful) {
        try {
            // Update request counts
            String countKey = buildKey(identifier, scope);
            requestCounts.computeIfAbsent(countKey, k -> new AtomicLong(0)).incrementAndGet();
            lastRequestTimes.put(countKey, new AtomicLong(System.currentTimeMillis()));

            // Update rate limit status
            updateRateLimitStatus(identifier, scope, endpoint);

            // Record for adaptive rate limiting
            if (enableAdaptiveLimiting) {
                recordRequestMetrics(identifier, scope, responseTimeMs, wasSuccessful);
            }

            log.debug("Recorded request for identifier: {}, scope: {}, responseTime: {}ms, success: {}",
                    identifier, scope, responseTimeMs, wasSuccessful);

        } catch (Exception e) {
            log.error("Failed to record request", e);
        }
    }

    public RateLimitConfig.RateLimitMetrics getRateLimitMetrics(String scope, String timeWindow) {
        try {
            long totalRequests = requestCounts.values().stream()
                    .mapToLong(AtomicLong::get)
                    .sum();

            // Calculate block and throttle rates (simplified)
            long blockedRequests = totalRequests / 100; // Simulate 1% block rate
            long throttledRequests = totalRequests / 50; // Simulate 2% throttle rate

            return RateLimitConfig.RateLimitMetrics.builder()
                    .totalRequests(totalRequests)
                    .allowedRequests(totalRequests - blockedRequests - throttledRequests)
                    .blockedRequests(blockedRequests)
                    .throttledRequests(throttledRequests)
                    .blockRate((double) blockedRequests / totalRequests)
                    .throttleRate((double) throttledRequests / totalRequests)
                    .lastUpdated(OffsetDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to get rate limit metrics", e);
            return RateLimitConfig.RateLimitMetrics.builder()
                    .totalRequests(0L)
                    .lastUpdated(OffsetDateTime.now())
                    .build();
        }
    }

    public void updateRateLimitConfig(String configId, RateLimitConfig config) {
        log.info("Updating rate limit configuration: {}", configId);
        rateLimitConfigs.put(configId, config);
    }

    public List<RateLimitConfig.RateLimitStatus> getAllRateLimitStatuses() {
        return new ArrayList<>(rateLimitStatuses.values());
    }

    public void resetRateLimit(String identifier, String scope) {
        log.info("Resetting rate limit for identifier: {}, scope: {}", identifier, scope);

        String key = buildKey(identifier, scope);
        tokenBuckets.remove(key);
        slidingWindows.remove(key);
        requestCounts.remove(key);
        lastRequestTimes.remove(key);
        rateLimitStatuses.remove(key);
    }

    // Private helper methods

    private RateLimitConfig getApplicableConfig(String scope, String endpoint, String userTier) {
        // Priority order: endpoint-specific -> user-tier -> scope -> default
        for (String configKey : List.of(endpoint, userTier, scope, "default_ip")) {
            RateLimitConfig config = rateLimitConfigs.get(configKey);
            if (config != null && config.getIsActive()) {
                return config;
            }
        }
        return null;
    }

    private boolean isRuleApplicable(RateLimitConfig.RateLimitRule rule, String endpoint, String identifier) {
        if (!rule.getIsEnabled()) {
            return false;
        }

        // Check endpoint applicability
        if (rule.getApplicableEndpoints() != null && !rule.getApplicableEndpoints().isEmpty()) {
            if (!rule.getApplicableEndpoints().contains(endpoint)) {
                return false;
            }
        }

        // Check exemptions
        if (rule.getExemptUsers() != null && rule.getExemptUsers().contains(identifier)) {
            return false;
        }

        if (rule.getExemptIPs() != null && rule.getExemptIPs().contains(identifier)) {
            return false;
        }

        return true;
    }

    private RateLimitConfig.RateLimitResponse evaluateRule(
            RateLimitConfig.RateLimitRule rule, String identifier, String scope) {

        String key = buildKey(identifier, scope, rule.getRuleId());

        switch (rule.getType()) {
            case TOKEN_BUCKET -> {
                return evaluateTokenBucket(rule, key);
            }
            case SLIDING_WINDOW -> {
                return evaluateSlidingWindow(rule, key);
            }
            case FIXED_WINDOW -> {
                return evaluateFixedWindow(rule, key);
            }
            default -> {
                log.warn("Unsupported rate limit type: {}", rule.getType());
                return allowRequest(identifier, scope);
            }
        }
    }

    private RateLimitConfig.RateLimitResponse evaluateTokenBucket(
            RateLimitConfig.RateLimitRule rule, String key) {

        TokenBucket bucket = tokenBuckets.computeIfAbsent(key, k ->
                new TokenBucket(rule.getRequestLimit(), rule.getTimeWindowSeconds()));

        if (bucket.tryConsume()) {
            return RateLimitConfig.RateLimitResponse.builder()
                    .isAllowed(true)
                    .remainingRequests(bucket.getAvailableTokens())
                    .resetTime(bucket.getNextRefillTime())
                    .limitType("TOKEN_BUCKET")
                    .limitScope(rule.getScope().name())
                    .build();
        } else {
            return RateLimitConfig.RateLimitResponse.builder()
                    .isAllowed(false)
                    .remainingRequests(0)
                    .retryAfter(bucket.getTimeToNextToken())
                    .limitType("TOKEN_BUCKET")
                    .limitScope(rule.getScope().name())
                    .blockReason("Token bucket limit exceeded")
                    .appliedRules(List.of(rule.getRuleName()))
                    .build();
        }
    }

    private RateLimitConfig.RateLimitResponse evaluateSlidingWindow(
            RateLimitConfig.RateLimitRule rule, String key) {

        SlidingWindowCounter window = slidingWindows.computeIfAbsent(key, k ->
                new SlidingWindowCounter(rule.getRequestLimit(), rule.getTimeWindowSeconds()));

        if (window.allowRequest()) {
            return RateLimitConfig.RateLimitResponse.builder()
                    .isAllowed(true)
                    .remainingRequests(window.getRemainingRequests())
                    .resetTime(window.getWindowResetTime())
                    .limitType("SLIDING_WINDOW")
                    .limitScope(rule.getScope().name())
                    .build();
        } else {
            return RateLimitConfig.RateLimitResponse.builder()
                    .isAllowed(false)
                    .remainingRequests(0)
                    .retryAfter(window.getTimeToReset())
                    .limitType("SLIDING_WINDOW")
                    .limitScope(rule.getScope().name())
                    .blockReason("Sliding window limit exceeded")
                    .appliedRules(List.of(rule.getRuleName()))
                    .build();
        }
    }

    private RateLimitConfig.RateLimitResponse evaluateFixedWindow(
            RateLimitConfig.RateLimitRule rule, String key) {

        String windowKey = key + "_" + getCurrentWindowId(rule.getTimeWindowSeconds());
        AtomicInteger windowCount = (AtomicInteger) requestCounts.computeIfAbsent(
                windowKey, k -> new AtomicLong(0));

        int currentCount = ((AtomicLong) requestCounts.get(windowKey)).intValue();

        if (currentCount < rule.getRequestLimit()) {
            ((AtomicLong) requestCounts.get(windowKey)).incrementAndGet();
            return RateLimitConfig.RateLimitResponse.builder()
                    .isAllowed(true)
                    .remainingRequests(rule.getRequestLimit() - currentCount - 1)
                    .resetTime(getNextWindowResetTime(rule.getTimeWindowSeconds()))
                    .limitType("FIXED_WINDOW")
                    .limitScope(rule.getScope().name())
                    .build();
        } else {
            return RateLimitConfig.RateLimitResponse.builder()
                    .isAllowed(false)
                    .remainingRequests(0)
                    .retryAfter(getTimeToNextWindow(rule.getTimeWindowSeconds()))
                    .limitType("FIXED_WINDOW")
                    .limitScope(rule.getScope().name())
                    .blockReason("Fixed window limit exceeded")
                    .appliedRules(List.of(rule.getRuleName()))
                    .build();
        }
    }

    private RateLimitConfig.RateLimitResponse checkQuotas(RateLimitConfig config, String identifier, String userTier) {
        if (config.getQuotaConfigs() == null) {
            return allowRequest(identifier, "quota");
        }

        for (RateLimitConfig.QuotaConfig quota : config.getQuotaConfigs()) {
            if (!isQuotaApplicable(quota, userTier)) {
                continue;
            }

            // Simplified quota check
            long currentUsage = getCurrentQuotaUsage(identifier, quota);
            if (currentUsage >= quota.getQuotaLimit()) {
                return RateLimitConfig.RateLimitResponse.builder()
                        .isAllowed(false)
                        .blockReason("Quota limit exceeded: " + quota.getQuotaName())
                        .limitType("QUOTA")
                        .limitScope(quota.getScope().name())
                        .build();
            }
        }

        return allowRequest(identifier, "quota");
    }

    private RateLimitConfig.RateLimitResponse checkBurstLimits(
            RateLimitConfig.BurstConfig burstConfig, String identifier) {

        // Simplified burst detection
        AtomicLong lastRequestTime = lastRequestTimes.get(identifier);
        if (lastRequestTime != null) {
            long timeSinceLastRequest = System.currentTimeMillis() - lastRequestTime.get();
            if (timeSinceLastRequest < 1000) { // Less than 1 second between requests
                AtomicLong burstCount = requestCounts.computeIfAbsent(identifier + "_burst", k -> new AtomicLong(0));
                if (burstCount.incrementAndGet() > burstConfig.getBurstLimit()) {
                    return RateLimitConfig.RateLimitResponse.builder()
                            .isAllowed(false)
                            .blockReason("Burst limit exceeded")
                            .limitType("BURST")
                            .retryAfter(burstConfig.getBurstCooldownMs())
                            .build();
                }
            }
        }

        return allowRequest(identifier, "burst");
    }

    private boolean shouldAdaptiveThrottle(String identifier, RateLimitConfig.AdaptiveConfig adaptiveConfig) {
        // Simplified adaptive throttling based on request rate
        AtomicLong requestCount = requestCounts.get(identifier);
        if (requestCount != null && requestCount.get() > adaptiveConfig.getMaxRateLimit()) {
            log.debug("Adaptive throttling triggered for identifier: {}", identifier);
            return true;
        }
        return false;
    }

    private int getCurrentConcurrentRequests(String identifier) {
        // Simplified concurrent request tracking
        return 1; // In production, track actual concurrent requests
    }

    private void recordRequestMetrics(String identifier, String scope, long responseTimeMs, boolean wasSuccessful) {
        // Record metrics for adaptive rate limiting decisions
        // In production, this would feed into ML models or statistical analysis
    }

    private void updateRateLimitStatus(String identifier, String scope, String endpoint) {
        String key = buildKey(identifier, scope);
        RateLimitConfig.RateLimitStatus status = RateLimitConfig.RateLimitStatus.builder()
                .identifier(identifier)
                .scope(scope)
                .currentCount(requestCounts.getOrDefault(key, new AtomicLong(0)).intValue())
                .status("OK")
                .windowStartTime(System.currentTimeMillis())
                .resetTime(System.currentTimeMillis() + 60000) // 1 minute from now
                .appliedRules(List.of("default"))
                .build();

        rateLimitStatuses.put(key, status);
    }

    private RateLimitConfig.RateLimitResponse allowRequest(String identifier, String scope) {
        return RateLimitConfig.RateLimitResponse.builder()
                .isAllowed(true)
                .limitType("NONE")
                .limitScope(scope)
                .responseHeaders(Map.of(
                        "X-RateLimit-Remaining", "1000",
                        "X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 60000)))
                .build();
    }

    // Helper methods for window calculations
    private String buildKey(String... parts) {
        return String.join(":", parts);
    }

    private long getCurrentWindowId(long windowSeconds) {
        return System.currentTimeMillis() / (windowSeconds * 1000);
    }

    private long getNextWindowResetTime(long windowSeconds) {
        return (getCurrentWindowId(windowSeconds) + 1) * windowSeconds * 1000;
    }

    private long getTimeToNextWindow(long windowSeconds) {
        return getNextWindowResetTime(windowSeconds) - System.currentTimeMillis();
    }

    private boolean isQuotaApplicable(RateLimitConfig.QuotaConfig quota, String userTier) {
        return quota.getApplicableUserTiers() == null ||
               quota.getApplicableUserTiers().isEmpty() ||
               quota.getApplicableUserTiers().contains(userTier);
    }

    private long getCurrentQuotaUsage(String identifier, RateLimitConfig.QuotaConfig quota) {
        // Simplified quota usage calculation
        return requestCounts.getOrDefault(identifier + "_quota_" + quota.getQuotaId(), new AtomicLong(0)).get();
    }

    // Default configuration builders
    private RateLimitConfig createDefaultIPRateLimit() {
        RateLimitConfig.RateLimitRule ipRule = RateLimitConfig.RateLimitRule.builder()
                .ruleId("ip_rate_limit")
                .ruleName("Default IP Rate Limit")
                .description("Default rate limiting per IP address")
                .scope(RateLimitConfig.RateLimitScope.PER_IP)
                .type(RateLimitConfig.RateLimitType.SLIDING_WINDOW)
                .requestLimit(defaultRequestsPerMinute)
                .timeWindowSeconds(60L)
                .timeWindowUnit("MINUTE")
                .priority(1)
                .penaltyAction(RateLimitConfig.PenaltyAction.builder()
                        .actionType("throttle")
                        .penaltyDurationSeconds(300L)
                        .penaltySeverity("moderate")
                        .build())
                .isEnabled(true)
                .build();

        return RateLimitConfig.builder()
                .configId("default_ip")
                .configName("Default IP Rate Limiting")
                .configVersion("1.0")
                .createdAt(OffsetDateTime.now())
                .lastModified(OffsetDateTime.now())
                .isActive(true)
                .rateLimitRules(List.of(ipRule))
                .build();
    }

    private RateLimitConfig createDefaultUserRateLimit() {
        RateLimitConfig.RateLimitRule userRule = RateLimitConfig.RateLimitRule.builder()
                .ruleId("user_rate_limit")
                .ruleName("Default User Rate Limit")
                .description("Default rate limiting per authenticated user")
                .scope(RateLimitConfig.RateLimitScope.PER_USER)
                .type(RateLimitConfig.RateLimitType.TOKEN_BUCKET)
                .requestLimit(defaultRequestsPerHour)
                .timeWindowSeconds(3600L)
                .timeWindowUnit("HOUR")
                .priority(2)
                .isEnabled(true)
                .build();

        return RateLimitConfig.builder()
                .configId("default_user")
                .configName("Default User Rate Limiting")
                .configVersion("1.0")
                .createdAt(OffsetDateTime.now())
                .lastModified(OffsetDateTime.now())
                .isActive(true)
                .rateLimitRules(List.of(userRule))
                .build();
    }

    private RateLimitConfig createSearchEndpointRateLimit() {
        RateLimitConfig.RateLimitRule searchRule = RateLimitConfig.RateLimitRule.builder()
                .ruleId("search_api_limit")
                .ruleName("Search API Rate Limit")
                .description("Rate limiting for search endpoints")
                .scope(RateLimitConfig.RateLimitScope.PER_ENDPOINT)
                .type(RateLimitConfig.RateLimitType.SLIDING_WINDOW)
                .requestLimit(30)
                .timeWindowSeconds(60L)
                .timeWindowUnit("MINUTE")
                .priority(3)
                .applicableEndpoints(List.of("/api/search/**"))
                .isEnabled(true)
                .build();

        RateLimitConfig.BurstConfig burstConfig = RateLimitConfig.BurstConfig.builder()
                .enableBurstHandling(true)
                .burstLimit(defaultBurstLimit)
                .burstWindowSeconds(10L)
                .burstStrategy("token_bucket")
                .burstCooldownMs(30000L)
                .enableBurstDetection(true)
                .burstThreshold(0.8)
                .build();

        return RateLimitConfig.builder()
                .configId("search_api")
                .configName("Search API Rate Limiting")
                .configVersion("1.0")
                .createdAt(OffsetDateTime.now())
                .lastModified(OffsetDateTime.now())
                .isActive(true)
                .rateLimitRules(List.of(searchRule))
                .burstConfig(burstConfig)
                .build();
    }

    // Inner classes for rate limiting algorithms
    private static class TokenBucket {
        private final int capacity;
        private final long refillIntervalMs;
        private volatile int availableTokens;
        private volatile long lastRefillTime;

        public TokenBucket(int capacity, long refillIntervalSeconds) {
            this.capacity = capacity;
            this.refillIntervalMs = refillIntervalSeconds * 1000;
            this.availableTokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refillTokens();
            if (availableTokens > 0) {
                availableTokens--;
                return true;
            }
            return false;
        }

        private void refillTokens() {
            long now = System.currentTimeMillis();
            if (now - lastRefillTime >= refillIntervalMs) {
                availableTokens = capacity;
                lastRefillTime = now;
            }
        }

        public int getAvailableTokens() {
            refillTokens();
            return availableTokens;
        }

        public long getNextRefillTime() {
            return lastRefillTime + refillIntervalMs;
        }

        public long getTimeToNextToken() {
            return Math.max(0, getNextRefillTime() - System.currentTimeMillis());
        }
    }

    private static class SlidingWindowCounter {
        private final int limit;
        private final long windowSizeMs;
        private final Queue<Long> timestamps = new LinkedList<>();

        public SlidingWindowCounter(int limit, long windowSizeSeconds) {
            this.limit = limit;
            this.windowSizeMs = windowSizeSeconds * 1000;
        }

        public synchronized boolean allowRequest() {
            long now = System.currentTimeMillis();
            removeExpiredTimestamps(now);

            if (timestamps.size() < limit) {
                timestamps.offer(now);
                return true;
            }
            return false;
        }

        private void removeExpiredTimestamps(long now) {
            while (!timestamps.isEmpty() && timestamps.peek() <= now - windowSizeMs) {
                timestamps.poll();
            }
        }

        public synchronized int getRemainingRequests() {
            removeExpiredTimestamps(System.currentTimeMillis());
            return Math.max(0, limit - timestamps.size());
        }

        public long getWindowResetTime() {
            return timestamps.isEmpty() ? 0 : timestamps.peek() + windowSizeMs;
        }

        public long getTimeToReset() {
            return Math.max(0, getWindowResetTime() - System.currentTimeMillis());
        }
    }
}