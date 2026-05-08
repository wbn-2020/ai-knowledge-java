package com.knowflow.vo;

import com.knowflow.vo.ChatSessionVO;
import com.knowflow.vo.DocumentVO;
import com.knowflow.vo.KnowledgeBaseVO;

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
