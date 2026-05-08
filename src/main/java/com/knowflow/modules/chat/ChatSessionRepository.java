package com.knowflow.modules.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Optional;

public interface ChatSessionRepository extends BaseMapper<ChatSession> {
    default Optional<ChatSession> findByIdAndUserIdAndDeletedFalse(Long id, Long userId) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getId, id)
                .eq(ChatSession::getUserId, userId)
                .last("limit 1")));
    }

    default Page<ChatSession> findByUserIdAndDeletedFalse(Long userId, Page<ChatSession> page) {
        return selectPage(page, new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime));
    }

    default long countByUserIdAndDeletedFalse(Long userId) {
        return selectCount(new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getUserId, userId));
    }

    default java.util.List<ChatSession> findRecentByUserIdAndKnowledgeBaseId(Long userId, Long knowledgeBaseId, int limit) {
        return selectList(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getKnowledgeBaseId, knowledgeBaseId)
                .orderByDesc(ChatSession::getUpdateTime)
                .last("limit " + limit));
    }

    default void deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        delete(new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getKnowledgeBaseId, knowledgeBaseId));
    }
}
