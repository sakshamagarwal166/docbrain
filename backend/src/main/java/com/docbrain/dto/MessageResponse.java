package com.docbrain.dto;

import com.docbrain.model.MessageRole;
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
public class MessageResponse {

    private UUID id;
    private MessageRole role;
    private String content;
    private String citations;
    private Instant createdAt;
}
