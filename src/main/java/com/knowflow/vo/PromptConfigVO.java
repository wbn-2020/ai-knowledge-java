package com.knowflow.vo;

import com.knowflow.entity.PromptTemplate;
import java.time.LocalDateTime;

public record PromptConfigVO(
        Long id,
        String name,
        String type,
        String content,
        Boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PromptConfigVO from(PromptTemplate template) {
        return new PromptConfigVO(
                template.getId(),
                template.getName(),
                template.getScene(),
                template.getContent(),
                template.getEnabled(),
                template.getCreateTime(),
                template.getUpdateTime()
        );
    }
}
