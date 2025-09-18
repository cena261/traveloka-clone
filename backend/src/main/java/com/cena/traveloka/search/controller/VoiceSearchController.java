package com.cena.traveloka.search.controller;

import com.cena.traveloka.search.dto.*;
import com.cena.traveloka.search.service.VoiceSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/search/voice")
@RequiredArgsConstructor
@Slf4j
public class VoiceSearchController {

    private final VoiceSearchService voiceSearchService;

    @PostMapping("/search")
    public ResponseEntity<VoiceSearchResponse> voiceSearch(
            @RequestBody @Valid VoiceSearchRequest request,
            HttpServletRequest httpRequest) {

        log.info("Voice search request received for user: {}, session: {}",
                request.getUserId(), request.getSessionId());

        try {
            // Enrich request with HTTP context if needed
            enrichRequestWithContext(request, httpRequest);

            // Process voice search
            VoiceSearchResponse response = voiceSearchService.processVoiceSearch(request);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid voice search request", e);
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("Voice search processing failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/nlp")
    public ResponseEntity<VoiceSearchResponse> naturalLanguageQuery(
            @RequestParam String query,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String sessionId) {

        log.info("Natural language query received: '{}'", query);

        try {
            VoiceSearchResponse response = voiceSearchService.processNaturalLanguageQuery(
                    query, userId, sessionId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Natural language query processing failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/intents/analyze")
    public ResponseEntity<List<VoiceSearchResponse.IntentScore>> analyzeIntents(
            @RequestParam String query) {

        log.info("Analyzing intents for query: '{}'", query);

        try {
            List<VoiceSearchResponse.IntentScore> intents = voiceSearchService.analyzeSearchIntents(query);
            return ResponseEntity.ok(intents);

        } catch (Exception e) {
            log.error("Intent analysis failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/conversation")
    public ResponseEntity<VoiceSearchResponse.ConversationResponse> handleConversation(
            @RequestParam String conversationId,
            @RequestParam String userInput,
            @RequestBody(required = false) Map<String, Object> context) {

        log.info("Handling conversation flow for conversation: {}", conversationId);

        try {
            VoiceSearchResponse.ConversationResponse response = voiceSearchService.handleConversationFlow(
                    conversationId, userInput, context);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Conversation flow handling failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/speech/generate")
    public CompletableFuture<ResponseEntity<String>> generateSpeech(
            @RequestParam String text,
            @RequestBody(required = false) VoiceSearchRequest.LocationContext locationContext) {

        log.info("Generating speech for text length: {}", text.length());

        return voiceSearchService.generateSpeechResponse(text, locationContext)
                .thenApply(audioData -> {
                    if (audioData != null) {
                        return ResponseEntity.ok(audioData);
                    } else {
                        return ResponseEntity.internalServerError().build();
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Speech generation failed", throwable);
                    return ResponseEntity.internalServerError().build();
                });
    }

    private void enrichRequestWithContext(VoiceSearchRequest request, HttpServletRequest httpRequest) {
        // Add HTTP context information to the voice search request if needed
        if (request.getUserContext() == null) {
            request.setUserContext(VoiceSearchRequest.UserContext.builder().build());
        }

        VoiceSearchRequest.UserContext userContext = request.getUserContext();

        if (userContext.getIpAddress() == null) {
            userContext.setIpAddress(getClientIpAddress(httpRequest));
        }

        if (userContext.getUserAgent() == null) {
            userContext.setUserAgent(httpRequest.getHeader("User-Agent"));
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
}