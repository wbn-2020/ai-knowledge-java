package com.knowflow.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowflow.entity.ChatMessageReference;
import java.util.List;



public interface ChatMessageReferenceRepository extends BaseMapper<ChatMessageReference> {
    default List<ChatMessageReference> findByUserIdAndMessageIdAndDeletedFalse(Long userId, Long messageId) {
        return selectList(new LambdaQueryWrapper<ChatMessageReference>()
                .eq(ChatMessageReference::getUserId, userId)
                .eq(ChatMessageReference::getMessageId, messageId));
    }
}
