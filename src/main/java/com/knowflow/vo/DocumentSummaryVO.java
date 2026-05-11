package com.knowflow.vo;

import java.time.LocalDateTime;

public record DocumentSummaryVO(
        Long documentId,
        String documentName,
        Long knowledgeBaseId,
        String summary,
        String modelName,
        String status,
        String errorMessage,
        LocalDateTime generatedAt
) {
}
