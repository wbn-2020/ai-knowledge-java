package com.knowflow.modules.document;

import com.knowflow.common.enums.DocumentParseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);
    Optional<Document> findByIdAndDeletedFalse(Long id);
    Page<Document> findByUserIdAndKnowledgeBaseIdAndDeletedFalseAndNameContaining(Long userId, Long knowledgeBaseId, String keyword, Pageable pageable);
    Page<Document> findByDeletedFalseAndNameContaining(String keyword, Pageable pageable);
    long countByUserIdAndDeletedFalse(Long userId);
    long countByDeletedFalse();
    long countByParseStatusAndDeletedFalse(DocumentParseStatus status);
}
