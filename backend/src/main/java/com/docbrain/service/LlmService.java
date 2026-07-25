package com.docbrain.service;

import com.docbrain.dto.LlmResponse;

import java.util.List;
import java.util.Map;

public interface LlmService {

    LlmResponse generate(String systemPrompt, String userMessage);

    LlmResponse generate(String systemPrompt, String userMessage, List<Map<String, String>> conversationHistory);
}
