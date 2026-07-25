package com.docbrain.dto;

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
public class CreateConversationRequest {

    @NotEmpty(message = "At least one document ID is required")
    @Size(max = 10, message = "Maximum 10 documents per conversation")
    private List<UUID> documentIds;
}
