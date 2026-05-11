package com.knowflow.vo;

import java.util.List;

public record AdminOverviewVO(
        long userCount,
        long knowledgeBaseCount,
        long documentCount,
        long qaCount,
        long todayUserCount,
        long todayDocumentCount,
        long todayQaCount,
        long failedDocumentCount,
        long disabledKnowledgeBaseCount,
        List<UserVO> recentUsers,
        List<KnowledgeBaseVO> recentKnowledgeBases,
        List<DocumentVO> recentDocuments,
        List<DocumentTaskVO> recentFailedTasks,
        List<LogVO> recentAiErrors
) {
}
