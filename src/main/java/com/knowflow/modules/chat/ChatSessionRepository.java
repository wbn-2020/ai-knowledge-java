package com.knowflow.modules.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);
    Page<ChatSession> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);
    long countByUserIdAndDeletedFalse(Long userId);
}
