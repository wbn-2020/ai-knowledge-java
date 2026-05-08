package com.knowflow.modules.knowledge;

import com.knowflow.modules.chat.ChatSessionVO;
import com.knowflow.modules.document.DocumentVO;

import java.util.List;

public record KnowledgeBaseDetailVO(
        KnowledgeBaseVO knowledgeBase,
        List<DocumentVO> recentDocuments,
        List<ChatSessionVO> recentSessions,
        String processingStatus
) {
}
