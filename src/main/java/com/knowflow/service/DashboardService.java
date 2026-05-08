package com.knowflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.entity.ChatSession;
import com.knowflow.entity.Document;
import com.knowflow.entity.KnowledgeBase;
import com.knowflow.mapper.ChatSessionRepository;
import com.knowflow.mapper.DocumentRepository;
import com.knowflow.mapper.KnowledgeBaseRepository;
import com.knowflow.security.SecurityUtils;
import com.knowflow.vo.ChatSessionVO;
import com.knowflow.vo.DashboardVO;
import com.knowflow.vo.DocumentVO;
import com.knowflow.vo.KnowledgeBaseVO;
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
