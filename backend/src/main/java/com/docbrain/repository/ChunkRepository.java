package com.docbrain.repository;

import com.docbrain.model.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ChunkRepository extends JpaRepository<Chunk, UUID> {

    int countByDocumentId(UUID documentId);

    void deleteByDocumentId(UUID documentId);

    @Modifying
    @Query(value = "INSERT INTO chunks (id, document_id, chunk_index, content, embedding, metadata) " +
            "VALUES (:id, :documentId, :chunkIndex, :content, cast(:embedding AS vector), cast(:metadata AS jsonb))",
            nativeQuery = true)
    void insertWithEmbedding(@Param("id") UUID id,
                             @Param("documentId") UUID documentId,
                             @Param("chunkIndex") int chunkIndex,
                             @Param("content") String content,
                             @Param("embedding") String embedding,
                             @Param("metadata") String metadata);
}
