package com.cena.traveloka.iam.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Request Tracing Interceptor for IAM API requests
 *
 * Adds request tracing capabilities using MDC (Mapped Diagnostic Context)
 * Enables correlation of log messages across the entire request lifecycle
 */
@Component
@Slf4j
public class RequestTracingInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_IP_ADDRESS = "ipAddress";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Generate or extract trace ID
        String traceId = extractOrGenerateTraceId(request);
        String requestId = extractOrGenerateRequestId(request);

        // Set MDC context
        MDC.put(MDC_TRACE_ID, traceId);
        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_IP_ADDRESS, getClientIpAddress(request));

        // Add trace ID to response headers for client correlation
        response.setHeader(TRACE_ID_HEADER, traceId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        // Store in request attributes for use in controllers
        request.setAttribute("traceId", traceId);
        request.setAttribute("requestId", requestId);

        log.debug("Request tracing started - TraceID: {}, RequestID: {}", traceId, requestId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            String traceId = MDC.get(MDC_TRACE_ID);
            String requestId = MDC.get(MDC_REQUEST_ID);

            log.debug("Request tracing completed - TraceID: {}, RequestID: {}", traceId, requestId);
        } finally {
            // Clear MDC to prevent memory leaks
            MDC.clear();
        }
    }

    private String extractOrGenerateTraceId(HttpServletRequest request) {
        // Try to extract from header first (for distributed tracing)
        String traceId = request.getHeader(TRACE_ID_HEADER);

        if (traceId == null || traceId.trim().isEmpty()) {
            // Generate new trace ID
            traceId = generateTraceId();
        }

        return traceId;
    }

    private String extractOrGenerateRequestId(HttpServletRequest request) {
        // Try to extract from header first
        String requestId = request.getHeader(REQUEST_ID_HEADER);

        if (requestId == null || requestId.trim().isEmpty()) {
            // Generate new request ID
            requestId = generateRequestId();
        }

        return requestId;
    }

    private String generateTraceId() {
        return "trace-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String generateRequestId() {
        return "req-" + UUID.randomUUID().toString().substring(0, 8);
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

    /**
     * Utility method to get current trace ID from MDC
     */
    public static String getCurrentTraceId() {
        return MDC.get(MDC_TRACE_ID);
    }

    /**
     * Utility method to get current request ID from MDC
     */
    public static String getCurrentRequestId() {
        return MDC.get(MDC_REQUEST_ID);
    }

    /**
     * Utility method to set user ID in MDC (called after authentication)
     */
    public static void setCurrentUserId(String userId) {
        if (userId != null) {
            MDC.put(MDC_USER_ID, userId);
        }
    }

    /**
     * Utility method to get current user ID from MDC
     */
    public static String getCurrentUserId() {
        return MDC.get(MDC_USER_ID);
    }
}