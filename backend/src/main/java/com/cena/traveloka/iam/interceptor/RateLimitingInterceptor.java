package com.cena.traveloka.iam.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting Interceptor for IAM API requests
 *
 * Implements rate limiting to protect against abuse and DoS attacks
 * Uses Redis for distributed rate limiting across multiple instances
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    @Value("${app.rate-limit.requests-per-hour:1000}")
    private int requestsPerHour;

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.rate-limit.admin-bypass:true}")
    private boolean adminBypass;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String MINUTE_SUFFIX = ":minute";
    private static final String HOUR_SUFFIX = ":hour";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!rateLimitEnabled) {
            return true;
        }

        // Skip rate limiting for health checks
        if (isHealthCheck(request)) {
            return true;
        }

        // Get user identifier for rate limiting
        String userIdentifier = getUserIdentifier(request);

        // Check if user should bypass rate limiting
        if (shouldBypassRateLimit(userIdentifier)) {
            return true;
        }

        // Check rate limits
        try {
            if (!checkRateLimit(userIdentifier, request, response)) {
                logRateLimitExceeded(userIdentifier, request);
                return false;
            }
        } catch (Exception e) {
            log.error("Error checking rate limit for user {}: {}", userIdentifier, e.getMessage());
            // Allow request to proceed if rate limiting service fails
            return true;
        }

        return true;
    }

    private boolean checkRateLimit(String userIdentifier, HttpServletRequest request, HttpServletResponse response) {
        // Check minute-based rate limit
        if (!checkMinuteRateLimit(userIdentifier)) {
            setRateLimitHeaders(response, 0, Duration.ofMinutes(1));
            sendRateLimitResponse(response, "Too many requests per minute. Rate limit: " + requestsPerMinute + " requests/minute");
            return false;
        }

        // Check hour-based rate limit
        if (!checkHourRateLimit(userIdentifier)) {
            setRateLimitHeaders(response, 0, Duration.ofHours(1));
            sendRateLimitResponse(response, "Too many requests per hour. Rate limit: " + requestsPerHour + " requests/hour");
            return false;
        }

        // Set success headers
        int remainingMinute = getRemainingRequests(userIdentifier + MINUTE_SUFFIX, requestsPerMinute);
        int remainingHour = getRemainingRequests(userIdentifier + HOUR_SUFFIX, requestsPerHour);
        int remaining = Math.min(remainingMinute, remainingHour);

        setRateLimitHeaders(response, remaining, Duration.ofMinutes(1));

        return true;
    }

    private boolean checkMinuteRateLimit(String userIdentifier) {
        String key = RATE_LIMIT_PREFIX + userIdentifier + MINUTE_SUFFIX;
        return checkAndIncrementCounter(key, requestsPerMinute, Duration.ofMinutes(1));
    }

    private boolean checkHourRateLimit(String userIdentifier) {
        String key = RATE_LIMIT_PREFIX + userIdentifier + HOUR_SUFFIX;
        return checkAndIncrementCounter(key, requestsPerHour, Duration.ofHours(1));
    }

    private boolean checkAndIncrementCounter(String key, int limit, Duration window) {
        try {
            // Get current count
            String currentCountStr = redisTemplate.opsForValue().get(key);
            int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;

            if (currentCount >= limit) {
                return false;
            }

            // Increment counter
            Long newCount = redisTemplate.opsForValue().increment(key);

            // Set expiration if this is the first increment
            if (newCount == 1) {
                redisTemplate.expire(key, window.getSeconds(), TimeUnit.SECONDS);
            }

            return newCount <= limit;

        } catch (Exception e) {
            log.error("Error checking rate limit for key {}: {}", key, e.getMessage());
            return true; // Allow request if Redis fails
        }
    }

    private int getRemainingRequests(String keySuffix, int limit) {
        try {
            String key = RATE_LIMIT_PREFIX + keySuffix;
            String currentCountStr = redisTemplate.opsForValue().get(key);
            int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;
            return Math.max(0, limit - currentCount);
        } catch (Exception e) {
            return limit; // Return full limit if Redis fails
        }
    }

    private String getUserIdentifier(HttpServletRequest request) {
        // Use authenticated user ID if available
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return "user:" + auth.getName();
        }

        // Fall back to IP address for anonymous users
        return "ip:" + getClientIpAddress(request);
    }

    private boolean shouldBypassRateLimit(String userIdentifier) {
        if (!adminBypass) {
            return false;
        }

        // Check if user has admin role
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        }

        return false;
    }

    private boolean isHealthCheck(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.contains("/health") || uri.contains("/actuator");
    }

    private void setRateLimitHeaders(HttpServletResponse response, int remaining, Duration resetTime) {
        response.setHeader("X-Rate-Limit-Limit", String.valueOf(requestsPerMinute));
        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(remaining));
        response.setHeader("X-Rate-Limit-Reset", String.valueOf(System.currentTimeMillis() + resetTime.toMillis()));
    }

    private void sendRateLimitResponse(HttpServletResponse response, String message) {
        try {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format("""
                    {
                        "error": "too_many_requests",
                        "errorDescription": "%s",
                        "errorCode": "IAM_RATE_LIMIT_EXCEEDED",
                        "status": 429,
                        "timestamp": "%s"
                    }
                    """, message, java.time.Instant.now()));
        } catch (Exception e) {
            log.error("Error sending rate limit response: {}", e.getMessage());
        }
    }

    private void logRateLimitExceeded(String userIdentifier, HttpServletRequest request) {
        log.warn("RATE_LIMIT_EXCEEDED - User: {} - IP: {} - Method: {} - URI: {}",
                userIdentifier,
                getClientIpAddress(request),
                request.getMethod(),
                request.getRequestURI());
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}