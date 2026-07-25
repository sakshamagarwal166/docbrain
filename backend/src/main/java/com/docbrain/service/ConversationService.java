package com.docbrain.service;

import com.docbrain.dto.*;
import com.docbrain.exception.BadRequestException;
import com.docbrain.exception.ResourceNotFoundException;
import com.docbrain.model.*;
import com.docbrain.repository.ConversationRepository;
import com.docbrain.repository.DocumentRepository;
import com.docbrain.repository.MessageRepository;
import com.docbrain.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final QueryService queryService;
    private final ObjectMapper objectMapper;
    private final int maxHistoryPairs;

    public ConversationService(ConversationRepository conversationRepository,
                               MessageRepository messageRepository,
                               DocumentRepository documentRepository,
                               UserRepository userRepository,
                               QueryService queryService,
                               ObjectMapper objectMapper,
                               @Value("${docbrain.rag.max-history-pairs:5}") int maxHistoryPairs) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.queryService = queryService;
        this.objectMapper = objectMapper;
        this.maxHistoryPairs = maxHistoryPairs;
    }

    @Transactional
    public ConversationResponse create(UUID userId, CreateConversationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Document> documents = new ArrayList<>();
        for (UUID docId : request.getDocumentIds()) {
            Document doc = documentRepository.findByIdAndUserId(docId, userId)
                    .orElseThrow(() -> new BadRequestException("Document not found: " + docId));
            if (doc.getStatus() != DocumentStatus.READY) {
                throw new BadRequestException("Document not ready: " + doc.getOriginalFilename());
            }
            documents.add(doc);
        }

        Conversation conversation = Conversation.builder()
                .user(user)
                .title("New conversation")
                .documents(documents)
                .build();
        conversation = conversationRepository.save(conversation);

        return toResponse(conversation);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(UUID userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse getDetail(UUID userId, UUID conversationId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        List<MessageResponse> messages = conversation.getMessages().stream()
                .map(this::toMessageResponse)
                .toList();

        return ConversationDetailResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .documentNames(conversation.getDocuments().stream()
                        .map(Document::getOriginalFilename)
                        .toList())
                .messages(messages)
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    @Transactional
    public MessageResponse sendMessage(UUID userId, UUID conversationId, String userMessage) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        Message userMsg = Message.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(userMessage)
                .build();
        messageRepository.save(userMsg);

        if (conversation.getTitle().equals("New conversation")) {
            String title = userMessage.length() > 80 ? userMessage.substring(0, 80) + "..." : userMessage;
            conversation.setTitle(title);
        }

        List<UUID> documentIds = conversation.getDocuments().stream()
                .map(Document::getId)
                .toList();

        List<Map<String, String>> history = buildConversationHistory(conversationId);

        QueryResponse queryResponse = queryService.query(userId, documentIds, userMessage, history);

        String citationsJson;
        try {
            citationsJson = objectMapper.writeValueAsString(queryResponse.getCitations());
        } catch (JsonProcessingException e) {
            citationsJson = "[]";
        }

        Message assistantMsg = Message.builder()
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content(queryResponse.getAnswer())
                .citations(citationsJson)
                .build();
        assistantMsg = messageRepository.save(assistantMsg);

        conversationRepository.save(conversation);

        return toMessageResponse(assistantMsg);
    }

    @Transactional
    public void delete(UUID userId, UUID conversationId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        conversationRepository.delete(conversation);
    }

    private List<Map<String, String>> buildConversationHistory(UUID conversationId) {
        List<Message> recentMessages = messageRepository
                .findTop10ByConversationIdOrderByCreatedAtDesc(conversationId);

        Collections.reverse(recentMessages);

        int maxMessages = maxHistoryPairs * 2;
        if (recentMessages.size() > maxMessages) {
            recentMessages = recentMessages.subList(recentMessages.size() - maxMessages, recentMessages.size());
        }

        return recentMessages.stream()
                .map(msg -> Map.of(
                        "role", msg.getRole() == MessageRole.USER ? "user" : "assistant",
                        "content", msg.getContent()))
                .collect(Collectors.toList());
    }

    private ConversationResponse toResponse(Conversation conversation) {
        return ConversationResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .documentNames(conversation.getDocuments().stream()
                        .map(Document::getOriginalFilename)
                        .toList())
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    private MessageResponse toMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .citations(message.getCitations())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
