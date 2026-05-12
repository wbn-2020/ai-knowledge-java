package com.knowflow.vo;

import com.knowflow.entity.ChatMessageFeedback;
import java.time.LocalDateTime;

public record ChatFeedbackVO(
        Long id,
        Long messageId,
        String feedbackType,
        String reason,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ChatFeedbackVO from(ChatMessageFeedback entity) {
        return new ChatFeedbackVO(
                entity.getId(),
                entity.getMessageId(),
                entity.getFeedbackType(),
                entity.getReason(),
                entity.getRemark(),
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }
}
