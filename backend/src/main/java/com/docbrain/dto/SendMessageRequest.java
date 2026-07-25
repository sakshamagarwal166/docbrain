package com.docbrain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    private String message;
}
