package com.knowflow.vo;

import java.time.LocalDateTime;

public record AdminFeedbackVO(
        Long feedbackId,
        Long messageId,
        Long sessionId,
        Long userId,
        String username,
        String feedbackType,
        String reason,
        String remark,
        String question,
        String answer,
        String answerType,
        Long knowledgeBaseId,
        String knowledgeBaseName,
        Long documentId,
        String documentName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
