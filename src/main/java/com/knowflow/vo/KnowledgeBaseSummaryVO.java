package com.knowflow.vo;

import java.time.LocalDateTime;

public record KnowledgeBaseSummaryVO(
        Long knowledgeBaseId,
        String knowledgeBaseName,
        String summary,
        Integer coveredDocumentCount,
        String modelName,
        String status,
        String errorMessage,
        LocalDateTime generatedAt
) {
}
