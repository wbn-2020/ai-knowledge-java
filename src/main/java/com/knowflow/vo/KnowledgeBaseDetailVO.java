package com.knowflow.vo;

import com.knowflow.vo.ChatSessionVO;
import com.knowflow.vo.DocumentVO;

import java.util.List;

public record KnowledgeBaseDetailVO(
        KnowledgeBaseVO knowledgeBase,
        List<DocumentVO> recentDocuments,
        List<ChatSessionVO> recentSessions,
        String processingStatus
) {
}
