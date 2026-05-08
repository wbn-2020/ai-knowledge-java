package com.knowflow.vo;

import com.knowflow.entity.ChatSession;
import java.time.LocalDateTime;


public record ChatSessionVO(Long id, Long knowledgeBaseId, String title, LocalDateTime createTime, LocalDateTime updateTime) {
    public static ChatSessionVO from(ChatSession session) {
        return new ChatSessionVO(session.getId(), session.getKnowledgeBaseId(), session.getTitle(), session.getCreateTime(), session.getUpdateTime());
    }
}
