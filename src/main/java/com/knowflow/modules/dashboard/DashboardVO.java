package com.knowflow.modules.dashboard;

import com.knowflow.modules.chat.ChatSessionVO;
import com.knowflow.modules.document.DocumentVO;
import com.knowflow.modules.knowledge.KnowledgeBaseVO;

import java.util.List;

public record DashboardVO(
        long knowledgeBaseCount,
        long documentCount,
        long chatSessionCount,
        List<KnowledgeBaseVO> recentKnowledgeBases,
        List<DocumentVO> recentDocuments,
        List<ChatSessionVO> recentSessions
) {
}
