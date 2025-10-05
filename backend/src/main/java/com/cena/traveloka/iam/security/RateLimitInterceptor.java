package com.cena.traveloka.iam.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

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

        int limit = getRateLimitForEndpoint(endpoint);

        String redisKey = RATE_LIMIT_KEY_PREFIX + clientIp + ":" + endpoint;

        Long currentCount = redisTemplate.opsForValue().increment(redisKey);

        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(redisKey, WINDOW_SECONDS, TimeUnit.SECONDS);
        }

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

        long remaining = Math.max(0, limit - (currentCount != null ? currentCount : 0));
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + WINDOW_SECONDS));

        return true;
    }

    private int getRateLimitForEndpoint(String endpoint) {
        if (endpoint.contains("/register") ||
                endpoint.contains("/forgot-password") ||
                endpoint.contains("/reset-password")) {
            return RESTRICTIVE_LIMIT; // 3 per minute for sensitive operations
        }
        return DEFAULT_LIMIT; // 5 per minute for login and verification
    }

    private String getClientIp(HttpServletRequest request) {
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
