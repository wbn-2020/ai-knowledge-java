package com.knowflow.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.entity.Document;
import com.knowflow.enums.DocumentParseStatus;
import java.util.List;
import java.util.Optional;



public interface DocumentRepository extends BaseMapper<Document> {
    default Optional<Document> findByIdAndUserIdAndDeletedFalse(Long id, Long userId) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<Document>()
                .eq(Document::getId, id)
                .eq(Document::getUserId, userId)
                .last("limit 1")));
    }

    default Optional<Document> findByIdAndDeletedFalse(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    default Page<Document> findByUserIdAndKnowledgeBaseIdAndDeletedFalseAndNameContaining(Long userId, Long knowledgeBaseId, String keyword, Page<Document> page) {
        return selectPage(page, new LambdaQueryWrapper<Document>()
                .eq(Document::getUserId, userId)
                .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
                .like(keyword != null && !keyword.isBlank(), Document::getName, keyword)
                .orderByDesc(Document::getCreateTime));
    }

    default Page<Document> findByDeletedFalseAndNameContaining(String keyword, Page<Document> page) {
        return selectPage(page, new LambdaQueryWrapper<Document>()
                .like(keyword != null && !keyword.isBlank(), Document::getName, keyword)
                .orderByDesc(Document::getCreateTime));
    }

    default List<Document> findByUserIdAndKnowledgeBaseIdAndDeletedFalse(Long userId, Long knowledgeBaseId) {
        return selectList(new LambdaQueryWrapper<Document>()
                .eq(Document::getUserId, userId)
                .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
                .orderByDesc(Document::getCreateTime));
    }

    default List<Document> findRecentByUserIdAndKnowledgeBaseId(Long userId, Long knowledgeBaseId, int limit) {
        return selectList(new LambdaQueryWrapper<Document>()
                .eq(Document::getUserId, userId)
                .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
                .orderByDesc(Document::getCreateTime)
                .last("limit " + limit));
    }

    default void deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        delete(new LambdaQueryWrapper<Document>().eq(Document::getKnowledgeBaseId, knowledgeBaseId));
    }

    default long countByUserIdAndDeletedFalse(Long userId) {
        return selectCount(new LambdaQueryWrapper<Document>().eq(Document::getUserId, userId));
    }

    default long countByDeletedFalse() {
        return selectCount(new LambdaQueryWrapper<>());
    }

    default long countByParseStatusAndDeletedFalse(DocumentParseStatus status) {
        return selectCount(new LambdaQueryWrapper<Document>().eq(Document::getParseStatus, status));
    }
}
