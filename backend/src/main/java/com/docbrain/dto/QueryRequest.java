package com.docbrain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {

    @NotEmpty(message = "At least one document ID is required")
    @Size(max = 10, message = "Maximum 10 documents per query")
    private List<UUID> documentIds;

    @NotBlank(message = "Question is required")
    @Size(max = 1000, message = "Question must not exceed 1000 characters")
    private String question;
}
