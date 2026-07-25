package com.docbrain.service;

import com.docbrain.exception.BadRequestException;
import com.docbrain.model.User;
import com.docbrain.repository.ChunkRepository;
import com.docbrain.repository.DocumentRepository;
import com.docbrain.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private ChunkRepository chunkRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChunkingService chunkingService;
    @Mock private EmbeddingService embeddingService;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
                documentRepository, chunkRepository, userRepository,
                chunkingService, embeddingService, new ObjectMapper());
    }

    @Test
    void upload_emptyFile_throws() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                "text/plain", new byte[0]);

        assertThrows(BadRequestException.class, () -> documentService.upload(userId, file));
    }

    @Test
    void upload_tooLargeFile_throws() {
        UUID userId = UUID.randomUUID();
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11 MB
        MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                "text/plain", largeContent);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> documentService.upload(userId, file));
        assertTrue(ex.getMessage().contains("10MB"));
    }

    @Test
    void upload_unsupportedType_throws() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "test.exe",
                "application/octet-stream", "content".getBytes());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> documentService.upload(userId, file));
        assertTrue(ex.getMessage().contains("Unsupported"));
    }

    @Test
    void upload_validTxtFile_saves() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().email("test@example.com").build();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(documentRepository.save(any())).thenAnswer(inv -> {
            var doc = inv.getArgument(0, com.docbrain.model.Document.class);
            doc.setId(UUID.randomUUID());
            return doc;
        });

        MockMultipartFile file = new MockMultipartFile("file", "test.txt",
                "text/plain", "Hello world content".getBytes());

        var response = documentService.upload(userId, file);

        assertNotNull(response.getId());
        assertEquals("test.txt", response.getOriginalFilename());
        verify(documentRepository).save(any());
    }

    @Test
    void upload_validPdfContentType_saves() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().email("test@example.com").build();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(documentRepository.save(any())).thenAnswer(inv -> {
            var doc = inv.getArgument(0, com.docbrain.model.Document.class);
            doc.setId(UUID.randomUUID());
            return doc;
        });

        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf",
                "application/pdf", "PDF content".getBytes());

        var response = documentService.upload(userId, file);
        assertEquals("doc.pdf", response.getOriginalFilename());
    }

    @Test
    void upload_validDocxContentType_saves() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().email("test@example.com").build();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(documentRepository.save(any())).thenAnswer(inv -> {
            var doc = inv.getArgument(0, com.docbrain.model.Document.class);
            doc.setId(UUID.randomUUID());
            return doc;
        });

        MockMultipartFile file = new MockMultipartFile("file", "doc.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "DOCX content".getBytes());

        var response = documentService.upload(userId, file);
        assertEquals("doc.docx", response.getOriginalFilename());
    }
}
