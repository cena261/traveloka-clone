package com.cena.traveloka.search.service;

import com.cena.traveloka.search.dto.VoiceSearchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SpeechToTextService {

    public SpeechToTextResult transcribeAudio(String audioData, VoiceSearchRequest.AudioMetadata metadata, String language) {
        // Mock implementation - in production, integrate with real speech-to-text service
        return new SpeechToTextResult("Sample transcription result", java.util.List.of("Alternative 1", "Alternative 2"), 0.85, "en-US", "american", java.util.List.of("sample", "text"));
    }

    public static class SpeechToTextResult {
        private final String primaryTranscript;
        private final java.util.List<String> alternatives;
        private final Double confidence;
        private final String detectedLanguage;
        private final String detectedDialect;
        private final java.util.List<String> segments;

        public SpeechToTextResult(String primaryTranscript, java.util.List<String> alternatives, Double confidence, String detectedLanguage, String detectedDialect, java.util.List<String> segments) {
            this.primaryTranscript = primaryTranscript;
            this.alternatives = alternatives;
            this.confidence = confidence;
            this.detectedLanguage = detectedLanguage;
            this.detectedDialect = detectedDialect;
            this.segments = segments;
        }

        public String getPrimaryTranscript() { return primaryTranscript; }
        public java.util.List<String> getAlternatives() { return alternatives; }
        public Double getConfidence() { return confidence; }
        public String getDetectedLanguage() { return detectedLanguage; }
        public String getDetectedDialect() { return detectedDialect; }
        public java.util.List<String> getSegments() { return segments; }
    }
}