package com.docbrain.service;

import com.docbrain.dto.CitedSource;
import com.docbrain.dto.LlmResponse;
import java.util.List;
import java.util.Map;

public class MockLlmService implements LlmService {

    @Override
    public LlmResponse generate(String systemPrompt, String userMessage) {
        return generate(systemPrompt, userMessage, List.of());
    }

    @Override
    public LlmResponse generate(String systemPrompt, String userMessage, List<Map<String, String>> conversationHistory) {
        return LlmResponse.builder()
                .answer("[Mock LLM] This is a mock response to: " + truncate(userMessage, 100)
                        + ". In production, this would be answered by an LLM using the provided context.")
                .citations(List.of())
                .build();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
