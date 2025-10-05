package com.cena.traveloka.iam.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * Rate limit interceptor for authentication endpoints.
 * <p>
 * Uses Redis to track and enforce rate limits per IP address.
 * </p>
 *
 * <p>Implementation Details:</p>
 * <ul>
 *   <li>Tracks requests by IP address</li>
 *   <li>Uses Redis with TTL for automatic cleanup</li>
 *   <li>Returns HTTP 429 (Too Many Requests) when limit exceeded</li>
 *   <li>Provides retry-after header with seconds to wait</li>
 * </ul>
 *
 * <p>Rate Limits by Endpoint:</p>
 * <ul>
 *   <li>/api/v1/auth/login: 5 requests per minute</li>
 *   <li>/api/v1/auth/register: 3 requests per minute</li>
 *   <li>/api/v1/auth/forgot-password: 3 requests per minute</li>
 *   <li>/api/v1/auth/reset-password: 3 requests per minute</li>
 *   <li>/api/v1/auth/verify-email: 5 requests per minute</li>
 * </ul>
 *
 * @author Traveloka IAM Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    private static final int DEFAULT_LIMIT = 5;
    private static final int RESTRICTIVE_LIMIT = 3;
    private static final long WINDOW_SECONDS = 60; // 1 minute

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String clientIp = getClientIp(request);
        String endpoint = request.getRequestURI();

        // Determine rate limit based on endpoint
        int limit = getRateLimitForEndpoint(endpoint);

        // Create Redis key: rate_limit:ip:endpoint
        String redisKey = RATE_LIMIT_KEY_PREFIX + clientIp + ":" + endpoint;

        // Increment counter
        Long currentCount = redisTemplate.opsForValue().increment(redisKey);

        // Set expiry on first request
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(redisKey, WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        // Check if limit exceeded
        if (currentCount != null && currentCount > limit) {
            log.warn("Rate limit exceeded for IP: {} on endpoint: {} (count: {})",
                    clientIp, endpoint, currentCount);

            response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            response.setContentType("application/json");
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + WINDOW_SECONDS));
            response.setHeader("Retry-After", String.valueOf(WINDOW_SECONDS));

            String errorJson = String.format(
                    "{\"status\":\"ERROR\",\"code\":\"RATE_LIMIT_EXCEEDED\"," +
                            "\"message\":\"Too many requests. Please try again later.\"," +
                            "\"timestamp\":\"%s\"}",
                    java.time.OffsetDateTime.now().toString()
            );

            response.getWriter().write(errorJson);
            return false;
        }

        // Add rate limit headers to successful requests
        long remaining = Math.max(0, limit - (currentCount != null ? currentCount : 0));
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + WINDOW_SECONDS));

        return true;
    }

    /**
     * Determine rate limit based on endpoint sensitivity.
     *
     * @param endpoint Request URI
     * @return Maximum requests per minute
     */
    private int getRateLimitForEndpoint(String endpoint) {
        if (endpoint.contains("/register") ||
                endpoint.contains("/forgot-password") ||
                endpoint.contains("/reset-password")) {
            return RESTRICTIVE_LIMIT; // 3 per minute for sensitive operations
        }
        return DEFAULT_LIMIT; // 5 per minute for login and verification
    }

    /**
     * Extract client IP address from request.
     * Handles X-Forwarded-For header for proxied requests.
     *
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
