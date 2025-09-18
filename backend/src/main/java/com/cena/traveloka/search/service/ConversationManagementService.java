package com.cena.traveloka.search.service;

import com.cena.traveloka.search.dto.VoiceSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
@Slf4j
public class ConversationManagementService {

    private final Map<String, ConversationState> conversationStates = new ConcurrentHashMap<>();

    public void updateConversationState(String sessionId, VoiceSearchResponse response) {
        log.debug("Updating conversation state for session: {}", sessionId);
        // Mock implementation - store conversation state
        conversationStates.put(sessionId, new ConversationState());
    }

    public ConversationState getConversationState(String conversationId) {
        return conversationStates.getOrDefault(conversationId, new ConversationState());
    }

    public static class ConversationState {
        // Mock conversation state
    }
}