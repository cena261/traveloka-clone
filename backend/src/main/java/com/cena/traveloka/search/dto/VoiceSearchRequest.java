package com.cena.traveloka.search.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
@Jacksonized
public class VoiceSearchRequest {

    // Audio data
    private String audioData; // Base64 encoded audio
    private AudioMetadata audioMetadata;

    // Natural language processing
    private String transcribedText;
    private String language;
    private String dialect;
    private NLPPreferences nlpPreferences;

    // User context
    private String userId;
    private String sessionId;
    private String deviceType;
    private LocationContext locationContext;

    // Search enhancement
    private ConversationContext conversationContext;
    private SearchIntent searchIntent;
    private Map<String, Object> customParameters;

    @Data
    @Builder
    @Jacksonized
    public static class AudioMetadata {
        private String format; // wav, mp3, m4a, flac
        private Integer sampleRate; // 16000, 44100, etc.
        private Integer bitDepth; // 16, 24, 32
        private Integer channels; // 1 (mono), 2 (stereo)
        private Long duration; // in milliseconds
        private Integer quality; // 1-10 scale
        private String encoding; // PCM, MP3, AAC
        private Double noiseLevel;
        private Boolean hasBackgroundNoise;
        private String recordingEnvironment; // quiet, noisy, outdoor, vehicle
    }

    @Data
    @Builder
    @Jacksonized
    public static class NLPPreferences {
        private Boolean enableIntentDetection;
        private Boolean enableEntityExtraction;
        private Boolean enableSentimentAnalysis;
        private Boolean enableContextAwareness;
        private Boolean enablePersonalization;
        private List<String> priorityIntents;
        private Map<String, Object> customNLPSettings;
        private String nlpModel; // standard, advanced, premium
    }

    @Data
    @Builder
    @Jacksonized
    public static class LocationContext {
        private Double latitude;
        private Double longitude;
        private String currentCity;
        private String currentCountry;
        private String timeZone;
        private String locationAccuracy; // high, medium, low
        private OffsetDateTime locationTimestamp;
        private String locationSource; // gps, network, manual
    }

    @Data
    @Builder
    @Jacksonized
    public static class ConversationContext {
        private String conversationId;
        private List<ConversationTurn> previousTurns;
        private Map<String, Object> contextVariables;
        private String conversationState; // new, ongoing, clarifying, completed
        private OffsetDateTime lastInteraction;
        private Integer turnNumber;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ConversationTurn {
        private Integer turnNumber;
        private String userInput;
        private String systemResponse;
        private Map<String, Object> extractedEntities;
        private String detectedIntent;
        private Double confidence;
        private OffsetDateTime timestamp;
    }

    @Data
    @Builder
    @Jacksonized
    public static class SearchIntent {
        private String primaryIntent; // search_hotels, find_flights, book_room, get_info
        private List<String> secondaryIntents;
        private Double confidence;
        private Map<String, Object> intentParameters;
        private List<ExtractedEntity> extractedEntities;
        private String urgencyLevel; // low, medium, high, urgent
        private Boolean requiresFollowUp;
    }

    @Data
    @Builder
    @Jacksonized
    public static class ExtractedEntity {
        private String entityType; // destination, date, duration, travelers, budget, amenity
        private String value;
        private String normalizedValue;
        private Double confidence;
        private String source; // speech, context, inference
        private Map<String, Object> metadata;
    }

    public enum VoiceSearchType {
        HOTEL_SEARCH,
        FLIGHT_SEARCH,
        GENERAL_INQUIRY,
        BOOKING_ASSISTANCE,
        TRAVEL_PLANNING,
        DESTINATION_INFO,
        PRICE_COMPARISON,
        RESERVATION_MANAGEMENT
    }

    public enum SpeechRecognitionEngine {
        GOOGLE_SPEECH_TO_TEXT,
        AWS_TRANSCRIBE,
        AZURE_SPEECH,
        WHISPER_OPENAI,
        CUSTOM_MODEL
    }

    public enum NLPProvider {
        OPENAI_GPT,
        GOOGLE_DIALOGFLOW,
        AWS_LEX,
        AZURE_LUIS,
        CUSTOM_NLP,
        RASA
    }
}