package com.cena.traveloka.search.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@Jacksonized
public class FraudDetectionResult {

    // Detection summary
    private String detectionId;
    private OffsetDateTime detectionTimestamp;
    private Boolean isFraudulent;
    private String fraudScore; // 0-100 or LOW/MEDIUM/HIGH/CRITICAL
    private Double confidence; // 0.0-1.0
    private String recommendation;

    // Fraud indicators
    private List<FraudIndicator> fraudIndicators;
    private List<FraudRule> triggeredRules;
    private FraudProfile fraudProfile;

    // Machine learning analysis
    private MLAnalysisResult mlAnalysis;

    // Pattern analysis
    private PatternAnalysis patternAnalysis;

    // Contextual analysis
    private ContextualAnalysis contextualAnalysis;

    // Mitigation actions
    private List<MitigationAction> recommendedActions;

    @Data
    @Builder
    @Jacksonized
    public static class FraudIndicator {
        private String indicatorType;
        private String indicatorName;
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        private Double weight; // Impact weight in overall score
        private String description;
        private Map<String, Object> evidence;
        private Boolean isPositive; // False positive or legitimate behavior
    }

    @Data
    @Builder
    @Jacksonized
    public static class FraudRule {
        private String ruleId;
        private String ruleName;
        private String ruleType; // VELOCITY, PATTERN, THRESHOLD, BLACKLIST, etc.
        private String severity;
        private String description;
        private Map<String, Object> ruleParameters;
        private Map<String, Object> triggeredValues;
        private Boolean isBlocking; // Whether this rule should block the request
    }

    @Data
    @Builder
    @Jacksonized
    public static class FraudProfile {
        private String profileType; // SCRAPER, BOT, FRAUDSTER, LEGITIMATE
        private String profileDescription;
        private Double matchScore; // How well the behavior matches this profile
        private List<String> profileCharacteristics;
        private String profileSource; // HISTORICAL, ML_MODEL, RULE_BASED
        private OffsetDateTime lastUpdated;
    }

    @Data
    @Builder
    @Jacksonized
    public static class MLAnalysisResult {
        private String modelVersion;
        private List<ModelPrediction> predictions;
        private FeatureAnalysis featureAnalysis;
        private Double modelConfidence;
        private String modelRecommendation;
        private Map<String, Double> probabilityScores;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ModelPrediction {
        private String modelName;
        private String modelType; // CLASSIFICATION, ANOMALY_DETECTION, CLUSTERING
        private String prediction;
        private Double probability;
        private String modelVersion;
        private Map<String, Object> modelFeatures;
    }

    @Data
    @Builder
    @Jacksonized
    public static class FeatureAnalysis {
        private Map<String, Double> featureImportance;
        private List<String> topFeatures;
        private Map<String, Object> featureValues;
        private List<String> anomalousFeatures;
    }

    @Data
    @Builder
    @Jacksonized
    public static class PatternAnalysis {
        private List<DetectedPattern> detectedPatterns;
        private SequenceAnalysis sequenceAnalysis;
        private TemporalAnalysis temporalAnalysis;
        private BehavioralPattern behavioralPattern;
    }

    @Data
    @Builder
    @Jacksonized
    public static class DetectedPattern {
        private String patternType; // REPETITIVE, SEQUENTIAL, RANDOMIZED, etc.
        private String patternName;
        private String description;
        private Double strength; // How strong the pattern is (0.0-1.0)
        private Map<String, Object> patternData;
        private Boolean isSuspicious;
    }

    @Data
    @Builder
    @Jacksonized
    public static class SequenceAnalysis {
        private List<String> actionSequence;
        private Boolean hasUnusualSequence;
        private String sequenceType; // HUMAN_LIKE, BOT_LIKE, MIXED
        private Double sequenceScore;
        private List<String> suspiciousTransitions;
    }

    @Data
    @Builder
    @Jacksonized
    public static class TemporalAnalysis {
        private String requestTiming; // HUMAN_LIKE, AUTOMATED, SUSPICIOUS
        private Double averageRequestInterval;
        private Boolean hasUniformTiming;
        private Boolean hasUnusualVelocity;
        private List<TimePattern> timePatterns;
    }

    @Data
    @Builder
    @Jacksonized
    public static class TimePattern {
        private String patternType; // BURST, UNIFORM, RANDOM, PERIODIC
        private OffsetDateTime startTime;
        private OffsetDateTime endTime;
        private Integer requestCount;
        private String description;
    }

    @Data
    @Builder
    @Jacksonized
    public static class BehavioralPattern {
        private String behaviorType; // EXPLORATION, FOCUSED_SEARCH, SCRAPING, RANDOM
        private String description;
        private Map<String, Object> behaviorMetrics;
        private Boolean isConsistent;
        private List<String> behaviorIndicators;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ContextualAnalysis {
        private GeographicContext geographicContext;
        private TemporalContext temporalContext;
        private DeviceContext deviceContext;
        private NetworkContext networkContext;
        private BusinessContext businessContext;
    }

    @Data
    @Builder
    @Jacksonized
    public static class GeographicContext {
        private Boolean isHighRiskCountry;
        private Boolean hasUnusualGeoPattern;
        private Boolean hasGeoInconsistency;
        private List<String> geoRiskFactors;
        private String riskLevel;
    }

    @Data
    @Builder
    @Jacksonized
    public static class TemporalContext {
        private String timeOfDay;
        private String dayOfWeek;
        private Boolean isUnusualTime;
        private Boolean isBusinessHours;
        private String seasonalContext;
        private List<String> temporalRiskFactors;
    }

    @Data
    @Builder
    @Jacksonized
    public static class DeviceContext {
        private Boolean isKnownDevice;
        private Boolean hasDeviceFingerprinting;
        private String deviceRiskLevel;
        private Boolean isCompromisedDevice;
        private List<String> deviceRiskFactors;
    }

    @Data
    @Builder
    @Jacksonized
    public static class NetworkContext {
        private String networkType; // RESIDENTIAL, DATACENTER, MOBILE, CORPORATE
        private Boolean isKnownBadNetwork;
        private Boolean isProxyNetwork;
        private Boolean isTorNetwork;
        private String networkRiskLevel;
        private List<String> networkRiskFactors;
    }

    @Data
    @Builder
    @Jacksonized
    public static class BusinessContext {
        private String businessImpact; // LOW, MEDIUM, HIGH
        private Boolean affectsRevenue;
        private Boolean affectsInventory;
        private String resourceConsumption; // LOW, MEDIUM, HIGH
        private List<String> businessRiskFactors;
    }

    @Data
    @Builder
    @Jacksonized
    public static class MitigationAction {
        private String actionType; // BLOCK, RATE_LIMIT, CAPTCHA, MONITOR, ALERT
        private String actionName;
        private String description;
        private String urgency; // LOW, MEDIUM, HIGH, IMMEDIATE
        private Map<String, Object> actionParameters;
        private String expectedOutcome;
        private Boolean isAutoExecuted;
    }

    // Fraud detection statistics
    @Data
    @Builder
    @Jacksonized
    public static class DetectionStats {
        private Integer totalRulesEvaluated;
        private Integer rulesTriggered;
        private Integer mlModelsUsed;
        private Long processingTimeMs;
        private Double falsePositiveRate;
        private Double truePositiveRate;
        private String detectionAccuracy;
    }

    // Historical context
    @Data
    @Builder
    @Jacksonized
    public static class HistoricalContext {
        private Boolean hasHistoryOfFraud;
        private Integer fraudAttemptCount;
        private OffsetDateTime lastFraudAttempt;
        private List<String> previousFraudTypes;
        private String riskProgression; // INCREASING, STABLE, DECREASING
    }

    // Real-time threat intelligence
    @Data
    @Builder
    @Jacksonized
    public static class ThreatIntelligence {
        private List<String> knownThreatActors;
        private List<String> threatCampaigns;
        private String threatLevel; // LOW, MEDIUM, HIGH, CRITICAL
        private Boolean isTargetedAttack;
        private List<String> threatIndicators;
        private OffsetDateTime intelligenceTimestamp;
    }

    public enum FraudType {
        ACCOUNT_TAKEOVER,
        CREDENTIAL_STUFFING,
        SYNTHETIC_IDENTITY,
        CARD_TESTING,
        PRICE_MANIPULATION,
        INVENTORY_HOARDING,
        REVIEW_MANIPULATION,
        CLICK_FRAUD,
        SCRAPING,
        BOT_ATTACK,
        DDOS_ATTACK,
        SOCIAL_ENGINEERING,
        PHISHING,
        MALWARE_DISTRIBUTION
    }

    public enum DetectionMethod {
        RULE_BASED,
        MACHINE_LEARNING,
        STATISTICAL_ANALYSIS,
        PATTERN_RECOGNITION,
        ANOMALY_DETECTION,
        BEHAVIORAL_ANALYSIS,
        DEVICE_FINGERPRINTING,
        NETWORK_ANALYSIS,
        THREAT_INTELLIGENCE,
        HYBRID
    }

    public enum ActionRecommendation {
        ALLOW,
        MONITOR,
        CHALLENGE,
        RATE_LIMIT,
        CAPTCHA,
        BLOCK_TEMPORARY,
        BLOCK_PERMANENT,
        MANUAL_REVIEW,
        ESCALATE,
        IGNORE
    }
}