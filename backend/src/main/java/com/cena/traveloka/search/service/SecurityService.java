package com.cena.traveloka.search.service;

import com.cena.traveloka.search.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityService {

    private final FraudDetectionService fraudDetectionService;
    private final ThreatIntelligenceService threatIntelligenceService;
    private final DeviceFingerprintingService deviceFingerprintingService;

    @Value("${security.risk.threshold.low:30}")
    private int lowRiskThreshold;

    @Value("${security.risk.threshold.medium:60}")
    private int mediumRiskThreshold;

    @Value("${security.risk.threshold.high:80}")
    private int highRiskThreshold;

    @Value("${security.enable.realtime.monitoring:true}")
    private boolean enableRealtimeMonitoring;

    @Value("${security.enable.ml.detection:true}")
    private boolean enableMLDetection;

    // In-memory stores for demonstration (in production, use Redis/database)
    private final Map<String, AtomicInteger> ipRequestCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> userRequestCounts = new ConcurrentHashMap<>();
    private final Map<String, OffsetDateTime> lastRequestTimes = new ConcurrentHashMap<>();
    private final Set<String> blockedIPs = ConcurrentHashMap.newKeySet();
    private final Set<String> blockedUsers = ConcurrentHashMap.newKeySet();

    // Suspicious patterns
    private static final List<Pattern> SUSPICIOUS_USER_AGENTS = List.of(
            Pattern.compile("(?i).*bot.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i).*crawler.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i).*scraper.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i).*spider.*", Pattern.CASE_INSENSITIVE)
    );

    private static final List<String> HIGH_RISK_COUNTRIES = List.of(
            "XX", "YY" // Placeholder country codes
    );

    public SecurityContext buildSecurityContext(HttpServletRequest request, String userId, String sessionId) {
        log.debug("Building security context for request from IP: {}, User: {}",
                getClientIP(request), userId);

        try {
            // Extract basic request information
            String ipAddress = getClientIP(request);
            String userAgent = request.getHeader("User-Agent");

            // Build geographic context
            SecurityContext.GeographicContext geoContext = buildGeographicContext(ipAddress, request);

            // Build device context
            SecurityContext.DeviceContext deviceContext = buildDeviceContext(request, userAgent);

            // Build request metadata
            SecurityContext.RequestMetadata requestMetadata = buildRequestMetadata(request);

            // Perform risk assessment
            SecurityContext.RiskAssessment riskAssessment = performRiskAssessment(
                    ipAddress, userId, userAgent, geoContext, deviceContext);

            // Behavioral analysis
            SecurityContext.BehavioralAnalysis behavioralAnalysis = analyzeBehavior(
                    ipAddress, userId, sessionId, request);

            // Set security flags
            SecurityContext.SecurityFlags securityFlags = determineSecurityFlags(
                    riskAssessment, behavioralAnalysis, ipAddress, userId);

            return SecurityContext.builder()
                    .requestId(UUID.randomUUID().toString())
                    .sessionId(sessionId)
                    .userId(userId)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .geographicContext(geoContext)
                    .deviceContext(deviceContext)
                    .requestMetadata(requestMetadata)
                    .riskAssessment(riskAssessment)
                    .behavioralAnalysis(behavioralAnalysis)
                    .securityFlags(securityFlags)
                    .build();

        } catch (Exception e) {
            log.error("Failed to build security context", e);
            return buildDefaultSecurityContext(request, userId, sessionId);
        }
    }

    public boolean isRequestBlocked(SecurityContext securityContext) {
        try {
            // Check IP blocklist
            if (blockedIPs.contains(securityContext.getIpAddress())) {
                log.warn("Request blocked - IP in blocklist: {}", securityContext.getIpAddress());
                return true;
            }

            // Check user blocklist
            if (securityContext.getUserId() != null &&
                blockedUsers.contains(securityContext.getUserId())) {
                log.warn("Request blocked - User in blocklist: {}", securityContext.getUserId());
                return true;
            }

            // Check security flags
            if (securityContext.getSecurityFlags() != null &&
                Boolean.TRUE.equals(securityContext.getSecurityFlags().getBlockRequest())) {
                log.warn("Request blocked - Security flags indicate blocking");
                return true;
            }

            // Check risk level
            if (securityContext.getRiskAssessment() != null) {
                String riskLevel = securityContext.getRiskAssessment().getRiskLevel();
                if ("critical".equals(riskLevel)) {
                    log.warn("Request blocked - Critical risk level detected");
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            log.error("Error checking if request should be blocked", e);
            // Fail-safe: don't block on error
            return false;
        }
    }

    public CompletableFuture<FraudDetectionResult> detectFraudAsync(SecurityContext securityContext) {
        return CompletableFuture.supplyAsync(() -> detectFraud(securityContext));
    }

    public FraudDetectionResult detectFraud(SecurityContext securityContext) {
        log.debug("Detecting fraud for request: {}", securityContext.getRequestId());

        try {
            long startTime = System.currentTimeMillis();

            // Rule-based detection
            List<FraudDetectionResult.FraudRule> triggeredRules = evaluateFraudRules(securityContext);

            // ML-based detection
            FraudDetectionResult.MLAnalysisResult mlAnalysis = null;
            if (enableMLDetection) {
                mlAnalysis = performMLAnalysis(securityContext);
            }

            // Pattern analysis
            FraudDetectionResult.PatternAnalysis patternAnalysis = analyzePatterns(securityContext);

            // Calculate overall fraud score
            int fraudScore = calculateFraudScore(triggeredRules, mlAnalysis, patternAnalysis);
            String fraudLevel = determineFraudLevel(fraudScore);
            boolean isFraudulent = fraudScore >= mediumRiskThreshold;

            // Generate recommendations
            List<FraudDetectionResult.MitigationAction> mitigationActions =
                    generateMitigationActions(fraudScore, triggeredRules);

            // Build fraud indicators
            List<FraudDetectionResult.FraudIndicator> fraudIndicators =
                    buildFraudIndicators(triggeredRules, mlAnalysis, patternAnalysis);

            long processingTime = System.currentTimeMillis() - startTime;

            FraudDetectionResult result = FraudDetectionResult.builder()
                    .detectionId(UUID.randomUUID().toString())
                    .detectionTimestamp(OffsetDateTime.now())
                    .isFraudulent(isFraudulent)
                    .fraudScore(String.valueOf(fraudScore))
                    .confidence(calculateConfidence(triggeredRules, mlAnalysis))
                    .recommendation(determineRecommendation(fraudScore))
                    .fraudIndicators(fraudIndicators)
                    .triggeredRules(triggeredRules)
                    .mlAnalysis(mlAnalysis)
                    .patternAnalysis(patternAnalysis)
                    .recommendedActions(mitigationActions)
                    .build();

            log.debug("Fraud detection completed in {}ms, score: {}, level: {}",
                    processingTime, fraudScore, fraudLevel);

            return result;

        } catch (Exception e) {
            log.error("Fraud detection failed", e);
            return buildErrorFraudDetectionResult(e);
        }
    }

    public void recordSecurityEvent(SecurityContext securityContext, String eventType, Map<String, Object> eventData) {
        try {
            log.info("Security event recorded - Type: {}, IP: {}, User: {}, Data: {}",
                    eventType, securityContext.getIpAddress(), securityContext.getUserId(), eventData);

            // In production, this would save to a security events database
            // and potentially trigger alerts or automated responses

        } catch (Exception e) {
            log.error("Failed to record security event", e);
        }
    }

    public void blockIP(String ipAddress, String reason, long durationMinutes) {
        log.warn("Blocking IP: {} for {} minutes - Reason: {}", ipAddress, durationMinutes, reason);

        blockedIPs.add(ipAddress);

        // In production, implement timed removal
        CompletableFuture.delayedExecutor(java.time.Duration.ofMinutes(durationMinutes))
                .execute(() -> {
                    blockedIPs.remove(ipAddress);
                    log.info("IP block expired for: {}", ipAddress);
                });
    }

    public void blockUser(String userId, String reason, long durationMinutes) {
        log.warn("Blocking user: {} for {} minutes - Reason: {}", userId, durationMinutes, reason);

        blockedUsers.add(userId);

        // In production, implement timed removal
        CompletableFuture.delayedExecutor(java.time.Duration.ofMinutes(durationMinutes))
                .execute(() -> {
                    blockedUsers.remove(userId);
                    log.info("User block expired for: {}", userId);
                });
    }

    // Private helper methods

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }

    private SecurityContext.GeographicContext buildGeographicContext(String ipAddress, HttpServletRequest request) {
        // In production, use actual geolocation service
        String detectedCountry = detectCountryFromIP(ipAddress);
        boolean isHighRiskCountry = HIGH_RISK_COUNTRIES.contains(detectedCountry);

        return SecurityContext.GeographicContext.builder()
                .detectedCountry(detectedCountry)
                .detectedRegion("Unknown")
                .detectedCity("Unknown")
                .locationSource("ip")
                .isProxyDetected(isProxyIP(ipAddress))
                .isVpnDetected(isVpnIP(ipAddress))
                .isTorDetected(isTorIP(ipAddress))
                .suspiciousLocationIndicators(isHighRiskCountry ? List.of("high_risk_country") : List.of())
                .build();
    }

    private SecurityContext.DeviceContext buildDeviceContext(HttpServletRequest request, String userAgent) {
        boolean isBot = isBotUserAgent(userAgent);

        SecurityContext.DeviceRiskScore riskScore = SecurityContext.DeviceRiskScore.builder()
                .score(isBot ? 80 : 20)
                .riskLevel(isBot ? "high" : "low")
                .riskFactors(isBot ? List.of("bot_user_agent") : List.of())
                .isBot(isBot)
                .build();

        return SecurityContext.DeviceContext.builder()
                .deviceType(detectDeviceType(userAgent))
                .operatingSystem(detectOS(userAgent))
                .browserName(detectBrowser(userAgent))
                .userAgent(userAgent)
                .riskScore(riskScore)
                .isRecognizedDevice(false)
                .firstSeen(OffsetDateTime.now())
                .lastSeen(OffsetDateTime.now())
                .build();
    }

    private SecurityContext.RequestMetadata buildRequestMetadata(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name ->
                headers.put(name, request.getHeader(name)));

        return SecurityContext.RequestMetadata.builder()
                .timestamp(OffsetDateTime.now())
                .method(request.getMethod())
                .endpoint(request.getRequestURI())
                .headers(headers)
                .referer(request.getHeader("Referer"))
                .origin(request.getHeader("Origin"))
                .isSecureConnection(request.isSecure())
                .build();
    }

    private SecurityContext.RiskAssessment performRiskAssessment(
            String ipAddress, String userId, String userAgent,
            SecurityContext.GeographicContext geoContext,
            SecurityContext.DeviceContext deviceContext) {

        List<SecurityContext.RiskFactor> riskFactors = new ArrayList<>();
        int riskScore = 0;

        // Check for bot user agent
        if (isBotUserAgent(userAgent)) {
            riskFactors.add(SecurityContext.RiskFactor.builder()
                    .factorType("USER_AGENT")
                    .factorName("BOT_DETECTED")
                    .severity(7)
                    .confidence(0.9)
                    .description("Bot user agent detected")
                    .build());
            riskScore += 30;
        }

        // Check for high-risk geolocation
        if (geoContext.getSuspiciousLocationIndicators() != null &&
            !geoContext.getSuspiciousLocationIndicators().isEmpty()) {
            riskFactors.add(SecurityContext.RiskFactor.builder()
                    .factorType("GEOLOCATION")
                    .factorName("HIGH_RISK_COUNTRY")
                    .severity(5)
                    .confidence(0.7)
                    .description("Request from high-risk country")
                    .build());
            riskScore += 20;
        }

        // Check request rate
        AtomicInteger ipCount = ipRequestCounts.computeIfAbsent(ipAddress, k -> new AtomicInteger(0));
        int requestCount = ipCount.incrementAndGet();

        if (requestCount > 100) { // Threshold for suspicious activity
            riskFactors.add(SecurityContext.RiskFactor.builder()
                    .factorType("RATE_LIMITING")
                    .factorName("HIGH_REQUEST_RATE")
                    .severity(8)
                    .confidence(0.95)
                    .description("Unusually high request rate from IP")
                    .build());
            riskScore += 40;
        }

        String riskLevel = riskScore >= highRiskThreshold ? "critical" :
                          riskScore >= mediumRiskThreshold ? "high" :
                          riskScore >= lowRiskThreshold ? "medium" : "low";

        String recommendation = riskScore >= highRiskThreshold ? "block" :
                               riskScore >= mediumRiskThreshold ? "challenge" :
                               riskScore >= lowRiskThreshold ? "monitor" : "allow";

        return SecurityContext.RiskAssessment.builder()
                .overallRiskScore(riskScore)
                .riskLevel(riskLevel)
                .riskFactors(riskFactors)
                .recommendation(recommendation)
                .requiresManualReview(riskScore >= highRiskThreshold)
                .assessmentTimestamp(OffsetDateTime.now())
                .build();
    }

    private SecurityContext.BehavioralAnalysis analyzeBehavior(
            String ipAddress, String userId, String sessionId, HttpServletRequest request) {

        // Request pattern analysis
        AtomicInteger requestCount = ipRequestCounts.getOrDefault(ipAddress, new AtomicInteger(0));

        SecurityContext.RequestPattern requestPattern = SecurityContext.RequestPattern.builder()
                .requestsPerMinute(calculateRequestsPerMinute(ipAddress))
                .requestsPerHour(requestCount.get())
                .averageTimeBetweenRequests(calculateAverageTimeBetween(ipAddress))
                .showsAutomatedBehavior(requestCount.get() > 50)
                .hasUnusualPatterns(requestCount.get() > 100)
                .build();

        // Search behavior (simplified)
        SecurityContext.SearchBehavior searchBehavior = SecurityContext.SearchBehavior.builder()
                .totalSearches(requestCount.get())
                .queryComplexity(0.5)
                .showsScrapingBehavior(requestCount.get() > 200)
                .searchVelocity(calculateSearchVelocity(ipAddress))
                .build();

        return SecurityContext.BehavioralAnalysis.builder()
                .requestPattern(requestPattern)
                .searchBehavior(searchBehavior)
                .build();
    }

    private SecurityContext.SecurityFlags determineSecurityFlags(
            SecurityContext.RiskAssessment riskAssessment,
            SecurityContext.BehavioralAnalysis behavioralAnalysis,
            String ipAddress, String userId) {

        boolean suspiciousActivity = riskAssessment.getOverallRiskScore() >= lowRiskThreshold;
        boolean maliciousIntent = riskAssessment.getOverallRiskScore() >= mediumRiskThreshold;
        boolean blockRequest = riskAssessment.getOverallRiskScore() >= highRiskThreshold;
        boolean requiresCaptcha = riskAssessment.getOverallRiskScore() >= mediumRiskThreshold;

        return SecurityContext.SecurityFlags.builder()
                .suspiciousActivity(suspiciousActivity)
                .maliciousIntent(maliciousIntent)
                .blockRequest(blockRequest)
                .requiresCaptcha(requiresCaptcha)
                .logSuspiciousActivity(suspiciousActivity)
                .triggerAlert(maliciousIntent)
                .build();
    }

    private List<FraudDetectionResult.FraudRule> evaluateFraudRules(SecurityContext securityContext) {
        List<FraudDetectionResult.FraudRule> triggeredRules = new ArrayList<>();

        // Rate limiting rule
        if (securityContext.getBehavioralAnalysis() != null &&
            securityContext.getBehavioralAnalysis().getRequestPattern() != null &&
            securityContext.getBehavioralAnalysis().getRequestPattern().getRequestsPerHour() > 100) {

            triggeredRules.add(FraudDetectionResult.FraudRule.builder()
                    .ruleId("RATE_LIMIT_001")
                    .ruleName("High Request Rate")
                    .ruleType("VELOCITY")
                    .severity("HIGH")
                    .description("Request rate exceeds normal user behavior")
                    .isBlocking(true)
                    .build());
        }

        // Bot detection rule
        if (securityContext.getDeviceContext() != null &&
            securityContext.getDeviceContext().getRiskScore() != null &&
            Boolean.TRUE.equals(securityContext.getDeviceContext().getRiskScore().getIsBot())) {

            triggeredRules.add(FraudDetectionResult.FraudRule.builder()
                    .ruleId("BOT_DETECT_001")
                    .ruleName("Bot User Agent")
                    .ruleType("PATTERN")
                    .severity("MEDIUM")
                    .description("Automated bot behavior detected")
                    .isBlocking(false)
                    .build());
        }

        return triggeredRules;
    }

    private FraudDetectionResult.MLAnalysisResult performMLAnalysis(SecurityContext securityContext) {
        // Simplified ML analysis simulation
        List<FraudDetectionResult.ModelPrediction> predictions = List.of(
                FraudDetectionResult.ModelPrediction.builder()
                        .modelName("AnomalyDetector")
                        .modelType("ANOMALY_DETECTION")
                        .prediction("NORMAL")
                        .probability(0.75)
                        .modelVersion("v1.2")
                        .build(),
                FraudDetectionResult.ModelPrediction.builder()
                        .modelName("BotClassifier")
                        .modelType("CLASSIFICATION")
                        .prediction("HUMAN")
                        .probability(0.65)
                        .modelVersion("v2.1")
                        .build()
        );

        return FraudDetectionResult.MLAnalysisResult.builder()
                .modelVersion("ensemble-v1")
                .predictions(predictions)
                .modelConfidence(0.70)
                .modelRecommendation("MONITOR")
                .build();
    }

    private FraudDetectionResult.PatternAnalysis analyzePatterns(SecurityContext securityContext) {
        // Simplified pattern analysis
        return FraudDetectionResult.PatternAnalysis.builder()
                .sequenceAnalysis(FraudDetectionResult.SequenceAnalysis.builder()
                        .sequenceType("HUMAN_LIKE")
                        .sequenceScore(0.75)
                        .hasUnusualSequence(false)
                        .build())
                .temporalAnalysis(FraudDetectionResult.TemporalAnalysis.builder()
                        .requestTiming("HUMAN_LIKE")
                        .averageRequestInterval(5.2)
                        .hasUniformTiming(false)
                        .hasUnusualVelocity(false)
                        .build())
                .build();
    }

    private int calculateFraudScore(List<FraudDetectionResult.FraudRule> rules,
                                   FraudDetectionResult.MLAnalysisResult mlAnalysis,
                                   FraudDetectionResult.PatternAnalysis patternAnalysis) {
        int score = 0;

        // Score from triggered rules
        for (FraudDetectionResult.FraudRule rule : rules) {
            switch (rule.getSeverity()) {
                case "HIGH" -> score += 30;
                case "MEDIUM" -> score += 20;
                case "LOW" -> score += 10;
            }
        }

        // Score from ML analysis
        if (mlAnalysis != null) {
            double avgProbability = mlAnalysis.getPredictions().stream()
                    .mapToDouble(FraudDetectionResult.ModelPrediction::getProbability)
                    .average()
                    .orElse(0.0);
            score += (int) (avgProbability * 20);
        }

        return Math.min(100, score); // Cap at 100
    }

    // Simplified helper methods
    private String detectCountryFromIP(String ipAddress) { return "US"; }
    private boolean isProxyIP(String ipAddress) { return false; }
    private boolean isVpnIP(String ipAddress) { return false; }
    private boolean isTorIP(String ipAddress) { return false; }
    private boolean isBotUserAgent(String userAgent) {
        if (userAgent == null) return true;
        return SUSPICIOUS_USER_AGENTS.stream().anyMatch(pattern -> pattern.matcher(userAgent).matches());
    }
    private String detectDeviceType(String userAgent) { return "desktop"; }
    private String detectOS(String userAgent) { return "Unknown"; }
    private String detectBrowser(String userAgent) { return "Unknown"; }
    private int calculateRequestsPerMinute(String ipAddress) { return 5; }
    private double calculateAverageTimeBetween(String ipAddress) { return 12.0; }
    private double calculateSearchVelocity(String ipAddress) { return 0.5; }
    private String determineFraudLevel(int score) {
        return score >= highRiskThreshold ? "CRITICAL" :
               score >= mediumRiskThreshold ? "HIGH" :
               score >= lowRiskThreshold ? "MEDIUM" : "LOW";
    }
    private String determineRecommendation(int score) {
        return score >= highRiskThreshold ? "BLOCK" :
               score >= mediumRiskThreshold ? "CHALLENGE" :
               score >= lowRiskThreshold ? "MONITOR" : "ALLOW";
    }
    private double calculateConfidence(List<FraudDetectionResult.FraudRule> rules, FraudDetectionResult.MLAnalysisResult mlAnalysis) {
        return 0.75;
    }
    private List<FraudDetectionResult.MitigationAction> generateMitigationActions(int score, List<FraudDetectionResult.FraudRule> rules) {
        return List.of();
    }
    private List<FraudDetectionResult.FraudIndicator> buildFraudIndicators(List<FraudDetectionResult.FraudRule> rules, FraudDetectionResult.MLAnalysisResult mlAnalysis, FraudDetectionResult.PatternAnalysis patternAnalysis) {
        return List.of();
    }

    private SecurityContext buildDefaultSecurityContext(HttpServletRequest request, String userId, String sessionId) {
        return SecurityContext.builder()
                .requestId(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .userId(userId)
                .ipAddress(getClientIP(request))
                .userAgent(request.getHeader("User-Agent"))
                .build();
    }

    private FraudDetectionResult buildErrorFraudDetectionResult(Exception e) {
        return FraudDetectionResult.builder()
                .detectionId(UUID.randomUUID().toString())
                .detectionTimestamp(OffsetDateTime.now())
                .isFraudulent(false)
                .fraudScore("0")
                .confidence(0.0)
                .recommendation("ALLOW")
                .fraudIndicators(List.of())
                .triggeredRules(List.of())
                .build();
    }
}