package com.knowflow.modules.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

public interface ChatMessageReferenceRepository extends BaseMapper<ChatMessageReference> {
    default List<ChatMessageReference> findByUserIdAndMessageIdAndDeletedFalse(Long userId, Long messageId) {
        return selectList(new LambdaQueryWrapper<ChatMessageReference>()
                .eq(ChatMessageReference::getUserId, userId)
                .eq(ChatMessageReference::getMessageId, messageId));
    }
}
