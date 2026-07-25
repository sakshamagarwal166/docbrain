package com.docbrain.controller;

import com.docbrain.dto.DocumentResponse;
import com.docbrain.security.AuthenticatedUser;
import com.docbrain.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Document upload and management")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a document (PDF, DOCX, or TXT, max 10MB)")
    public DocumentResponse upload(@AuthenticationPrincipal AuthenticatedUser user,
                                   @RequestParam("file") MultipartFile file) {
        return documentService.upload(user.id(), file);
    }

    @GetMapping
    @Operation(summary = "List all documents for the authenticated user")
    public List<DocumentResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return documentService.listDocuments(user.id());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single document by ID")
    public DocumentResponse get(@AuthenticationPrincipal AuthenticatedUser user,
                                @PathVariable UUID id) {
        return documentService.getDocument(user.id(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a document and its chunks")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user,
                       @PathVariable UUID id) {
        documentService.deleteDocument(user.id(), id);
    }

    @PostMapping("/{id}/reprocess")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Re-process a document (re-parse, re-chunk, re-embed)")
    public DocumentResponse reprocess(@AuthenticationPrincipal AuthenticatedUser user,
                                      @PathVariable UUID id) {
        return documentService.reprocessDocument(user.id(), id);
    }

    @PostMapping("/reprocess-all")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Re-process all documents for the authenticated user")
    public List<DocumentResponse> reprocessAll(@AuthenticationPrincipal AuthenticatedUser user) {
        return documentService.reprocessAllDocuments(user.id());
    }
}
