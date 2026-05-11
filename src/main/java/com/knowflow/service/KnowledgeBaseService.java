package com.knowflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.common.BusinessException;
import com.knowflow.common.PageResponse;
import com.knowflow.dto.KnowledgeBaseRequest;
import com.knowflow.entity.Document;
import com.knowflow.entity.KnowledgeBase;
import com.knowflow.enums.DocumentParseStatus;
import com.knowflow.enums.KnowledgeBaseStatus;
import com.knowflow.mapper.ChatSessionRepository;
import com.knowflow.mapper.DocumentChunkRepository;
import com.knowflow.mapper.DocumentProcessTaskRepository;
import com.knowflow.mapper.DocumentRepository;
import com.knowflow.mapper.KnowledgeBaseRepository;
import com.knowflow.security.SecurityUtils;
import com.knowflow.vo.ChatSessionVO;
import com.knowflow.vo.DocumentVO;
import com.knowflow.vo.KnowledgeBaseDetailVO;
import com.knowflow.vo.KnowledgeBaseVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
public class KnowledgeBaseService {
    private final KnowledgeBaseRepository repository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentProcessTaskRepository taskRepository;
    private final ChatSessionRepository chatSessionRepository;

    public KnowledgeBaseService(KnowledgeBaseRepository repository,
                                DocumentRepository documentRepository,
                                DocumentChunkRepository chunkRepository,
                                DocumentProcessTaskRepository taskRepository,
                                ChatSessionRepository chatSessionRepository) {
        this.repository = repository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.taskRepository = taskRepository;
        this.chatSessionRepository = chatSessionRepository;
    }

    @Transactional
    public KnowledgeBaseVO create(KnowledgeBaseRequest request) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setUserId(SecurityUtils.getCurrentUserId());
        fill(kb, request);
        repository.insert(kb);
        return KnowledgeBaseVO.from(kb);
    }

    public PageResponse<KnowledgeBaseVO> page(String keyword, int pageNo, int pageSize, String sortBy) {
        Long userId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<KnowledgeBase> query = new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getUserId, userId)
                .eq(KnowledgeBase::getDeleted, false)
                .like(keyword != null && !keyword.isBlank(), KnowledgeBase::getName, keyword);
        if ("createTime".equalsIgnoreCase(sortBy)) {
            query.orderByDesc(KnowledgeBase::getCreateTime);
        } else {
            query.orderByDesc(KnowledgeBase::getUpdateTime);
        }
        return PageResponse.of(repository.selectPage(new Page<>(pageNo, pageSize), query).convert(KnowledgeBaseVO::from));
    }

    public KnowledgeBaseVO detail(Long id) {
        return KnowledgeBaseVO.from(requireOwned(id));
    }

    public KnowledgeBaseDetailVO detailFull(Long id) {
        KnowledgeBase kb = requireOwned(id);
        Long userId = SecurityUtils.getCurrentUserId();
        List<DocumentVO> recentDocuments = documentRepository.findRecentByUserIdAndKnowledgeBaseId(userId, id, 10)
                .stream().map(DocumentVO::from).toList();
        List<ChatSessionVO> recentSessions = chatSessionRepository.findRecentByUserIdAndKnowledgeBaseId(userId, id, 10)
                .stream().map(ChatSessionVO::from).toList();
        return new KnowledgeBaseDetailVO(KnowledgeBaseVO.from(kb), recentDocuments, recentSessions, processingStatus(kb, userId));
    }

    @Transactional
    public KnowledgeBaseVO update(Long id, KnowledgeBaseRequest request) {
        KnowledgeBase kb = requireOwned(id);
        fill(kb, request);
        repository.updateById(kb);
        return KnowledgeBaseVO.from(kb);
    }

    @Transactional
    public void delete(Long id) {
        KnowledgeBase kb = requireOwned(id);
        chunkRepository.deleteByKnowledgeBaseId(id);
        taskRepository.deleteByKnowledgeBaseId(id);
        documentRepository.deleteByKnowledgeBaseId(id);
        chatSessionRepository.deleteByKnowledgeBaseId(id);
        kb.setDeleted(true);
        kb.setDocumentCount(0);
        repository.updateById(kb);
    }

    public KnowledgeBase requireOwned(Long id) {
        return repository.findByIdAndUserIdAndDeletedFalse(id, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> BusinessException.notFound("knowledge base not found"));
    }

    private String processingStatus(KnowledgeBase kb, Long userId) {
        if (kb.getStatus() == KnowledgeBaseStatus.DISABLED) {
            return "DISABLED";
        }
        List<Document> documents = documentRepository.findByUserIdAndKnowledgeBaseIdAndDeletedFalse(userId, kb.getId());
        if (documents.stream().anyMatch(doc -> doc.getParseStatus() == DocumentParseStatus.FAILED)) {
            return "FAILED";
        }
        if (documents.stream().anyMatch(doc -> doc.getParseStatus() == DocumentParseStatus.PENDING || doc.getParseStatus() == DocumentParseStatus.PARSING)) {
            return "PROCESSING";
        }
        return "NORMAL";
    }

    private void fill(KnowledgeBase kb, KnowledgeBaseRequest request) {
        kb.setName(request.name());
        kb.setDescription(request.description());
        kb.setIcon(request.icon());
        kb.setCategory(request.category());
    }
}
