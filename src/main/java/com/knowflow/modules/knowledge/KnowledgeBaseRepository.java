package com.knowflow.modules.knowledge;

import com.knowflow.common.enums.KnowledgeBaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    Optional<KnowledgeBase> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);
    Optional<KnowledgeBase> findByIdAndDeletedFalse(Long id);
    Page<KnowledgeBase> findByUserIdAndDeletedFalseAndNameContaining(String keyword, Long userId, Pageable pageable);
    Page<KnowledgeBase> findByDeletedFalseAndNameContaining(String keyword, Pageable pageable);
    long countByUserIdAndDeletedFalse(Long userId);
    long countByDeletedFalse();
    long countByStatusAndDeletedFalse(KnowledgeBaseStatus status);
}
