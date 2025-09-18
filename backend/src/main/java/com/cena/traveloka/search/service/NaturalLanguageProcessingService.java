package com.cena.traveloka.search.service;

import com.cena.traveloka.search.dto.VoiceSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NaturalLanguageProcessingService {

    @Value("${nlp.confidence.threshold:0.7}")
    private double confidenceThreshold;

    @Value("${nlp.enable.advanced.features:true}")
    private boolean enableAdvancedFeatures;

    private static final Map<String, List<String>> INTENT_PATTERNS = Map.of(
            "search_hotels", List.of(
                    "find.*hotel", "search.*accommodation", "book.*room", "looking.*stay",
                    "need.*place.*stay", "hotel.*near", "accommodation.*in", "where.*stay"
            ),
            "search_flights", List.of(
                    "find.*flight", "book.*flight", "fly.*to", "flight.*from.*to",
                    "travel.*to", "plane.*ticket", "airline.*ticket"
            ),
            "price_inquiry", List.of(
                    "how.*much", "what.*price", "cost.*of", "expensive", "cheap.*option",
                    "budget.*friendly", "price.*range"
            ),
            "availability_check", List.of(
                    "available", "vacancy", "free.*room", "book.*now", "reserve"
            ),
            "location_inquiry", List.of(
                    "where.*is", "location.*of", "address", "how.*get.*to", "directions"
            ),
            "amenity_inquiry", List.of(
                    "does.*have", "include.*wifi", "pool", "breakfast", "gym", "spa",
                    "parking", "what.*amenities", "facilities"
            ),
            "cancellation_inquiry", List.of(
                    "cancel.*booking", "refund", "change.*reservation", "modify.*booking"
            )
    );

    private static final Map<String, List<String>> ENTITY_PATTERNS = Map.of(
            "destination", List.of(
                    "in\\s+(\\w+(?:\\s+\\w+)*)", "to\\s+(\\w+(?:\\s+\\w+)*)",
                    "near\\s+(\\w+(?:\\s+\\w+)*)", "at\\s+(\\w+(?:\\s+\\w+)*)"
            ),
            "date", List.of(
                    "\\b(\\d{1,2}/\\d{1,2}/\\d{4})\\b", "\\b(\\d{1,2}-\\d{1,2}-\\d{4})\\b",
                    "\\b(january|february|march|april|may|june|july|august|september|october|november|december)\\s+\\d{1,2}\\b",
                    "\\b(today|tomorrow|next week|this weekend)\\b",
                    "\\b(\\d{1,2})\\s+(january|february|march|april|may|june|july|august|september|october|november|december)\\b"
            ),
            "duration", List.of(
                    "for\\s+(\\d+)\\s+nights?", "\\b(\\d+)\\s+days?\\b", "\\b(\\d+)\\s+weeks?\\b"
            ),
            "travelers", List.of(
                    "\\b(\\d+)\\s+(?:people|guests|adults|persons)\\b",
                    "\\b(\\d+)\\s+(?:children|kids)\\b", "family\\s+of\\s+(\\d+)"
            ),
            "budget", List.of(
                    "under\\s+\\$?(\\d+)", "budget.*\\$?(\\d+)", "less.*than.*\\$?(\\d+)",
                    "maximum.*\\$?(\\d+)", "up.*to.*\\$?(\\d+)"
            ),
            "room_type", List.of(
                    "\\b(single|double|twin|king|queen|suite|deluxe|standard)\\s+(?:room|bed)\\b",
                    "\\b(presidential|executive|junior)\\s+suite\\b"
            ),
            "amenity", List.of(
                    "\\b(wifi|internet|pool|spa|gym|fitness|restaurant|bar|breakfast|parking)\\b",
                    "\\b(air\\s+conditioning|ac)\\b", "\\b(room\\s+service)\\b"
            )
    );

    public Map<String, Double> analyzeIntents(String text) {
        log.debug("Analyzing intents for text: '{}'", text);

        Map<String, Double> intentScores = new HashMap<>();
        String lowerText = text.toLowerCase();

        for (Map.Entry<String, List<String>> entry : INTENT_PATTERNS.entrySet()) {
            String intent = entry.getKey();
            List<String> patterns = entry.getValue();

            double maxScore = 0.0;
            for (String patternStr : patterns) {
                Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
                if (pattern.matcher(text).find()) {
                    maxScore = Math.max(maxScore, calculateIntentConfidence(patternStr, text));
                }
            }

            if (maxScore > 0) {
                intentScores.put(intent, maxScore);
            }
        }

        // Apply contextual boosting
        intentScores = applyContextualBoosting(intentScores, lowerText);

        log.debug("Intent analysis results: {}", intentScores);
        return intentScores;
    }

    public List<VoiceSearchResponse.ExtractedEntity> extractEntities(String text) {
        log.debug("Extracting entities from text: '{}'", text);

        List<VoiceSearchResponse.ExtractedEntity> entities = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : ENTITY_PATTERNS.entrySet()) {
            String entityType = entry.getKey();
            List<String> patterns = entry.getValue();

            for (String patternStr : patterns) {
                Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
                var matcher = pattern.matcher(text);

                while (matcher.find()) {
                    String extractedValue = matcher.group(1);
                    if (extractedValue != null && !extractedValue.trim().isEmpty()) {
                        entities.add(VoiceSearchResponse.ExtractedEntity.builder()
                                .entityType(entityType)
                                .value(extractedValue.trim())
                                .normalizedValue(normalizeEntityValue(entityType, extractedValue))
                                .confidence(calculateEntityConfidence(entityType, extractedValue))
                                .source("pattern_matching")
                                .validation(validateEntity(entityType, extractedValue))
                                .build());
                    }
                }
            }
        }

        // Remove duplicates and sort by confidence
        entities = deduplicateAndSortEntities(entities);

        log.debug("Extracted {} entities", entities.size());
        return entities;
    }

    public VoiceSearchResponse.SentimentAnalysis analyzeSentiment(String text) {
        log.debug("Analyzing sentiment for text: '{}'", text);

        Map<String, Integer> positiveWords = Map.of(
                "great", 2, "excellent", 3, "amazing", 3, "perfect", 3, "love", 2,
                "good", 1, "nice", 1, "beautiful", 2, "wonderful", 3, "fantastic", 3
        );

        Map<String, Integer> negativeWords = Map.of(
                "terrible", -3, "awful", -3, "horrible", -3, "bad", -2, "poor", -2,
                "disappointing", -2, "worst", -3, "hate", -3, "disgusting", -3
        );

        Map<String, Integer> urgencyWords = Map.of(
                "urgent", 3, "asap", 3, "immediately", 3, "quickly", 2, "soon", 1,
                "now", 2, "today", 2, "tonight", 2
        );

        String[] words = text.toLowerCase().split("\\s+");
        int sentimentScore = 0;
        int urgencyScore = 0;

        for (String word : words) {
            sentimentScore += positiveWords.getOrDefault(word, 0);
            sentimentScore += negativeWords.getOrDefault(word, 0);
            urgencyScore += urgencyWords.getOrDefault(word, 0);
        }

        String overallSentiment = sentimentScore > 1 ? "positive" :
                sentimentScore < -1 ? "negative" : "neutral";

        String urgencyLevel = urgencyScore > 3 ? "urgent" :
                urgencyScore > 1 ? "high" :
                urgencyScore > 0 ? "medium" : "low";

        Map<String, Double> emotionScores = analyzeEmotions(text);

        return VoiceSearchResponse.SentimentAnalysis.builder()
                .overallSentiment(overallSentiment)
                .sentimentScore((double) sentimentScore / 10.0) // Normalize to -1.0 to 1.0
                .emotionScores(emotionScores)
                .urgencyLevel(urgencyLevel)
                .sentimentIndicators(identifySentimentIndicators(text))
                .build();
    }

    @Cacheable(value = "nlpContext", key = "#text.hashCode()")
    public VoiceSearchResponse.ContextAnalysis analyzeContext(String text, Map<String, Object> conversationHistory) {
        log.debug("Analyzing context for text: '{}'", text);

        // Determine conversation phase
        String conversationPhase = determineConversationPhase(text, conversationHistory);

        // Extract context variables
        Map<String, Object> contextVariables = extractContextVariables(text, conversationHistory);

        // Infer preferences
        List<String> inferredPreferences = inferUserPreferences(text, conversationHistory);

        // Assess user expertise level
        String userExpertiseLevel = assessUserExpertiseLevel(text, conversationHistory);

        // Check for ambiguities
        List<String> ambiguities = identifyAmbiguities(text);

        return VoiceSearchResponse.ContextAnalysis.builder()
                .conversationPhase(conversationPhase)
                .contextVariables(contextVariables)
                .inferredPreferences(inferredPreferences)
                .userExpertiseLevel(userExpertiseLevel)
                .hasAmbiguity(!ambiguities.isEmpty())
                .ambiguities(ambiguities)
                .build();
    }

    public VoiceSearchResponse.NLUResult processWithContext(String text, Map<String, Object> context) {
        log.debug("Processing text with context: '{}'", text);

        try {
            // Analyze intents with context
            Map<String, Double> intentScores = analyzeIntentsWithContext(text, context);
            VoiceSearchResponse.IntentAnalysis intentAnalysis = buildIntentAnalysis(intentScores);

            // Extract entities with context
            List<VoiceSearchResponse.ExtractedEntity> entities = extractEntitiesWithContext(text, context);

            // Analyze sentiment
            VoiceSearchResponse.SentimentAnalysis sentiment = analyzeSentiment(text);

            // Analyze context
            VoiceSearchResponse.ContextAnalysis contextAnalysis = analyzeContext(text, context);

            // Generate clarifications
            List<VoiceSearchResponse.ClarificationQuestion> clarifications =
                    generateClarificationQuestions(intentAnalysis, entities, context);

            // Generate insights
            VoiceSearchResponse.ActionableInsights insights =
                    generateActionableInsights(intentAnalysis, entities, contextAnalysis);

            return VoiceSearchResponse.NLUResult.builder()
                    .intentAnalysis(intentAnalysis)
                    .entities(entities)
                    .sentiment(sentiment)
                    .context(contextAnalysis)
                    .clarifications(clarifications)
                    .insights(insights)
                    .build();

        } catch (Exception e) {
            log.error("NLP processing with context failed", e);
            throw new RuntimeException("NLP processing failed", e);
        }
    }

    public List<String> generateSearchQueries(String naturalLanguageInput) {
        log.debug("Generating search queries from natural language: '{}'", naturalLanguageInput);

        List<String> queries = new ArrayList<>();

        // Extract key terms
        List<String> keyTerms = extractKeyTerms(naturalLanguageInput);

        // Generate different query variations
        queries.add(naturalLanguageInput); // Original
        queries.add(String.join(" ", keyTerms)); // Key terms only
        queries.add(generateSimplifiedQuery(naturalLanguageInput)); // Simplified
        queries.addAll(generateSynonymQueries(keyTerms)); // Synonym variations

        // Remove duplicates and empty queries
        return queries.stream()
                .filter(q -> q != null && !q.trim().isEmpty())
                .distinct()
                .limit(5)
                .collect(Collectors.toList());
    }

    // Private helper methods

    private double calculateIntentConfidence(String pattern, String text) {
        // Calculate confidence based on pattern complexity and text length
        double baseConfidence = 0.8;
        double lengthFactor = Math.min(1.0, text.length() / 50.0);
        double patternComplexity = pattern.length() / 20.0;

        return Math.min(0.95, baseConfidence * lengthFactor * patternComplexity);
    }

    private Map<String, Double> applyContextualBoosting(Map<String, Double> intentScores, String text) {
        Map<String, Double> boostedScores = new HashMap<>(intentScores);

        // Boost hotel search if travel-related keywords are present
        if (text.contains("vacation") || text.contains("trip") || text.contains("travel")) {
            boostedScores.computeIfPresent("search_hotels", (k, v) -> Math.min(0.95, v * 1.2));
        }

        // Boost price inquiry if budget keywords are present
        if (text.contains("cheap") || text.contains("budget") || text.contains("affordable")) {
            boostedScores.computeIfPresent("price_inquiry", (k, v) -> Math.min(0.95, v * 1.3));
        }

        return boostedScores;
    }

    private String normalizeEntityValue(String entityType, String value) {
        return switch (entityType) {
            case "destination" -> value.toLowerCase().trim();
            case "date" -> normalizeDateValue(value);
            case "duration" -> normalizeDurationValue(value);
            case "budget" -> normalizeBudgetValue(value);
            default -> value.trim();
        };
    }

    private double calculateEntityConfidence(String entityType, String value) {
        // Calculate confidence based on entity type and value characteristics
        return switch (entityType) {
            case "destination" -> value.length() > 2 ? 0.9 : 0.6;
            case "date" -> value.matches("\\d{1,2}/\\d{1,2}/\\d{4}") ? 0.95 : 0.7;
            case "budget" -> value.matches("\\d+") ? 0.85 : 0.6;
            default -> 0.75;
        };
    }

    private VoiceSearchResponse.EntityValidation validateEntity(String entityType, String value) {
        boolean isValid = switch (entityType) {
            case "destination" -> value.length() >= 2;
            case "date" -> isValidDate(value);
            case "budget" -> value.matches("\\d+");
            case "travelers" -> value.matches("[1-9]\\d?");
            default -> true;
        };

        return VoiceSearchResponse.EntityValidation.builder()
                .isValid(isValid)
                .validationStatus(isValid ? "VALID" : "INVALID")
                .validationErrors(isValid ? List.of() : List.of("Invalid " + entityType))
                .build();
    }

    private List<VoiceSearchResponse.ExtractedEntity> deduplicateAndSortEntities(
            List<VoiceSearchResponse.ExtractedEntity> entities) {

        return entities.stream()
                .collect(Collectors.toMap(
                        e -> e.getEntityType() + ":" + e.getValue(),
                        e -> e,
                        (e1, e2) -> e1.getConfidence() > e2.getConfidence() ? e1 : e2))
                .values()
                .stream()
                .sorted(Comparator.comparing(VoiceSearchResponse.ExtractedEntity::getConfidence).reversed())
                .collect(Collectors.toList());
    }

    private Map<String, Double> analyzeEmotions(String text) {
        Map<String, Double> emotions = new HashMap<>();
        emotions.put("excitement", text.toLowerCase().contains("amazing") || text.toLowerCase().contains("excited") ? 0.8 : 0.0);
        emotions.put("frustration", text.toLowerCase().contains("annoying") || text.toLowerCase().contains("frustrated") ? 0.7 : 0.0);
        emotions.put("satisfaction", text.toLowerCase().contains("satisfied") || text.toLowerCase().contains("happy") ? 0.75 : 0.0);
        return emotions;
    }

    private List<String> identifySentimentIndicators(String text) {
        List<String> indicators = new ArrayList<>();
        String lowerText = text.toLowerCase();

        if (lowerText.contains("!")) indicators.add("exclamation");
        if (lowerText.contains("?")) indicators.add("question");
        if (lowerText.contains("please")) indicators.add("politeness");
        if (lowerText.contains("urgent") || lowerText.contains("asap")) indicators.add("urgency");

        return indicators;
    }

    private String determineConversationPhase(String text, Map<String, Object> history) {
        String lowerText = text.toLowerCase();

        if (lowerText.contains("hello") || lowerText.contains("hi")) return "greeting";
        if (lowerText.contains("thank") || lowerText.contains("bye")) return "farewell";
        if (lowerText.contains("book") || lowerText.contains("reserve")) return "booking";
        if (lowerText.contains("?")) return "clarification";

        return "search";
    }

    private Map<String, Object> extractContextVariables(String text, Map<String, Object> history) {
        Map<String, Object> variables = new HashMap<>();

        if (history != null) {
            variables.putAll(history);
        }

        // Extract implicit variables from current text
        if (text.toLowerCase().contains("business")) variables.put("travel_purpose", "business");
        if (text.toLowerCase().contains("family")) variables.put("travel_type", "family");
        if (text.toLowerCase().contains("luxury")) variables.put("accommodation_level", "luxury");

        return variables;
    }

    private List<String> inferUserPreferences(String text, Map<String, Object> history) {
        List<String> preferences = new ArrayList<>();
        String lowerText = text.toLowerCase();

        if (lowerText.contains("cheap") || lowerText.contains("budget")) preferences.add("budget_conscious");
        if (lowerText.contains("luxury") || lowerText.contains("premium")) preferences.add("luxury_seeking");
        if (lowerText.contains("quiet") || lowerText.contains("peaceful")) preferences.add("quiet_environment");
        if (lowerText.contains("city") || lowerText.contains("downtown")) preferences.add("urban_location");

        return preferences;
    }

    private String assessUserExpertiseLevel(String text, Map<String, Object> history) {
        String lowerText = text.toLowerCase();

        if (lowerText.contains("what is") || lowerText.contains("how do") || lowerText.contains("explain")) {
            return "novice";
        }

        if (lowerText.contains("specific") || lowerText.contains("detailed") || lowerText.contains("technical")) {
            return "expert";
        }

        return "intermediate";
    }

    private List<String> identifyAmbiguities(String text) {
        List<String> ambiguities = new ArrayList<>();

        if (text.toLowerCase().contains("near") && !containsSpecificLocation(text)) {
            ambiguities.add("Location reference 'near' is ambiguous");
        }

        if (text.toLowerCase().contains("cheap") && !containsBudgetRange(text)) {
            ambiguities.add("Budget preference 'cheap' is subjective");
        }

        return ambiguities;
    }

    // Additional helper methods with simplified implementations

    private Map<String, Double> analyzeIntentsWithContext(String text, Map<String, Object> context) {
        Map<String, Double> baseScores = analyzeIntents(text);
        // Apply context-based boosting here
        return baseScores;
    }

    private VoiceSearchResponse.IntentAnalysis buildIntentAnalysis(Map<String, Double> intentScores) {
        if (intentScores.isEmpty()) {
            return VoiceSearchResponse.IntentAnalysis.builder()
                    .primaryIntent("general_inquiry")
                    .primaryConfidence(0.5)
                    .intentCategory("general")
                    .build();
        }

        Map.Entry<String, Double> topIntent = intentScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(Map.entry("general_inquiry", 0.5));

        List<VoiceSearchResponse.IntentScore> alternatives = intentScores.entrySet().stream()
                .filter(e -> !e.getKey().equals(topIntent.getKey()))
                .map(e -> VoiceSearchResponse.IntentScore.builder()
                        .intent(e.getKey())
                        .confidence(e.getValue())
                        .parameters(Map.of())
                        .build())
                .collect(Collectors.toList());

        return VoiceSearchResponse.IntentAnalysis.builder()
                .primaryIntent(topIntent.getKey())
                .primaryConfidence(topIntent.getValue())
                .alternativeIntents(alternatives)
                .intentCategory(categorizeIntent(topIntent.getKey()))
                .requiresAction(topIntent.getValue() > confidenceThreshold)
                .missingParameters(List.of())
                .complexityLevel(assessComplexity(topIntent.getKey()))
                .build();
    }

    private List<VoiceSearchResponse.ExtractedEntity> extractEntitiesWithContext(String text, Map<String, Object> context) {
        List<VoiceSearchResponse.ExtractedEntity> entities = extractEntities(text);
        // Enhance entities with context information
        return entities;
    }

    private List<VoiceSearchResponse.ClarificationQuestion> generateClarificationQuestions(
            VoiceSearchResponse.IntentAnalysis intent, List<VoiceSearchResponse.ExtractedEntity> entities, Map<String, Object> context) {

        List<VoiceSearchResponse.ClarificationQuestion> questions = new ArrayList<>();

        // Check for missing essential entities based on intent
        if ("search_hotels".equals(intent.getPrimaryIntent())) {
            boolean hasDestination = entities.stream().anyMatch(e -> "destination".equals(e.getEntityType()));
            if (!hasDestination) {
                questions.add(VoiceSearchResponse.ClarificationQuestion.builder()
                        .questionId("dest_01")
                        .question("Where would you like to stay?")
                        .questionType("open_ended")
                        .priority("high")
                        .isRequired(true)
                        .build());
            }
        }

        return questions;
    }

    private VoiceSearchResponse.ActionableInsights generateActionableInsights(
            VoiceSearchResponse.IntentAnalysis intent, List<VoiceSearchResponse.ExtractedEntity> entities,
            VoiceSearchResponse.ContextAnalysis context) {

        List<String> detectedPreferences = new ArrayList<>();
        List<String> recommendedActions = new ArrayList<>();

        // Generate insights based on intent and entities
        if ("search_hotels".equals(intent.getPrimaryIntent())) {
            recommendedActions.add("Execute hotel search");
            if (entities.stream().anyMatch(e -> "budget".equals(e.getEntityType()))) {
                detectedPreferences.add("budget_conscious");
            }
        }

        return VoiceSearchResponse.ActionableInsights.builder()
                .detectedPreferences(detectedPreferences)
                .recommendedActions(recommendedActions)
                .nextBestAction(recommendedActions.isEmpty() ? "ask_for_clarification" : recommendedActions.get(0))
                .build();
    }

    private List<String> extractKeyTerms(String text) {
        // Simple keyword extraction
        return Arrays.stream(text.split("\\s+"))
                .filter(word -> word.length() > 3)
                .filter(word -> !isStopWord(word))
                .collect(Collectors.toList());
    }

    private String generateSimplifiedQuery(String text) {
        // Remove common phrases and simplify
        return text.replaceAll("(?i)\\b(i|am|looking|for|want|to|need|a|an|the)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<String> generateSynonymQueries(List<String> keyTerms) {
        // Simple synonym replacement
        Map<String, String> synonyms = Map.of(
                "hotel", "accommodation",
                "cheap", "budget",
                "expensive", "luxury",
                "near", "close to"
        );

        return keyTerms.stream()
                .map(term -> synonyms.getOrDefault(term.toLowerCase(), term))
                .distinct()
                .map(term -> String.join(" ", keyTerms).replace(keyTerms.get(0), term))
                .collect(Collectors.toList());
    }

    // Utility methods with simplified implementations
    private String normalizeDateValue(String value) { return value.toLowerCase(); }
    private String normalizeDurationValue(String value) { return value.toLowerCase(); }
    private String normalizeBudgetValue(String value) { return value.replaceAll("[^\\d]", ""); }
    private boolean isValidDate(String value) { return value.matches("\\d{1,2}/\\d{1,2}/\\d{4}"); }
    private boolean containsSpecificLocation(String text) { return text.matches(".*\\b[A-Z][a-z]+(\\s+[A-Z][a-z]+)*\\b.*"); }
    private boolean containsBudgetRange(String text) { return text.matches(".*\\$\\d+.*"); }
    private String categorizeIntent(String intent) {
        return intent.startsWith("search_") ? "search" : "inquiry";
    }
    private String assessComplexity(String intent) { return "medium"; }
    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are", "was", "were", "be", "been", "have", "has", "had", "do", "does", "did", "will", "would", "could", "should");
        return stopWords.contains(word.toLowerCase());
    }
}