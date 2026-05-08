package com.knowflow.modules.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    List<DocumentChunk> findByUserIdAndKnowledgeBaseIdAndDeletedFalse(Long userId, Long knowledgeBaseId);
    List<DocumentChunk> findByDocumentIdAndDeletedFalseOrderByChunkIndexAsc(Long documentId);
    void deleteByDocumentId(Long documentId);
}
