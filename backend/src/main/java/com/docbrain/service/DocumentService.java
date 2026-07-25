package com.docbrain.service;

import com.docbrain.dto.DocumentResponse;
import com.docbrain.exception.BadRequestException;
import com.docbrain.exception.ResourceNotFoundException;
import com.docbrain.model.Document;
import com.docbrain.model.DocumentStatus;
import com.docbrain.model.User;
import com.docbrain.repository.ChunkRepository;
import com.docbrain.repository.DocumentRepository;
import com.docbrain.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final UserRepository userRepository;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;
    private final Tika tika;

    public DocumentService(DocumentRepository documentRepository,
                           ChunkRepository chunkRepository,
                           UserRepository userRepository,
                           ChunkingService chunkingService,
                           EmbeddingService embeddingService,
                           ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.userRepository = userRepository;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
        this.tika = new Tika();
    }

    @Transactional
    public DocumentResponse upload(UUID userId, MultipartFile file) {
        validateFile(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Document document = Document.builder()
                .user(user)
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .status(DocumentStatus.UPLOADED)
                .build();
        document = documentRepository.save(document);

        processDocumentAsync(document.getId(), file);

        return DocumentResponse.from(document);
    }

    public List<DocumentResponse> listDocuments(UUID userId) {
        return documentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    public DocumentResponse getDocument(UUID userId, UUID documentId) {
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        return DocumentResponse.from(document);
    }

    @Transactional
    public void deleteDocument(UUID userId, UUID documentId) {
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        chunkRepository.deleteByDocumentId(documentId);
        documentRepository.delete(document);
    }

    @Async
    public void processDocumentAsync(UUID documentId, MultipartFile file) {
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(document);

            String text = extractText(file);
            List<String> chunks = chunkingService.chunkText(text);

            for (int i = 0; i < chunks.size(); i++) {
                String chunkContent = chunks.get(i);
                float[] embedding = embeddingService.embed(chunkContent);
                String embeddingStr = embeddingToString(embedding);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("chunk_index", i);
                metadata.put("source", document.getOriginalFilename());

                chunkRepository.insertWithEmbedding(
                        UUID.randomUUID(),
                        documentId,
                        i,
                        chunkContent,
                        embeddingStr,
                        toJson(metadata)
                );
            }

            document.setTotalChunks(chunks.size());
            document.setStatus(DocumentStatus.READY);
            documentRepository.save(document);

            log.info("Document {} processed: {} chunks", documentId, chunks.size());
        } catch (Exception e) {
            log.error("Failed to process document {}", documentId, e);
            documentRepository.findById(documentId).ifPresent(doc -> {
                doc.setStatus(DocumentStatus.FAILED);
                documentRepository.save(doc);
            });
        }
    }

    @Transactional
    public DocumentResponse reprocessDocument(UUID userId, UUID documentId) {
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        document.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(document);

        reEmbedDocumentAsync(documentId);

        return DocumentResponse.from(document);
    }

    public List<DocumentResponse> reprocessAllDocuments(UUID userId) {
        List<Document> documents = documentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<DocumentResponse> responses = new ArrayList<>();
        for (Document doc : documents) {
            doc.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(doc);
            reEmbedDocumentAsync(doc.getId());
            responses.add(DocumentResponse.from(doc));
        }
        return responses;
    }

    @Async
    public void reEmbedDocumentAsync(UUID documentId) {
        try {
            List<Object[]> chunks = chunkRepository.findChunkContentByDocumentId(documentId);
            for (Object[] row : chunks) {
                UUID chunkId = (UUID) row[0];
                String content = (String) row[1];
                float[] embedding = embeddingService.embed(content);
                String embeddingStr = embeddingToString(embedding);
                chunkRepository.updateEmbedding(chunkId, embeddingStr);
            }
            documentRepository.findById(documentId).ifPresent(doc -> {
                doc.setStatus(DocumentStatus.READY);
                documentRepository.save(doc);
            });
            log.info("Document {} re-embedded: {} chunks", documentId, chunks.size());
        } catch (Exception e) {
            log.error("Failed to re-embed document {}", documentId, e);
            documentRepository.findById(documentId).ifPresent(doc -> {
                doc.setStatus(DocumentStatus.FAILED);
                documentRepository.save(doc);
            });
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds 10MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BadRequestException("Unsupported file type. Allowed: PDF, DOCX, TXT");
        }
    }

    private String extractText(MultipartFile file) throws IOException, TikaException {
        try (InputStream is = file.getInputStream()) {
            return tika.parseToString(is);
        }
    }

    private String embeddingToString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
