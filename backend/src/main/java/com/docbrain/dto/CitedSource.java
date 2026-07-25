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
public class CitedSource {

    private UUID documentId;
    private String documentName;
    private Integer chunkIndex;
    private Integer pageNumber;
    private String relevantText;
}
