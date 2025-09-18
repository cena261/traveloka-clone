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
public class VoiceSearchResponse {

    // Speech recognition results
    private SpeechRecognitionResult speechRecognition;

    // Natural language understanding
    private NLUResult naturalLanguageUnderstanding;

    // Search results
    private List<PropertySearchResult> searchResults;

    // Conversation management
    private ConversationResponse conversationResponse;

    // Voice response
    private VoiceResponse voiceResponse;

    // Processing metadata
    private ProcessingMetadata processingMetadata;

    @Data
    @Builder
    @Jacksonized
    public static class SpeechRecognitionResult {
        private String primaryTranscript;
        private List<TranscriptAlternative> alternatives;
        private Double confidence;
        private String language;
        private String dialect;
        private AudioQualityAssessment audioQuality;
        private List<SpeechSegment> segments;
        private Map<String, Object> recognitionMetadata;
    }

    @Data
    @Builder
    @Jacksonized
    public static class TranscriptAlternative {
        private String transcript;
        private Double confidence;
        private List<WordInfo> words;
    }

    @Data
    @Builder
    @Jacksonized
    public static class WordInfo {
        private String word;
        private Double confidence;
        private Long startTime; // milliseconds
        private Long endTime; // milliseconds
        private String speakerTag;
    }

    @Data
    @Builder
    @Jacksonized
    public static class AudioQualityAssessment {
        private String overallQuality; // excellent, good, fair, poor
        private Double noiseLevel;
        private Double signalToNoiseRatio;
        private Boolean hasClipping;
        private Boolean hasDistortion;
        private String recommendedAction;
        private List<String> qualityIssues;
    }

    @Data
    @Builder
    @Jacksonized
    public static class SpeechSegment {
        private String text;
        private Long startTime;
        private Long endTime;
        private Double confidence;
        private String segmentType; // speech, silence, noise
    }

    @Data
    @Builder
    @Jacksonized
    public static class NLUResult {
        private IntentAnalysis intentAnalysis;
        private List<ExtractedEntity> entities;
        private SentimentAnalysis sentiment;
        private ContextAnalysis context;
        private List<ClarificationQuestion> clarifications;
        private ActionableInsights insights;
    }

    @Data
    @Builder
    @Jacksonized
    public static class IntentAnalysis {
        private String primaryIntent;
        private Double primaryConfidence;
        private List<IntentScore> alternativeIntents;
        private String intentCategory;
        private Boolean requiresAction;
        private List<String> missingParameters;
        private String complexityLevel;
    }

    @Data
    @Builder
    @Jacksonized
    public static class IntentScore {
        private String intent;
        private Double confidence;
        private Map<String, Object> parameters;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ExtractedEntity {
        private String entityType;
        private String value;
        private String normalizedValue;
        private Double confidence;
        private String source;
        private EntityValidation validation;
        private List<String> suggestedCorrections;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @Jacksonized
    public static class EntityValidation {
        private Boolean isValid;
        private String validationStatus;
        private List<String> validationErrors;
        private String correctedValue;
        private Double correctionConfidence;
    }

    @Data
    @Builder
    @Jacksonized
    public static class SentimentAnalysis {
        private String overallSentiment; // positive, negative, neutral
        private Double sentimentScore; // -1.0 to 1.0
        private Map<String, Double> emotionScores;
        private String urgencyLevel;
        private List<String> sentimentIndicators;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ContextAnalysis {
        private String conversationPhase; // greeting, search, clarification, booking, farewell
        private Map<String, Object> contextVariables;
        private List<String> inferredPreferences;
        private String userExpertiseLevel; // novice, intermediate, expert
        private Boolean hasAmbiguity;
        private List<String> ambiguities;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ClarificationQuestion {
        private String questionId;
        private String question;
        private String questionType; // choice, open_ended, confirmation, disambiguation
        private List<String> options;
        private String priority; // high, medium, low
        private Boolean isRequired;
        private String context;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ActionableInsights {
        private List<String> detectedPreferences;
        private List<String> recommendedActions;
        private Map<String, Object> personalizedSuggestions;
        private String nextBestAction;
        private List<String> potentialUpsells;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ConversationResponse {
        private String responseText;
        private String responseType; // answer, clarification, suggestion, confirmation
        private String conversationState;
        private Boolean requiresUserResponse;
        private List<String> suggestedFollowUps;
        private ConversationFlow nextFlow;
        private Map<String, Object> conversationMemory;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ConversationFlow {
        private String flowId;
        private String flowName;
        private String currentStep;
        private List<String> availableActions;
        private Map<String, Object> flowContext;
        private String expectedUserInput;
    }

    @Data
    @Builder
    @Jacksonized
    public static class VoiceResponse {
        private String textToSpeech;
        private AudioResponse audioResponse;
        private SpeechSettings speechSettings;
        private List<VoicePrompt> prompts;
    }

    @Data
    @Builder
    @Jacksonized
    public static class AudioResponse {
        private String audioData; // Base64 encoded
        private String audioFormat;
        private Integer duration;
        private String voiceId;
        private String language;
        private Map<String, Object> audioMetadata;
    }

    @Data
    @Builder
    @Jacksonized
    public static class SpeechSettings {
        private String voice; // male, female, neutral
        private String accent; // american, british, australian, etc.
        private Double speed; // 0.5 - 2.0
        private Double pitch; // -20 to 20
        private String emotion; // neutral, friendly, professional, excited
        private String style; // conversational, news, customerservice
    }

    @Data
    @Builder
    @Jacksonized
    public static class VoicePrompt {
        private String promptType; // question, confirmation, suggestion, error
        private String text;
        private List<String> expectedResponses;
        private String priority;
        private Long timeoutSeconds;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ProcessingMetadata {
        private Long totalProcessingTime;
        private Long speechRecognitionTime;
        private Long nlpProcessingTime;
        private Long searchProcessingTime;
        private Long responseGenerationTime;
        private String processingPipeline;
        private Map<String, Object> performanceMetrics;
        private List<ProcessingStep> processingSteps;
        private ErrorInfo errorInfo;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ProcessingStep {
        private String stepName;
        private String status;
        private Long duration;
        private Map<String, Object> stepMetadata;
        private String errorMessage;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ErrorInfo {
        private Boolean hasErrors;
        private List<ProcessingError> errors;
        private String fallbackStrategy;
        private Boolean partialResults;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ProcessingError {
        private String errorType;
        private String errorMessage;
        private String errorCode;
        private String severity;
        private String component;
        private Map<String, Object> errorContext;
    }

    // Enhanced search result with voice-specific context
    @Data
    @Builder
    @Jacksonized
    public static class VoiceSearchResult extends PropertySearchResult {
        private String voiceDescription;
        private String pronunciationGuide;
        private List<String> keyHighlights;
        private String bookingInstructions;
        private Double voiceRelevanceScore;
        private Map<String, Object> voiceMetadata;
    }

    public enum ResponseStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        CLARIFICATION_NEEDED,
        INSUFFICIENT_AUDIO_QUALITY,
        TRANSCRIPTION_FAILED,
        NLP_PROCESSING_FAILED,
        SEARCH_FAILED,
        TIMEOUT,
        ERROR
    }

    public enum ConversationState {
        NEW,
        IN_PROGRESS,
        CLARIFICATION_PENDING,
        SEARCH_RESULTS_PRESENTED,
        BOOKING_INITIATED,
        COMPLETED,
        ABANDONED,
        ERROR
    }
}