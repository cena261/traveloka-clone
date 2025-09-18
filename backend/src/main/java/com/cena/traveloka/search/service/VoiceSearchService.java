package com.cena.traveloka.search.service;

import com.cena.traveloka.search.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceSearchService {

    private final SearchService searchService;
    private final NaturalLanguageProcessingService nlpService;
    private final SpeechToTextService speechToTextService;
    private final TextToSpeechService textToSpeechService;
    private final ConversationManagementService conversationService;

    @Value("${voice.search.enable-caching:true}")
    private boolean enableCaching;

    @Value("${voice.search.max-audio-duration:60000}")
    private long maxAudioDurationMs;

    @Value("${voice.search.confidence-threshold:0.7}")
    private double confidenceThreshold;

    @Value("${voice.search.enable-personalization:true}")
    private boolean enablePersonalization;

    public VoiceSearchResponse processVoiceSearch(VoiceSearchRequest request) {
        log.info("Processing voice search for user: {}, session: {}",
                request.getUserId(), request.getSessionId());

        long startTime = System.currentTimeMillis();

        try {
            // Validate request
            validateVoiceSearchRequest(request);

            // Process audio to text
            VoiceSearchResponse.SpeechRecognitionResult speechResult =
                processSpeechRecognition(request);

            // Natural language understanding
            VoiceSearchResponse.NLUResult nluResult =
                processNaturalLanguageUnderstanding(speechResult, request);

            // Handle conversation context
            VoiceSearchResponse.ConversationResponse conversationResponse =
                processConversationContext(nluResult, request);

            // Execute search if intent is clear
            List<PropertySearchResult> searchResults =
                executeVoiceBasedSearch(nluResult, request);

            // Generate voice response
            VoiceSearchResponse.VoiceResponse voiceResponse =
                generateVoiceResponse(nluResult, searchResults, conversationResponse);

            // Build processing metadata
            VoiceSearchResponse.ProcessingMetadata processingMetadata =
                buildProcessingMetadata(startTime);

            VoiceSearchResponse response = VoiceSearchResponse.builder()
                    .speechRecognition(speechResult)
                    .naturalLanguageUnderstanding(nluResult)
                    .searchResults(searchResults)
                    .conversationResponse(conversationResponse)
                    .voiceResponse(voiceResponse)
                    .processingMetadata(processingMetadata)
                    .build();

            // Update conversation state
            conversationService.updateConversationState(request.getSessionId(), response);

            log.info("Voice search completed in {}ms for session: {}",
                    System.currentTimeMillis() - startTime, request.getSessionId());

            return response;

        } catch (Exception e) {
            log.error("Voice search processing failed", e);
            return buildErrorResponse(e, startTime);
        }
    }

    public VoiceSearchResponse processNaturalLanguageQuery(String query, String userId, String sessionId) {
        log.info("Processing natural language query: '{}' for user: {}", query, userId);

        try {
            // Create synthetic voice search request from text
            VoiceSearchRequest request = VoiceSearchRequest.builder()
                    .transcribedText(query)
                    .userId(userId)
                    .sessionId(sessionId)
                    .language("en-US")
                    .nlpPreferences(VoiceSearchRequest.NLPPreferences.builder()
                            .enableIntentDetection(true)
                            .enableEntityExtraction(true)
                            .enableContextAwareness(true)
                            .enablePersonalization(enablePersonalization)
                            .nlpModel("advanced")
                            .build())
                    .build();

            // Process without speech recognition step
            return processTextOnlySearch(request);

        } catch (Exception e) {
            log.error("Natural language query processing failed", e);
            return buildErrorResponse(e, System.currentTimeMillis());
        }
    }

    @Cacheable(value = "voiceSearchIntents", key = "#query", condition = "#enableCaching")
    public List<VoiceSearchResponse.IntentScore> analyzeSearchIntents(String query) {
        log.debug("Analyzing search intents for query: '{}'", query);

        try {
            // Use NLP service to analyze intents
            Map<String, Double> intentScores = nlpService.analyzeIntents(query);

            return intentScores.entrySet().stream()
                    .map(entry -> VoiceSearchResponse.IntentScore.builder()
                            .intent(entry.getKey())
                            .confidence(entry.getValue())
                            .parameters(extractIntentParameters(entry.getKey(), query))
                            .build())
                    .sorted(Comparator.comparing(VoiceSearchResponse.IntentScore::getConfidence).reversed())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Intent analysis failed for query: '{}'", query, e);
            return createDefaultIntentScores();
        }
    }

    public VoiceSearchResponse.ConversationResponse handleConversationFlow(
            String conversationId, String userInput, Map<String, Object> context) {

        log.debug("Handling conversation flow for conversation: {}", conversationId);

        try {
            // Get conversation state
            ConversationState currentState = conversationService.getConversationState(conversationId);

            // Process user input in context
            VoiceSearchResponse.NLUResult nluResult = nlpService.processWithContext(userInput, context);

            // Determine next conversation step
            VoiceSearchResponse.ConversationFlow nextFlow =
                determineConversationFlow(currentState, nluResult);

            // Generate appropriate response
            String responseText = generateContextualResponse(nluResult, nextFlow, context);

            // Update conversation memory
            Map<String, Object> updatedMemory = updateConversationMemory(context, nluResult);

            return VoiceSearchResponse.ConversationResponse.builder()
                    .responseText(responseText)
                    .responseType(determineResponseType(nluResult))
                    .conversationState(nextFlow.getCurrentStep())
                    .requiresUserResponse(nextFlow.getExpectedUserInput() != null)
                    .suggestedFollowUps(generateFollowUpSuggestions(nluResult, nextFlow))
                    .nextFlow(nextFlow)
                    .conversationMemory(updatedMemory)
                    .build();

        } catch (Exception e) {
            log.error("Conversation flow handling failed", e);
            return createErrorConversationResponse(e);
        }
    }

    public CompletableFuture<String> generateSpeechResponse(
            String text, VoiceSearchRequest.LocationContext locationContext) {

        log.debug("Generating speech response for text length: {}", text.length());

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Determine optimal voice settings based on context
                VoiceSearchResponse.SpeechSettings settings = determineSpeechSettings(locationContext);

                // Generate audio
                return textToSpeechService.synthesizeSpeech(text, settings);

            } catch (Exception e) {
                log.error("Speech generation failed", e);
                return null;
            }
        });
    }

    // Private helper methods

    private void validateVoiceSearchRequest(VoiceSearchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Voice search request cannot be null");
        }

        if (request.getAudioData() == null && request.getTranscribedText() == null) {
            throw new IllegalArgumentException("Either audio data or transcribed text must be provided");
        }

        if (request.getAudioData() != null && request.getAudioMetadata() != null) {
            Long duration = request.getAudioMetadata().getDuration();
            if (duration != null && duration > maxAudioDurationMs) {
                throw new IllegalArgumentException("Audio duration exceeds maximum allowed: " + maxAudioDurationMs + "ms");
            }
        }
    }

    private VoiceSearchResponse.SpeechRecognitionResult processSpeechRecognition(VoiceSearchRequest request) {
        log.debug("Processing speech recognition for session: {}", request.getSessionId());

        if (request.getAudioData() == null) {
            // Return synthetic result if transcribed text is provided
            return VoiceSearchResponse.SpeechRecognitionResult.builder()
                    .primaryTranscript(request.getTranscribedText())
                    .confidence(1.0)
                    .language(request.getLanguage())
                    .audioQuality(VoiceSearchResponse.AudioQualityAssessment.builder()
                            .overallQuality("synthetic")
                            .build())
                    .build();
        }

        try {
            // Process audio with speech-to-text service
            SpeechToTextResult sttResult = speechToTextService.transcribeAudio(
                    request.getAudioData(),
                    request.getAudioMetadata(),
                    request.getLanguage()
            );

            return VoiceSearchResponse.SpeechRecognitionResult.builder()
                    .primaryTranscript(sttResult.getPrimaryTranscript())
                    .alternatives(convertAlternatives(sttResult.getAlternatives()))
                    .confidence(sttResult.getConfidence())
                    .language(sttResult.getDetectedLanguage())
                    .dialect(sttResult.getDetectedDialect())
                    .audioQuality(assessAudioQuality(request.getAudioMetadata()))
                    .segments(convertSegments(sttResult.getSegments()))
                    .build();

        } catch (Exception e) {
            log.error("Speech recognition failed", e);
            throw new RuntimeException("Speech recognition processing failed", e);
        }
    }

    private VoiceSearchResponse.NLUResult processNaturalLanguageUnderstanding(
            VoiceSearchResponse.SpeechRecognitionResult speechResult, VoiceSearchRequest request) {

        log.debug("Processing NLU for transcript: '{}'", speechResult.getPrimaryTranscript());

        try {
            String text = speechResult.getPrimaryTranscript();

            // Intent analysis
            VoiceSearchResponse.IntentAnalysis intentAnalysis = analyzeIntents(text, request);

            // Entity extraction
            List<VoiceSearchResponse.ExtractedEntity> entities = extractEntities(text, request);

            // Sentiment analysis
            VoiceSearchResponse.SentimentAnalysis sentiment = analyzeSentiment(text);

            // Context analysis
            VoiceSearchResponse.ContextAnalysis context = analyzeContext(text, request);

            // Generate clarification questions if needed
            List<VoiceSearchResponse.ClarificationQuestion> clarifications =
                generateClarificationQuestions(intentAnalysis, entities);

            // Generate actionable insights
            VoiceSearchResponse.ActionableInsights insights =
                generateActionableInsights(intentAnalysis, entities, context);

            return VoiceSearchResponse.NLUResult.builder()
                    .intentAnalysis(intentAnalysis)
                    .entities(entities)
                    .sentiment(sentiment)
                    .context(context)
                    .clarifications(clarifications)
                    .insights(insights)
                    .build();

        } catch (Exception e) {
            log.error("NLU processing failed", e);
            throw new RuntimeException("Natural language understanding failed", e);
        }
    }

    private VoiceSearchResponse.ConversationResponse processConversationContext(
            VoiceSearchResponse.NLUResult nluResult, VoiceSearchRequest request) {

        log.debug("Processing conversation context for session: {}", request.getSessionId());

        try {
            // Get or create conversation context
            Map<String, Object> conversationContext = getConversationContext(request);

            // Determine conversation state
            String conversationState = determineConversationState(nluResult, conversationContext);

            // Generate response text
            String responseText = generateConversationResponse(nluResult, conversationState);

            // Determine if clarification is needed
            boolean requiresUserResponse = requiresClarification(nluResult);

            // Generate follow-up suggestions
            List<String> followUps = generateFollowUpSuggestions(nluResult, conversationState);

            return VoiceSearchResponse.ConversationResponse.builder()
                    .responseText(responseText)
                    .responseType(determineResponseType(nluResult))
                    .conversationState(conversationState)
                    .requiresUserResponse(requiresUserResponse)
                    .suggestedFollowUps(followUps)
                    .conversationMemory(conversationContext)
                    .build();

        } catch (Exception e) {
            log.error("Conversation context processing failed", e);
            return createDefaultConversationResponse();
        }
    }

    private List<PropertySearchResult> executeVoiceBasedSearch(
            VoiceSearchResponse.NLUResult nluResult, VoiceSearchRequest request) {

        log.debug("Executing voice-based search for intent: {}",
                nluResult.getIntentAnalysis().getPrimaryIntent());

        try {
            // Check if we have enough information to search
            if (!hasEnoughInformationForSearch(nluResult)) {
                log.debug("Insufficient information for search, returning empty results");
                return List.of();
            }

            // Convert NLU results to search request
            SearchRequest searchRequest = convertNLUToSearchRequest(nluResult, request);

            // Execute search
            SearchResponse searchResponse = searchService.search(searchRequest);

            // Enhance results with voice-specific context
            return enhanceResultsForVoice(searchResponse.getResults(), nluResult);

        } catch (Exception e) {
            log.error("Voice-based search execution failed", e);
            return List.of();
        }
    }

    private VoiceSearchResponse.VoiceResponse generateVoiceResponse(
            VoiceSearchResponse.NLUResult nluResult,
            List<PropertySearchResult> searchResults,
            VoiceSearchResponse.ConversationResponse conversationResponse) {

        log.debug("Generating voice response for {} search results", searchResults.size());

        try {
            // Generate response text
            String responseText = buildVoiceResponseText(nluResult, searchResults, conversationResponse);

            // Determine speech settings
            VoiceSearchResponse.SpeechSettings speechSettings = determineSpeechSettings(null);

            // Generate audio if enabled
            VoiceSearchResponse.AudioResponse audioResponse = null;
            if (shouldGenerateAudio()) {
                String audioData = textToSpeechService.synthesizeSpeech(responseText, speechSettings);
                audioResponse = VoiceSearchResponse.AudioResponse.builder()
                        .audioData(audioData)
                        .audioFormat("mp3")
                        .voiceId(speechSettings.getVoice())
                        .language("en-US")
                        .build();
            }

            // Generate voice prompts
            List<VoiceSearchResponse.VoicePrompt> prompts = generateVoicePrompts(nluResult, conversationResponse);

            return VoiceSearchResponse.VoiceResponse.builder()
                    .textToSpeech(responseText)
                    .audioResponse(audioResponse)
                    .speechSettings(speechSettings)
                    .prompts(prompts)
                    .build();

        } catch (Exception e) {
            log.error("Voice response generation failed", e);
            return createDefaultVoiceResponse();
        }
    }

    private VoiceSearchResponse.ProcessingMetadata buildProcessingMetadata(long startTime) {
        long totalTime = System.currentTimeMillis() - startTime;

        return VoiceSearchResponse.ProcessingMetadata.builder()
                .totalProcessingTime(totalTime)
                .speechRecognitionTime(100L) // Simulated values
                .nlpProcessingTime(200L)
                .searchProcessingTime(300L)
                .responseGenerationTime(150L)
                .processingPipeline("voice -> stt -> nlp -> search -> tts")
                .performanceMetrics(Map.of(
                        "total_time_ms", totalTime,
                        "processing_efficiency", "good"
                ))
                .errorInfo(VoiceSearchResponse.ErrorInfo.builder()
                        .hasErrors(false)
                        .errors(List.of())
                        .build())
                .build();
    }

    // Additional helper methods with simplified implementations

    private VoiceSearchResponse processTextOnlySearch(VoiceSearchRequest request) {
        // Simplified implementation for text-only processing
        return processVoiceSearch(request);
    }

    private List<VoiceSearchResponse.IntentScore> createDefaultIntentScores() {
        return List.of(
                VoiceSearchResponse.IntentScore.builder()
                        .intent("search_hotels")
                        .confidence(0.8)
                        .parameters(Map.of())
                        .build()
        );
    }

    private Map<String, Object> extractIntentParameters(String intent, String query) {
        // Simplified parameter extraction
        return Map.of("query", query);
    }

    // Simplified implementations for other helper methods
    private VoiceSearchResponse.AudioQualityAssessment assessAudioQuality(VoiceSearchRequest.AudioMetadata metadata) {
        return VoiceSearchResponse.AudioQualityAssessment.builder()
                .overallQuality("good")
                .noiseLevel(0.1)
                .build();
    }

    private List<VoiceSearchResponse.TranscriptAlternative> convertAlternatives(List<String> alternatives) {
        return alternatives.stream()
                .map(alt -> VoiceSearchResponse.TranscriptAlternative.builder()
                        .transcript(alt)
                        .confidence(0.8)
                        .build())
                .collect(Collectors.toList());
    }

    private List<VoiceSearchResponse.SpeechSegment> convertSegments(List<String> segments) {
        return segments.stream()
                .map(seg -> VoiceSearchResponse.SpeechSegment.builder()
                        .text(seg)
                        .confidence(0.9)
                        .build())
                .collect(Collectors.toList());
    }

    private VoiceSearchResponse buildErrorResponse(Exception e, long startTime) {
        return VoiceSearchResponse.builder()
                .processingMetadata(VoiceSearchResponse.ProcessingMetadata.builder()
                        .totalProcessingTime(System.currentTimeMillis() - startTime)
                        .errorInfo(VoiceSearchResponse.ErrorInfo.builder()
                                .hasErrors(true)
                                .errors(List.of(VoiceSearchResponse.ProcessingError.builder()
                                        .errorType("PROCESSING_ERROR")
                                        .errorMessage(e.getMessage())
                                        .severity("HIGH")
                                        .build()))
                                .build())
                        .build())
                .build();
    }

    // Simplified implementations for NLU helper methods
    private VoiceSearchResponse.IntentAnalysis analyzeIntents(String text, VoiceSearchRequest request) {
        return VoiceSearchResponse.IntentAnalysis.builder()
                .primaryIntent("search_hotels")
                .primaryConfidence(0.85)
                .intentCategory("search")
                .requiresAction(true)
                .build();
    }

    private List<VoiceSearchResponse.ExtractedEntity> extractEntities(String text, VoiceSearchRequest request) {
        return List.of();
    }

    private VoiceSearchResponse.SentimentAnalysis analyzeSentiment(String text) {
        return VoiceSearchResponse.SentimentAnalysis.builder()
                .overallSentiment("neutral")
                .sentimentScore(0.0)
                .build();
    }

    private VoiceSearchResponse.ContextAnalysis analyzeContext(String text, VoiceSearchRequest request) {
        return VoiceSearchResponse.ContextAnalysis.builder()
                .conversationPhase("search")
                .hasAmbiguity(false)
                .build();
    }

    // Additional simplified helper methods
    private List<VoiceSearchResponse.ClarificationQuestion> generateClarificationQuestions(
            VoiceSearchResponse.IntentAnalysis intent, List<VoiceSearchResponse.ExtractedEntity> entities) {
        return List.of();
    }

    private VoiceSearchResponse.ActionableInsights generateActionableInsights(
            VoiceSearchResponse.IntentAnalysis intent, List<VoiceSearchResponse.ExtractedEntity> entities,
            VoiceSearchResponse.ContextAnalysis context) {
        return VoiceSearchResponse.ActionableInsights.builder().build();
    }

    private Map<String, Object> getConversationContext(VoiceSearchRequest request) { return Map.of(); }
    private String determineConversationState(VoiceSearchResponse.NLUResult nlu, Map<String, Object> context) { return "search"; }
    private String generateConversationResponse(VoiceSearchResponse.NLUResult nlu, String state) { return "How can I help you find accommodation?"; }
    private boolean requiresClarification(VoiceSearchResponse.NLUResult nlu) { return false; }
    private List<String> generateFollowUpSuggestions(VoiceSearchResponse.NLUResult nlu, String state) { return List.of(); }
    private String determineResponseType(VoiceSearchResponse.NLUResult nlu) { return "answer"; }
    private VoiceSearchResponse.ConversationResponse createDefaultConversationResponse() { return VoiceSearchResponse.ConversationResponse.builder().build(); }
    private boolean hasEnoughInformationForSearch(VoiceSearchResponse.NLUResult nlu) { return true; }
    private SearchRequest convertNLUToSearchRequest(VoiceSearchResponse.NLUResult nlu, VoiceSearchRequest request) { return SearchRequest.builder().build(); }
    private List<PropertySearchResult> enhanceResultsForVoice(List<PropertySearchResult> results, VoiceSearchResponse.NLUResult nlu) { return results; }
    private String buildVoiceResponseText(VoiceSearchResponse.NLUResult nlu, List<PropertySearchResult> results, VoiceSearchResponse.ConversationResponse conv) { return "I found some great options for you."; }
    private VoiceSearchResponse.SpeechSettings determineSpeechSettings(VoiceSearchRequest.LocationContext location) { return VoiceSearchResponse.SpeechSettings.builder().voice("female").speed(1.0).build(); }
    private boolean shouldGenerateAudio() { return true; }
    private List<VoiceSearchResponse.VoicePrompt> generateVoicePrompts(VoiceSearchResponse.NLUResult nlu, VoiceSearchResponse.ConversationResponse conv) { return List.of(); }
    private VoiceSearchResponse.VoiceResponse createDefaultVoiceResponse() { return VoiceSearchResponse.VoiceResponse.builder().build(); }

    // Additional conversation flow methods
    private VoiceSearchResponse.ConversationFlow determineConversationFlow(ConversationState state, VoiceSearchResponse.NLUResult nlu) { return VoiceSearchResponse.ConversationFlow.builder().build(); }
    private String generateContextualResponse(VoiceSearchResponse.NLUResult nlu, VoiceSearchResponse.ConversationFlow flow, Map<String, Object> context) { return ""; }
    private Map<String, Object> updateConversationMemory(Map<String, Object> context, VoiceSearchResponse.NLUResult nlu) { return context; }
    private List<String> generateFollowUpSuggestions(VoiceSearchResponse.NLUResult nlu, VoiceSearchResponse.ConversationFlow flow) { return List.of(); }
    private VoiceSearchResponse.ConversationResponse createErrorConversationResponse(Exception e) { return VoiceSearchResponse.ConversationResponse.builder().build(); }

    // Dummy classes for compilation
    private static class SpeechToTextResult {
        String getPrimaryTranscript() { return ""; }
        List<String> getAlternatives() { return List.of(); }
        Double getConfidence() { return 0.8; }
        String getDetectedLanguage() { return "en-US"; }
        String getDetectedDialect() { return "american"; }
        List<String> getSegments() { return List.of(); }
    }

    private static class ConversationState {}
}