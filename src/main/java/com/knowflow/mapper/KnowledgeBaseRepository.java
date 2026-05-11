package com.knowflow.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.entity.KnowledgeBase;
import com.knowflow.enums.KnowledgeBaseStatus;
import java.util.Optional;



public interface KnowledgeBaseRepository extends BaseMapper<KnowledgeBase> {
    default Optional<KnowledgeBase> findByIdAndUserIdAndDeletedFalse(Long id, Long userId) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, id)
                .eq(KnowledgeBase::getUserId, userId)
                .eq(KnowledgeBase::getDeleted, false)
                .last("limit 1")));
    }

    default Optional<KnowledgeBase> findByIdAndDeletedFalse(Long id) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, id)
                .eq(KnowledgeBase::getDeleted, false)
                .last("limit 1")));
    }

    default Page<KnowledgeBase> findByUserIdAndDeletedFalseAndNameContaining(String keyword, Long userId, Page<KnowledgeBase> page) {
        return selectPage(page, new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getUserId, userId)
                .eq(KnowledgeBase::getDeleted, false)
                .like(keyword != null && !keyword.isBlank(), KnowledgeBase::getName, keyword)
                .orderByDesc(KnowledgeBase::getUpdateTime));
    }

    default Page<KnowledgeBase> findByDeletedFalseAndNameContaining(String keyword, Page<KnowledgeBase> page) {
        return selectPage(page, new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getDeleted, false)
                .like(keyword != null && !keyword.isBlank(), KnowledgeBase::getName, keyword)
                .orderByDesc(KnowledgeBase::getCreateTime));
    }

    default Page<KnowledgeBase> findByAdminFilters(String keyword,
                                                   KnowledgeBaseStatus status,
                                                   Page<KnowledgeBase> page) {
        return selectPage(page, new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getDeleted, false)
                .like(keyword != null && !keyword.isBlank(), KnowledgeBase::getName, keyword)
                .eq(status != null, KnowledgeBase::getStatus, status)
                .orderByDesc(KnowledgeBase::getCreateTime));
    }

    default long countByUserIdAndDeletedFalse(Long userId) {
        return selectCount(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getUserId, userId)
                .eq(KnowledgeBase::getDeleted, false));
    }

    default long countByDeletedFalse() {
        return selectCount(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getDeleted, false));
    }

    default long countByStatusAndDeletedFalse(KnowledgeBaseStatus status) {
        return selectCount(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getStatus, status)
                .eq(KnowledgeBase::getDeleted, false));
    }
}
