package com.knowflow.modules.document;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

public interface DocumentChunkRepository extends BaseMapper<DocumentChunk> {
    default List<DocumentChunk> findByUserIdAndKnowledgeBaseIdAndDeletedFalse(Long userId, Long knowledgeBaseId) {
        return selectList(new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getUserId, userId)
                .eq(DocumentChunk::getKnowledgeBaseId, knowledgeBaseId));
    }

    default List<DocumentChunk> findByDocumentIdAndDeletedFalseOrderByChunkIndexAsc(Long documentId) {
        return selectList(new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, documentId)
                .orderByAsc(DocumentChunk::getChunkIndex));
    }

    default void deleteByDocumentId(Long documentId) {
        delete(new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getDocumentId, documentId));
    }
}
