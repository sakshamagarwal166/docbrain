package com.docbrain.service;

import com.docbrain.dto.ChunkSearchResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChunkSearchService {

    private final EntityManager entityManager;
    private final EmbeddingService embeddingService;

    public ChunkSearchService(EntityManager entityManager, EmbeddingService embeddingService) {
        this.entityManager = entityManager;
        this.embeddingService = embeddingService;
    }

    public List<ChunkSearchResult> searchSimilarChunks(String queryText, List<UUID> documentIds,
                                                       UUID userId, int topK) {
        float[] embedding = embeddingService.embed(queryText);
        String embeddingStr = embeddingToString(embedding);

        String sql = """
                SELECT c.id, c.document_id, d.original_filename, c.chunk_index, c.content,
                       1 - (c.embedding <=> cast(:embedding AS vector)) AS similarity,
                       cast(c.metadata AS text)
                FROM chunks c
                JOIN documents d ON c.document_id = d.id
                WHERE d.user_id = :userId
                  AND c.document_id IN (:documentIds)
                  AND c.embedding IS NOT NULL
                ORDER BY c.embedding <=> cast(:embedding AS vector)
                LIMIT :topK
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("embedding", embeddingStr);
        query.setParameter("userId", userId);
        query.setParameter("documentIds", documentIds);
        query.setParameter("topK", topK);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<ChunkSearchResult> results = new ArrayList<>();
        for (Object[] row : rows) {
            results.add(ChunkSearchResult.builder()
                    .chunkId((UUID) row[0])
                    .documentId((UUID) row[1])
                    .documentName((String) row[2])
                    .chunkIndex((Integer) row[3])
                    .content((String) row[4])
                    .similarityScore(((Number) row[5]).doubleValue())
                    .metadata((String) row[6])
                    .build());
        }
        return results;
    }

    private String embeddingToString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
