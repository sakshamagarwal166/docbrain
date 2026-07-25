package com.docbrain.controller;

import com.docbrain.dto.*;
import com.docbrain.exception.RateLimitException;
import com.docbrain.security.AuthenticatedUser;
import com.docbrain.security.RateLimiter;
import com.docbrain.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversations", description = "Conversation history with RAG")
@SecurityRequirement(name = "bearerAuth")
public class ConversationController {

    private final ConversationService conversationService;
    private final RateLimiter rateLimiter;

    public ConversationController(ConversationService conversationService, RateLimiter rateLimiter) {
        this.conversationService = conversationService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new conversation with selected documents")
    public ConversationResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                       @Valid @RequestBody CreateConversationRequest request) {
        return conversationService.create(user.id(), request);
    }

    @GetMapping
    @Operation(summary = "List all conversations for the authenticated user")
    public List<ConversationResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return conversationService.list(user.id());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full conversation with all messages")
    public ConversationDetailResponse get(@AuthenticationPrincipal AuthenticatedUser user,
                                          @PathVariable UUID id) {
        return conversationService.getDetail(user.id(), id);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send a message and get an AI response")
    public MessageResponse sendMessage(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable UUID id,
                                       @Valid @RequestBody SendMessageRequest request) {
        if (!rateLimiter.isAllowed(user.id())) {
            throw new RateLimitException("Rate limit exceeded. Maximum 20 requests per minute.");
        }
        return conversationService.sendMessage(user.id(), id, request.getMessage());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a conversation and its messages")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user,
                       @PathVariable UUID id) {
        conversationService.delete(user.id(), id);
    }
}
