package com.docbrain.service;

import com.docbrain.dto.LlmResponse;
import com.docbrain.exception.AiServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenAiLlmService implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmService.class);
    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;

    public OpenAiLlmService(RestTemplate restTemplate, String apiKey, String model) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public LlmResponse generate(String systemPrompt, String userMessage) {
        return generate(systemPrompt, userMessage, List.of());
    }

    @Override
    public LlmResponse generate(String systemPrompt, String userMessage, List<Map<String, String>> conversationHistory) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(conversationHistory);
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0.3
        );

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    CHAT_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    JsonNode.class
            );

            String answer = response.getBody()
                    .get("choices").get(0)
                    .get("message").get("content")
                    .asText();

            return LlmResponse.builder()
                    .answer(answer)
                    .citations(List.of())
                    .build();
        } catch (RestClientException e) {
            log.error("OpenAI Chat API call failed", e);
            throw new AiServiceException("Failed to generate response: " + e.getMessage());
        }
    }
}
