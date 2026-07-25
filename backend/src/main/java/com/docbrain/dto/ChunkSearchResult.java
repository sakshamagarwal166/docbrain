package com.docbrain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChunkSearchResult {

    private UUID chunkId;
    private UUID documentId;
    private String documentName;
    private Integer chunkIndex;
    private String content;
    private Double similarityScore;
    private String metadata;
}
