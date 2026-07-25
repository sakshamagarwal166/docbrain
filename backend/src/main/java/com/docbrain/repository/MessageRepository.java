package com.docbrain.repository;

import com.docbrain.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findTop10ByConversationIdOrderByCreatedAtDesc(UUID conversationId);
}
