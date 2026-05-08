package com.knowflow.modules.admin;

public record AdminOverviewVO(
        long userCount,
        long knowledgeBaseCount,
        long documentCount,
        long failedDocumentCount,
        long disabledKnowledgeBaseCount
) {
}
