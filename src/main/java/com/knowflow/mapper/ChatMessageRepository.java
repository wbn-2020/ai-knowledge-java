package com.knowflow.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowflow.entity.ChatMessage;
import java.util.List;



public interface ChatMessageRepository extends BaseMapper<ChatMessage> {
    default List<ChatMessage> findByUserIdAndSessionIdAndDeletedFalseOrderByCreateTimeAsc(Long userId, Long sessionId) {
        return selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getUserId, userId)
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime));
    }
}
