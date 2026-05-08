package com.knowflow.vo;

public record AdminOverviewVO(
        long userCount,
        long knowledgeBaseCount,
        long documentCount,
        long failedDocumentCount,
        long disabledKnowledgeBaseCount
) {
}
