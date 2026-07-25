package com.docbrain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDetailResponse {

    private UUID id;
    private String title;
    private List<String> documentNames;
    private List<MessageResponse> messages;
    private Instant createdAt;
}
