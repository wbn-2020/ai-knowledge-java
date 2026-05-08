package com.knowflow.modules.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.modules.chat.ChatSession;
import com.knowflow.modules.chat.ChatSessionRepository;
import com.knowflow.modules.chat.ChatSessionVO;
import com.knowflow.modules.document.Document;
import com.knowflow.modules.document.DocumentRepository;
import com.knowflow.modules.document.DocumentVO;
import com.knowflow.modules.knowledge.KnowledgeBase;
import com.knowflow.modules.knowledge.KnowledgeBaseRepository;
import com.knowflow.modules.knowledge.KnowledgeBaseVO;
import com.knowflow.security.SecurityUtils;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final ChatSessionRepository chatSessionRepository;

    public DashboardService(KnowledgeBaseRepository knowledgeBaseRepository, DocumentRepository documentRepository, ChatSessionRepository chatSessionRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.documentRepository = documentRepository;
        this.chatSessionRepository = chatSessionRepository;
    }

    public DashboardVO overview() {
        Long userId = SecurityUtils.getCurrentUserId();
        return new DashboardVO(
                knowledgeBaseRepository.countByUserIdAndDeletedFalse(userId),
                documentRepository.countByUserIdAndDeletedFalse(userId),
                chatSessionRepository.countByUserIdAndDeletedFalse(userId),
                knowledgeBaseRepository.selectPage(new Page<>(1, 5), new LambdaQueryWrapper<KnowledgeBase>().eq(KnowledgeBase::getUserId, userId).orderByDesc(KnowledgeBase::getUpdateTime)).getRecords().stream().map(KnowledgeBaseVO::from).toList(),
                documentRepository.selectPage(new Page<>(1, 5), new LambdaQueryWrapper<Document>().eq(Document::getUserId, userId).orderByDesc(Document::getCreateTime)).getRecords().stream().map(DocumentVO::from).toList(),
                chatSessionRepository.selectPage(new Page<>(1, 5), new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getUserId, userId).orderByDesc(ChatSession::getUpdateTime)).getRecords().stream().map(ChatSessionVO::from).toList()
        );
    }
}
