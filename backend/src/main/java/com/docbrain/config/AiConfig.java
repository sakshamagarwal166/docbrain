package com.docbrain.config;

import com.docbrain.service.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AiConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @ConditionalOnProperty(name = "docbrain.ai.provider", havingValue = "openai")
    public EmbeddingService openAiEmbeddingService(RestTemplate restTemplate,
                                                    org.springframework.core.env.Environment env) {
        return new OpenAiEmbeddingService(
                restTemplate,
                env.getProperty("docbrain.ai.openai.api-key"),
                env.getProperty("docbrain.ai.openai.embedding-model")
        );
    }

    @Bean
    @ConditionalOnProperty(name = "docbrain.ai.provider", havingValue = "openai")
    public LlmService openAiLlmService(RestTemplate restTemplate,
                                        org.springframework.core.env.Environment env) {
        return new OpenAiLlmService(
                restTemplate,
                env.getProperty("docbrain.ai.openai.api-key"),
                env.getProperty("docbrain.ai.openai.chat-model")
        );
    }

    @Bean
    @ConditionalOnProperty(name = "docbrain.ai.provider", havingValue = "mock", matchIfMissing = true)
    public EmbeddingService mockEmbeddingService(org.springframework.core.env.Environment env) {
        int dimension = Integer.parseInt(env.getProperty("docbrain.embedding.dimension", "1536"));
        return new MockEmbeddingService(dimension);
    }

    @Bean
    @ConditionalOnProperty(name = "docbrain.ai.provider", havingValue = "mock", matchIfMissing = true)
    public LlmService mockLlmService() {
        return new MockLlmService();
    }
}
