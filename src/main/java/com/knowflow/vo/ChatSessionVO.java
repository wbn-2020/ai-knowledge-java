package com.knowflow.vo;

import com.knowflow.entity.ChatSession;
import java.time.LocalDateTime;

public record ChatSessionVO(
        Long id,
        Long knowledgeBaseId,
        String knowledgeBaseName,
        String title,
        String latestQuestion,
        Long messageCount,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        LocalDateTime updatedAt
) {
    public static ChatSessionVO from(ChatSession session) {
        return new ChatSessionVO(session.getId(), session.getKnowledgeBaseId(), null, session.getTitle(), null, 0L,
                session.getCreateTime(), session.getUpdateTime(), session.getUpdateTime());
    }
}
