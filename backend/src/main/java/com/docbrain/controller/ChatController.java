package com.docbrain.controller;

import com.docbrain.dto.QueryRequest;
import com.docbrain.dto.QueryResponse;
import com.docbrain.exception.RateLimitException;
import com.docbrain.security.AuthenticatedUser;
import com.docbrain.security.RateLimiter;
import com.docbrain.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "RAG query pipeline")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final QueryService queryService;
    private final RateLimiter rateLimiter;

    public ChatController(QueryService queryService, RateLimiter rateLimiter) {
        this.queryService = queryService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/query")
    @Operation(summary = "Ask a question against selected documents")
    public QueryResponse query(@AuthenticationPrincipal AuthenticatedUser user,
                               @Valid @RequestBody QueryRequest request) {
        if (!rateLimiter.isAllowed(user.id())) {
            throw new RateLimitException("Rate limit exceeded. Maximum 20 requests per minute.");
        }
        return queryService.query(user.id(), request.getDocumentIds(), request.getQuestion());
    }
}
