package com.knowflow.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.entity.Document;
import com.knowflow.enums.DocumentParseStatus;
import com.knowflow.enums.EmbeddingStatus;
import java.util.List;
import java.util.Optional;



public interface DocumentRepository extends BaseMapper<Document> {
    default Optional<Document> findByIdAndUserIdAndDeletedFalse(Long id, Long userId) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<Document>()
                .eq(Document::getId, id)
                .eq(Document::getUserId, userId)
                .eq(Document::getDeleted, false)
                .last("limit 1")));
    }

    default Optional<Document> findByIdAndDeletedFalse(Long id) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<Document>()
                .eq(Document::getId, id)
                .eq(Document::getDeleted, false)
                .last("limit 1")));
    }

    default Page<Document> findByUserIdAndKnowledgeBaseIdAndDeletedFalseAndNameContaining(Long userId, Long knowledgeBaseId, String keyword, Page<Document> page) {
        return selectPage(page, new LambdaQueryWrapper<Document>()
                .eq(Document::getUserId, userId)
                .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
                .eq(Document::getDeleted, false)
                .like(keyword != null && !keyword.isBlank(), Document::getName, keyword)
                .orderByDesc(Document::getCreateTime));
    }

    default Page<Document> findByUserIdAndFiltersAndDeletedFalse(Long userId,
                                                                 Long knowledgeBaseId,
                                                                 String keyword,
                                                                 DocumentParseStatus parseStatus,
                                                                 EmbeddingStatus embeddingStatus,
                                                                 Page<Document> page) {
        return selectPage(page, new LambdaQueryWrapper<Document>()
                .eq(Document::getUserId, userId)
                .eq(knowledgeBaseId != null, Document::getKnowledgeBaseId, knowledgeBaseId)
                .eq(Document::getDeleted, false)
                .eq(parseStatus != null, Document::getParseStatus, parseStatus)
                .eq(embeddingStatus != null, Document::getEmbeddingStatus, embeddingStatus)
                .like(keyword != null && !keyword.isBlank(), Document::getName, keyword)
                .orderByDesc(Document::getCreateTime));
    }

    default Page<Document> findByDeletedFalseAndNameContaining(String keyword, Page<Document> page) {
        return selectPage(page, new LambdaQueryWrapper<Document>()
                .eq(Document::getDeleted, false)
                .like(keyword != null && !keyword.isBlank(), Document::getName, keyword)
                .orderByDesc(Document::getCreateTime));
    }

    default Page<Document> findByAdminFilters(String keyword,
                                              Long knowledgeBaseId,
                                              Long userId,
                                              java.util.List<Long> userIds,
                                              DocumentParseStatus parseStatus,
                                              EmbeddingStatus embeddingStatus,
                                              String fileType,
                                              Page<Document> page) {
        return selectPage(page, new LambdaQueryWrapper<Document>()
                .eq(Document::getDeleted, false)
                .eq(knowledgeBaseId != null, Document::getKnowledgeBaseId, knowledgeBaseId)
                .eq(userId != null, Document::getUserId, userId)
                .in(userIds != null && !userIds.isEmpty(), Document::getUserId, userIds)
                .eq(parseStatus != null, Document::getParseStatus, parseStatus)
                .eq(embeddingStatus != null, Document::getEmbeddingStatus, embeddingStatus)
                .eq(fileType != null && !fileType.isBlank(), Document::getFileType, fileType)
                .like(keyword != null && !keyword.isBlank(), Document::getName, keyword)
                .orderByDesc(Document::getCreateTime));
    }

    default List<Document> findByUserIdAndKnowledgeBaseIdAndDeletedFalse(Long userId, Long knowledgeBaseId) {
        return selectList(new LambdaQueryWrapper<Document>()
                .eq(Document::getUserId, userId)
                .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
                .eq(Document::getDeleted, false)
                .orderByDesc(Document::getCreateTime));
    }

    default List<Document> findRecentByUserIdAndKnowledgeBaseId(Long userId, Long knowledgeBaseId, int limit) {
        return selectList(new LambdaQueryWrapper<Document>()
                .eq(Document::getUserId, userId)
                .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
                .eq(Document::getDeleted, false)
                .orderByDesc(Document::getCreateTime)
                .last("limit " + limit));
    }

    default void deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        delete(new LambdaQueryWrapper<Document>().eq(Document::getKnowledgeBaseId, knowledgeBaseId));
    }

    default long countByUserIdAndDeletedFalse(Long userId) {
        return selectCount(new LambdaQueryWrapper<Document>()
                .eq(Document::getUserId, userId)
                .eq(Document::getDeleted, false));
    }

    default long countByDeletedFalse() {
        return selectCount(new LambdaQueryWrapper<Document>()
                .eq(Document::getDeleted, false));
    }

    default long countByParseStatusAndDeletedFalse(DocumentParseStatus status) {
        return selectCount(new LambdaQueryWrapper<Document>()
                .eq(Document::getParseStatus, status)
                .eq(Document::getDeleted, false));
    }

    default long countByKnowledgeBaseIdAndDeletedFalse(Long knowledgeBaseId) {
        return selectCount(new LambdaQueryWrapper<Document>()
                .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
                .eq(Document::getDeleted, false));
    }

    default long countByKnowledgeBaseIdAndStatuses(Long knowledgeBaseId, List<DocumentParseStatus> statuses) {
        return selectCount(new LambdaQueryWrapper<Document>()
                .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
                .eq(Document::getDeleted, false)
                .in(statuses != null && !statuses.isEmpty(), Document::getParseStatus, statuses));
    }
}
