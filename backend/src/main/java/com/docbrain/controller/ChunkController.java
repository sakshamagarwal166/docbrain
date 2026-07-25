package com.docbrain.controller;

import com.docbrain.dto.ChunkSearchResult;
import com.docbrain.security.AuthenticatedUser;
import com.docbrain.service.ChunkSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chunks")
@Tag(name = "Chunks", description = "Chunk search for debugging and transparency")
@SecurityRequirement(name = "bearerAuth")
public class ChunkController {

    private final ChunkSearchService chunkSearchService;

    public ChunkController(ChunkSearchService chunkSearchService) {
        this.chunkSearchService = chunkSearchService;
    }

    @GetMapping("/search")
    @Operation(summary = "Search similar chunks by text query (without LLM call)")
    public List<ChunkSearchResult> search(@AuthenticationPrincipal AuthenticatedUser user,
                                          @RequestParam String q,
                                          @RequestParam List<UUID> documentIds,
                                          @RequestParam(defaultValue = "5") int topK) {
        return chunkSearchService.searchSimilarChunks(q, documentIds, user.id(), topK);
    }
}
