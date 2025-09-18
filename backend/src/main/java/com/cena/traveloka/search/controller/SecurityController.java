package com.cena.traveloka.search.controller;

import com.cena.traveloka.search.dto.*;
import com.cena.traveloka.search.service.SecurityService;
import com.cena.traveloka.search.service.RateLimitingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/search/security")
@RequiredArgsConstructor
@Slf4j
public class SecurityController {

    private final SecurityService securityService;
    private final RateLimitingService rateLimitingService;

    @PostMapping("/context")
    public ResponseEntity<SecurityContext> buildSecurityContext(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String sessionId,
            HttpServletRequest request) {

        log.info("Building security context for user: {}, session: {}", userId, sessionId);

        try {
            SecurityContext context = securityService.buildSecurityContext(request, userId, sessionId);
            return ResponseEntity.ok(context);

        } catch (Exception e) {
            log.error("Failed to build security context", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/fraud-detection")
    public ResponseEntity<FraudDetectionResult> detectFraud(
            @RequestBody SecurityContext securityContext) {

        log.info("Fraud detection request for: {}", securityContext.getRequestId());

        try {
            FraudDetectionResult result = securityService.detectFraud(securityContext);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Fraud detection failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/fraud-detection/async")
    public CompletableFuture<ResponseEntity<FraudDetectionResult>> detectFraudAsync(
            @RequestBody SecurityContext securityContext) {

        log.info("Async fraud detection request for: {}", securityContext.getRequestId());

        return securityService.detectFraudAsync(securityContext)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Async fraud detection failed", throwable);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/block/ip")
    public ResponseEntity<Void> blockIP(
            @RequestParam String ipAddress,
            @RequestParam String reason,
            @RequestParam(defaultValue = "60") long durationMinutes) {

        log.info("Blocking IP: {} for {} minutes - Reason: {}", ipAddress, durationMinutes, reason);

        try {
            securityService.blockIP(ipAddress, reason, durationMinutes);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to block IP", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/block/user")
    public ResponseEntity<Void> blockUser(
            @RequestParam String userId,
            @RequestParam String reason,
            @RequestParam(defaultValue = "60") long durationMinutes) {

        log.info("Blocking user: {} for {} minutes - Reason: {}", userId, durationMinutes, reason);

        try {
            securityService.blockUser(userId, reason, durationMinutes);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to block user", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/events")
    public ResponseEntity<Void> recordSecurityEvent(
            @RequestBody SecurityContext securityContext,
            @RequestParam String eventType,
            @RequestBody(required = false) Map<String, Object> eventData) {

        log.info("Recording security event: {} for request: {}", eventType, securityContext.getRequestId());

        try {
            securityService.recordSecurityEvent(securityContext, eventType, eventData);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to record security event", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/rate-limit/check")
    public ResponseEntity<RateLimitConfig.RateLimitResponse> checkRateLimit(
            @RequestParam String identifier,
            @RequestParam String scope,
            @RequestParam String endpoint,
            @RequestParam(required = false) String userTier) {

        log.debug("Checking rate limit for identifier: {}, scope: {}, endpoint: {}",
                identifier, scope, endpoint);

        try {
            RateLimitConfig.RateLimitResponse response = rateLimitingService.checkRateLimit(
                    identifier, scope, endpoint, userTier);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Rate limit check failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/rate-limit/metrics")
    public ResponseEntity<RateLimitConfig.RateLimitMetrics> getRateLimitMetrics(
            @RequestParam String scope,
            @RequestParam(defaultValue = "1h") String timeWindow) {

        log.info("Getting rate limit metrics for scope: {}, window: {}", scope, timeWindow);

        try {
            RateLimitConfig.RateLimitMetrics metrics = rateLimitingService.getRateLimitMetrics(scope, timeWindow);
            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            log.error("Failed to get rate limit metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/rate-limit/status")
    public ResponseEntity<List<RateLimitConfig.RateLimitStatus>> getAllRateLimitStatuses() {
        log.info("Getting all rate limit statuses");

        try {
            List<RateLimitConfig.RateLimitStatus> statuses = rateLimitingService.getAllRateLimitStatuses();
            return ResponseEntity.ok(statuses);

        } catch (Exception e) {
            log.error("Failed to get rate limit statuses", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/rate-limit/reset")
    public ResponseEntity<Void> resetRateLimit(
            @RequestParam String identifier,
            @RequestParam String scope) {

        log.info("Resetting rate limit for identifier: {}, scope: {}", identifier, scope);

        try {
            rateLimitingService.resetRateLimit(identifier, scope);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to reset rate limit", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/throttle/check")
    public ResponseEntity<Boolean> shouldThrottle(
            @RequestParam String identifier,
            @RequestParam String endpoint) {

        log.debug("Checking if should throttle identifier: {}, endpoint: {}", identifier, endpoint);

        try {
            boolean shouldThrottle = rateLimitingService.shouldThrottle(identifier, endpoint);
            return ResponseEntity.ok(shouldThrottle);

        } catch (Exception e) {
            log.error("Throttle check failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/request/record")
    public ResponseEntity<Void> recordRequest(
            @RequestParam String identifier,
            @RequestParam String scope,
            @RequestParam String endpoint,
            @RequestParam long responseTimeMs,
            @RequestParam boolean wasSuccessful) {

        log.debug("Recording request for identifier: {}, scope: {}, endpoint: {}",
                identifier, scope, endpoint);

        try {
            rateLimitingService.recordRequest(identifier, scope, endpoint, responseTimeMs, wasSuccessful);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to record request", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}