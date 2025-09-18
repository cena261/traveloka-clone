package com.cena.traveloka.search.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@Jacksonized
public class SecurityContext {

    // Request identification
    private String requestId;
    private String sessionId;
    private String userId;
    private String ipAddress;
    private String userAgent;

    // Geographic context
    private GeographicContext geographicContext;

    // Device and browser information
    private DeviceContext deviceContext;

    // Request metadata
    private RequestMetadata requestMetadata;

    // Risk assessment
    private RiskAssessment riskAssessment;

    // Behavioral analysis
    private BehavioralAnalysis behavioralAnalysis;

    // Security flags
    private SecurityFlags securityFlags;

    @Data
    @Builder
    @Jacksonized
    public static class GeographicContext {
        private String detectedCountry;
        private String detectedRegion;
        private String detectedCity;
        private String timeZone;
        private Double latitude;
        private Double longitude;
        private String locationAccuracy;
        private String locationSource; // ip, gps, manual
        private Boolean isProxyDetected;
        private Boolean isVpnDetected;
        private Boolean isTorDetected;
        private String proxyType;
        private List<String> suspiciousLocationIndicators;
    }

    @Data
    @Builder
    @Jacksonized
    public static class DeviceContext {
        private String deviceType; // mobile, desktop, tablet, bot
        private String operatingSystem;
        private String browserName;
        private String browserVersion;
        private String deviceModel;
        private String screenResolution;
        private List<String> installedPlugins;
        private Boolean isJavaScriptEnabled;
        private Boolean areCookiesEnabled;
        private String fingerprint;
        private DeviceRiskScore riskScore;
        private Boolean isRecognizedDevice;
        private OffsetDateTime firstSeen;
        private OffsetDateTime lastSeen;
    }

    @Data
    @Builder
    @Jacksonized
    public static class DeviceRiskScore {
        private Integer score; // 0-100
        private String riskLevel; // low, medium, high, critical
        private List<String> riskFactors;
        private Boolean isBot;
        private Boolean isCompromised;
        private Boolean isSuspicious;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RequestMetadata {
        private OffsetDateTime timestamp;
        private String method;
        private String endpoint;
        private Map<String, String> headers;
        private Long requestSize;
        private String referer;
        private String origin;
        private List<String> languages;
        private String encoding;
        private Boolean isSecureConnection;
        private String tlsVersion;
        private List<String> customHeaders;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RiskAssessment {
        private Integer overallRiskScore; // 0-100
        private String riskLevel; // low, medium, high, critical
        private List<RiskFactor> riskFactors;
        private List<SecurityThreat> detectedThreats;
        private String recommendation; // allow, challenge, block, monitor
        private Boolean requiresManualReview;
        private OffsetDateTime assessmentTimestamp;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RiskFactor {
        private String factorType;
        private String factorName;
        private Integer severity; // 1-10
        private Double confidence; // 0.0-1.0
        private String description;
        private Map<String, Object> evidence;
    }

    @Data
    @Builder
    @Jacksonized
    public static class SecurityThreat {
        private String threatType; // bot, scraper, dos, fraud, injection
        private String threatName;
        private String severity; // low, medium, high, critical
        private Double confidence;
        private String description;
        private List<String> indicators;
        private String mitigation;
        private Boolean isBlocked;
    }

    @Data
    @Builder
    @Jacksonized
    public static class BehavioralAnalysis {
        private RequestPattern requestPattern;
        private SearchBehavior searchBehavior;
        private AnomalyDetection anomalyDetection;
        private UserProfile userProfile;
        private SessionAnalysis sessionAnalysis;
    }

    @Data
    @Builder
    @Jacksonized
    public static class RequestPattern {
        private Integer requestsPerMinute;
        private Integer requestsPerHour;
        private Integer requestsToday;
        private Double averageTimeBetweenRequests;
        private List<String> requestedEndpoints;
        private Boolean showsAutomatedBehavior;
        private Boolean hasUnusualPatterns;
        private List<String> suspiciousPatterns;
    }

    @Data
    @Builder
    @Jacksonized
    public static class SearchBehavior {
        private Integer totalSearches;
        private Integer uniqueQueries;
        private Double queryComplexity;
        private List<String> searchTerms;
        private Boolean showsScrapingBehavior;
        private Boolean hasRandomizedQueries;
        private Double searchVelocity;
        private List<String> behaviorIndicators;
    }

    @Data
    @Builder
    @Jacksonized
    public static class AnomalyDetection {
        private List<Anomaly> detectedAnomalies;
        private Double anomalyScore; // 0.0-1.0
        private String anomalyLevel; // none, low, medium, high
        private Boolean isAnomalous;
        private List<String> anomalyTypes;
    }

    @Data
    @Builder
    @Jacksonized
    public static class Anomaly {
        private String anomalyType;
        private String description;
        private Double score;
        private Map<String, Object> details;
        private OffsetDateTime detectedAt;
    }

    @Data
    @Builder
    @Jacksonized
    public static class UserProfile {
        private String userType; // legitimate, suspicious, malicious, unknown
        private String accountAge;
        private String activityLevel;
        private String verificationStatus;
        private List<String> trustIndicators;
        private List<String> suspiciousIndicators;
        private Double reputationScore; // 0.0-1.0
        private Boolean isWhitelisted;
        private Boolean isBlacklisted;
    }

    @Data
    @Builder
    @Jacksonized
    public static class SessionAnalysis {
        private Integer sessionDuration;
        private Integer pagesViewed;
        private Integer actionsPerformed;
        private String sessionType; // human, bot, hybrid
        private Boolean hasHumanBehavior;
        private Boolean hasBotBehavior;
        private List<String> interactionPatterns;
        private String sessionQuality;
    }

    @Data
    @Builder
    @Jacksonized
    public static class SecurityFlags {
        private Boolean suspiciousActivity;
        private Boolean maliciousIntent;
        private Boolean rateLimit;
        private Boolean requiresCaptcha;
        private Boolean requiresAuth;
        private Boolean blockRequest;
        private Boolean logSuspiciousActivity;
        private Boolean triggerAlert;
        private List<String> appliedMitigations;
        private Map<String, String> customFlags;
    }

    public enum ThreatType {
        BOT_ATTACK,
        SCRAPING_ATTEMPT,
        DOS_ATTACK,
        DDOS_ATTACK,
        SQL_INJECTION,
        XSS_ATTEMPT,
        CSRF_ATTACK,
        BRUTE_FORCE,
        CREDENTIAL_STUFFING,
        CLICK_FRAUD,
        PRICE_SCRAPING,
        DATA_HARVESTING,
        ACCOUNT_TAKEOVER,
        FRAUDULENT_BOOKING
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum SecurityAction {
        ALLOW,
        CHALLENGE,
        BLOCK,
        MONITOR,
        RATE_LIMIT,
        REQUIRE_AUTH,
        MANUAL_REVIEW
    }
}