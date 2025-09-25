package com.cena.traveloka.iam.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit Logging Interceptor for IAM API requests
 *
 * Logs all API requests for security and compliance purposes
 * Tracks user activity, request details, and response information
 */
@Component
@Slf4j
public class AuditLoggingInterceptor implements HandlerInterceptor {

    private static final String REQUEST_START_TIME = "requestStartTime";
    private static final String REQUEST_ID = "requestId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Generate unique request ID
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        request.setAttribute(REQUEST_ID, requestId);
        request.setAttribute(REQUEST_START_TIME, Instant.now());

        // Log request details
        logRequestStart(request, requestId);

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // Additional logging can be added here if needed
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        logRequestCompletion(request, response, ex);
    }

    private void logRequestStart(HttpServletRequest request, String requestId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth != null && auth.isAuthenticated() ? auth.getName() : "anonymous";

        log.info("API_REQUEST_START [{}] {} {} - User: {} - IP: {} - UserAgent: {}",
                requestId,
                request.getMethod(),
                request.getRequestURI(),
                userId,
                getClientIpAddress(request),
                request.getHeader("User-Agent"));

        // Log query parameters (excluding sensitive data)
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            // Filter out sensitive parameters
            String sanitizedQuery = sanitizeQueryString(queryString);
            log.debug("API_REQUEST_PARAMS [{}] Query: {}", requestId, sanitizedQuery);
        }
    }

    private void logRequestCompletion(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        String requestId = (String) request.getAttribute(REQUEST_ID);
        Instant startTime = (Instant) request.getAttribute(REQUEST_START_TIME);
        long duration = startTime != null ?
                java.time.Duration.between(startTime, Instant.now()).toMillis() : -1;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth != null && auth.isAuthenticated() ? auth.getName() : "anonymous";

        if (ex != null) {
            log.error("API_REQUEST_ERROR [{}] {} {} - User: {} - Status: {} - Duration: {}ms - Error: {}",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    userId,
                    response.getStatus(),
                    duration,
                    ex.getMessage());
        } else {
            log.info("API_REQUEST_COMPLETE [{}] {} {} - User: {} - Status: {} - Duration: {}ms",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    userId,
                    response.getStatus(),
                    duration);
        }

        // Log slow requests (> 1 second)
        if (duration > 1000) {
            log.warn("SLOW_REQUEST [{}] {} {} took {}ms - User: {}",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    duration,
                    userId);
        }
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

    private String sanitizeQueryString(String queryString) {
        // Remove sensitive parameters like passwords, tokens, etc.
        return queryString
                .replaceAll("(?i)(password|token|secret|key)=[^&]*", "$1=***")
                .replaceAll("(?i)(access_token|refresh_token)=[^&]*", "$1=***");
    }
}