package com.knowflow.modules.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageReferenceRepository extends JpaRepository<ChatMessageReference, Long> {
    List<ChatMessageReference> findByUserIdAndMessageIdAndDeletedFalse(Long userId, Long messageId);
}
