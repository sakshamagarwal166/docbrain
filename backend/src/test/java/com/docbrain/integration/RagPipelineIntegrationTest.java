package com.docbrain.integration;

import com.docbrain.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.docbrain.model.DocumentStatus;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class RagPipelineIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("docbrain_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = "test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        RegisterRequest register = new RegisterRequest(email, "password123");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        authToken = authResponse.getToken();
    }

    @Test
    void fullRagPipeline_uploadQueryWithCitations() throws Exception {
        // 1. Upload a document
        MockMultipartFile file = new MockMultipartFile("file", "test-doc.txt",
                "text/plain",
                "DocBrain is an AI-powered document Q&A application. It uses RAG to provide accurate answers with citations.".getBytes());

        MvcResult uploadResult = mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated())
                .andReturn();

        DocumentResponse docResponse = objectMapper.readValue(
                uploadResult.getResponse().getContentAsString(), DocumentResponse.class);
        UUID documentId = docResponse.getId();
        assertNotNull(documentId);

        // 2. Wait for async processing
        DocumentStatus docStatus = DocumentStatus.UPLOADED;
        for (int i = 0; i < 20 && docStatus != DocumentStatus.READY; i++) {
            Thread.sleep(500);
            MvcResult docResult = mockMvc.perform(get("/api/documents/" + documentId)
                            .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk())
                    .andReturn();
            DocumentResponse doc = objectMapper.readValue(
                    docResult.getResponse().getContentAsString(), DocumentResponse.class);
            docStatus = doc.getStatus();
        }
        assertEquals(DocumentStatus.READY, docStatus, "Document should be READY after processing");

        // 3. Create conversation
        CreateConversationRequest convRequest = new CreateConversationRequest(List.of(documentId));
        MvcResult convResult = mockMvc.perform(post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convRequest))
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated())
                .andReturn();

        ConversationResponse convResponse = objectMapper.readValue(
                convResult.getResponse().getContentAsString(), ConversationResponse.class);
        UUID conversationId = convResponse.getId();

        // 4. Send a question
        SendMessageRequest msgRequest = new SendMessageRequest();
        msgRequest.setMessage("What is DocBrain?");

        MvcResult msgResult = mockMvc.perform(post("/api/conversations/" + conversationId + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgRequest))
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isCreated())
                .andReturn();

        MessageResponse msgResponse = objectMapper.readValue(
                msgResult.getResponse().getContentAsString(), MessageResponse.class);

        assertNotNull(msgResponse.getContent());
        assertFalse(msgResponse.getContent().isEmpty());
        assertNotNull(msgResponse.getCitations());

        // 5. Verify conversation detail includes messages
        MvcResult detailResult = mockMvc.perform(get("/api/conversations/" + conversationId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andReturn();

        ConversationDetailResponse detail = objectMapper.readValue(
                detailResult.getResponse().getContentAsString(), ConversationDetailResponse.class);

        assertEquals(2, detail.getMessages().size());
        assertEquals("USER", detail.getMessages().get(0).getRole().name());
        assertEquals("ASSISTANT", detail.getMessages().get(1).getRole().name());
    }

    @Test
    void actuatorHealth_returnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
