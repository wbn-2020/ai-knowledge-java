package com.knowflow.modules.document;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.common.enums.DocumentParseStatus;

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
