package com.docbrain.service;

import com.docbrain.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class QueryService {

    private final ChunkSearchService chunkSearchService;
    private final LlmService llmService;
    private final String systemPromptTemplate;
    private final int defaultTopK;

    public QueryService(ChunkSearchService chunkSearchService,
                        LlmService llmService,
                        @Value("${docbrain.rag.top-k:5}") int defaultTopK) {
        this.chunkSearchService = chunkSearchService;
        this.llmService = llmService;
        this.defaultTopK = defaultTopK;
        this.systemPromptTemplate = loadSystemPrompt();
    }

    public QueryResponse query(UUID userId, List<UUID> documentIds, String question) {
        return query(userId, documentIds, question, List.of());
    }

    public QueryResponse query(UUID userId, List<UUID> documentIds, String question,
                                List<Map<String, String>> conversationHistory) {
        List<ChunkSearchResult> chunks = chunkSearchService.searchSimilarChunks(
                question, documentIds, userId, defaultTopK);

        String context = formatContext(chunks);
        String systemPrompt = systemPromptTemplate.replace("{context}", context);

        LlmResponse llmResponse = llmService.generate(systemPrompt, question, conversationHistory);

        List<CitedSource> citations = new ArrayList<>(llmResponse.getCitations());
        for (ChunkSearchResult chunk : chunks) {
            Integer pageNumber = null;
            if (chunk.getMetadata() != null && chunk.getMetadata().contains("page")) {
                try {
                    String meta = chunk.getMetadata();
                    int idx = meta.indexOf("\"page\":");
                    if (idx >= 0) {
                        String numStr = meta.substring(idx + 7).replaceAll("[^0-9]", " ").trim().split("\\s+")[0];
                        pageNumber = Integer.parseInt(numStr);
                    }
                } catch (Exception ignored) {
                }
            }

            citations.add(CitedSource.builder()
                    .documentId(chunk.getDocumentId())
                    .documentName(chunk.getDocumentName())
                    .chunkIndex(chunk.getChunkIndex())
                    .pageNumber(pageNumber)
                    .relevantText(truncate(chunk.getContent(), 200))
                    .build());
        }

        return QueryResponse.builder()
                .answer(llmResponse.getAnswer())
                .citations(citations)
                .chunksUsed(chunks.size())
                .build();
    }

    private String formatContext(List<ChunkSearchResult> chunks) {
        if (chunks.isEmpty()) {
            return "No relevant context found.";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            ChunkSearchResult chunk = chunks.get(i);
            sb.append(String.format("[Source %d: %s, Chunk %d (similarity: %.3f)]\n%s\n\n",
                    i + 1,
                    chunk.getDocumentName(),
                    chunk.getChunkIndex(),
                    chunk.getSimilarityScore(),
                    chunk.getContent()));
        }
        return sb.toString();
    }

    private String loadSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource("rag-system-prompt.txt");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "You are an AI assistant. Answer questions based on the provided context.\n\nContext:\n{context}";
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
