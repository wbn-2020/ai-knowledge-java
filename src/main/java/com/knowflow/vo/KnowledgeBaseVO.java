package com.knowflow.vo;

import com.knowflow.entity.KnowledgeBase;
import com.knowflow.enums.KnowledgeBaseStatus;
import java.time.LocalDateTime;



public record KnowledgeBaseVO(
        Long id,
        Long userId,
        String name,
        String description,
        String icon,
        String category,
        KnowledgeBaseStatus status,
        Integer documentCount,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
    public static KnowledgeBaseVO from(KnowledgeBase kb) {
        return new KnowledgeBaseVO(
                kb.getId(),
                kb.getUserId(),
                kb.getName(),
                kb.getDescription(),
                kb.getIcon(),
                kb.getCategory(),
                kb.getStatus(),
                kb.getDocumentCount(),
                kb.getCreateTime(),
                kb.getUpdateTime()
        );
    }
}
