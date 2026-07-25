package com.docbrain.dto;

import com.docbrain.model.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {

    private UUID id;
    private String originalFilename;
    private String contentType;
    private Long fileSizeBytes;
    private Integer totalChunks;
    private DocumentStatus status;
    private Instant createdAt;

    public static DocumentResponse from(com.docbrain.model.Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .originalFilename(doc.getOriginalFilename())
                .contentType(doc.getContentType())
                .fileSizeBytes(doc.getFileSizeBytes())
                .totalChunks(doc.getTotalChunks())
                .status(doc.getStatus())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
