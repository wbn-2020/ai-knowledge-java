package com.knowflow.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowflow.entity.KnowledgeBaseSummary;
import java.util.Optional;

public interface KnowledgeBaseSummaryRepository extends BaseMapper<KnowledgeBaseSummary> {
    default Optional<KnowledgeBaseSummary> findByUserIdAndKnowledgeBaseId(Long userId, Long knowledgeBaseId) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<KnowledgeBaseSummary>()
                .eq(KnowledgeBaseSummary::getUserId, userId)
                .eq(KnowledgeBaseSummary::getKnowledgeBaseId, knowledgeBaseId)
                .last("limit 1")));
    }
}
