package com.knowflow.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowflow.entity.DocumentSummary;
import java.util.Optional;

public interface DocumentSummaryRepository extends BaseMapper<DocumentSummary> {
    default Optional<DocumentSummary> findByUserIdAndDocumentId(Long userId, Long documentId) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<DocumentSummary>()
                .eq(DocumentSummary::getUserId, userId)
                .eq(DocumentSummary::getDocumentId, documentId)
                .last("limit 1")));
    }
}
