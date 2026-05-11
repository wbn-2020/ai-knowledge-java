package com.knowflow.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowflow.entity.KeywordExtractResult;
import java.util.List;

public interface KeywordExtractResultRepository extends BaseMapper<KeywordExtractResult> {
    default List<KeywordExtractResult> findByUserIdAndTarget(String targetType, Long targetId, Long userId) {
        return selectList(new LambdaQueryWrapper<KeywordExtractResult>()
                .eq(KeywordExtractResult::getTargetType, targetType)
                .eq(KeywordExtractResult::getTargetId, targetId)
                .eq(KeywordExtractResult::getUserId, userId)
                .orderByDesc(KeywordExtractResult::getWeight)
                .orderByAsc(KeywordExtractResult::getCreateTime));
    }

    default void deleteByUserIdAndTarget(String targetType, Long targetId, Long userId) {
        delete(new LambdaQueryWrapper<KeywordExtractResult>()
                .eq(KeywordExtractResult::getTargetType, targetType)
                .eq(KeywordExtractResult::getTargetId, targetId)
                .eq(KeywordExtractResult::getUserId, userId));
    }
}
