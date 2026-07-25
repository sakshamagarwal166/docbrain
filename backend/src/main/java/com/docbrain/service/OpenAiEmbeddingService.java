package com.docbrain.service;

import com.docbrain.exception.AiServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class OpenAiEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingService.class);
    private static final String EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;

    public OpenAiEmbeddingService(RestTemplate restTemplate, String apiKey, String model) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public float[] embed(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", model,
                "input", text
        );

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    EMBEDDINGS_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    JsonNode.class
            );

            JsonNode data = response.getBody().get("data").get(0).get("embedding");
            float[] embedding = new float[data.size()];
            for (int i = 0; i < data.size(); i++) {
                embedding[i] = data.get(i).floatValue();
            }
            return embedding;
        } catch (RestClientException e) {
            log.error("OpenAI Embeddings API call failed", e);
            throw new AiServiceException("Failed to generate embedding: " + e.getMessage());
        }
    }
}
