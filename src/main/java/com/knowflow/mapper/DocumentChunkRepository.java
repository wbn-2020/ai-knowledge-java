package com.knowflow.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.entity.DocumentChunk;
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

    default Page<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId, Page<DocumentChunk> page) {
        return selectPage(page, new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, documentId)
                .orderByAsc(DocumentChunk::getChunkIndex));
    }

    default Page<DocumentChunk> findByUserIdAndDocumentIdOrderByChunkIndexAsc(Long userId, Long documentId, Page<DocumentChunk> page) {
        return selectPage(page, new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getUserId, userId)
                .eq(DocumentChunk::getDocumentId, documentId)
                .orderByAsc(DocumentChunk::getChunkIndex));
    }

    default int deleteByDocumentId(Long documentId) {
        return delete(new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getDocumentId, documentId));
    }

    default void deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        delete(new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getKnowledgeBaseId, knowledgeBaseId));
    }

    default long countByDocumentId(Long documentId) {
        return selectCount(new LambdaQueryWrapper<DocumentChunk>().eq(DocumentChunk::getDocumentId, documentId));
    }
}
