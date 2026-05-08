package com.knowflow.modules.summary;

import com.knowflow.infrastructure.ai.LlmClient;
import com.knowflow.modules.document.Document;
import com.knowflow.modules.document.DocumentChunkRepository;
import com.knowflow.modules.document.DocumentService;
import com.knowflow.modules.knowledge.KnowledgeBaseService;
import com.knowflow.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class SummaryService {
    private final DocumentService documentService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentChunkRepository chunkRepository;
    private final LlmClient llmClient;

    public SummaryService(DocumentService documentService, KnowledgeBaseService knowledgeBaseService, DocumentChunkRepository chunkRepository, LlmClient llmClient) {
        this.documentService = documentService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.chunkRepository = chunkRepository;
        this.llmClient = llmClient;
    }

    public SummaryVO documentSummary(Long documentId) {
        Document document = documentService.requireOwned(documentId);
        String content = chunkRepository.findByDocumentIdAndDeletedFalseOrderByChunkIndexAsc(documentId).stream()
                .limit(8)
                .map(chunk -> chunk.getContent())
                .collect(Collectors.joining("\n\n"));
        return new SummaryVO(documentId, "DOCUMENT", llmClient.complete("请总结以下文档内容，保持简洁准确：\n" + content));
    }

    public SummaryVO knowledgeBaseSummary(Long knowledgeBaseId) {
        knowledgeBaseService.requireOwned(knowledgeBaseId);
        Long userId = SecurityUtils.getCurrentUserId();
        String content = chunkRepository.findByUserIdAndKnowledgeBaseIdAndDeletedFalse(userId, knowledgeBaseId).stream()
                .limit(12)
                .map(chunk -> chunk.getContent())
                .collect(Collectors.joining("\n\n"));
        return new SummaryVO(knowledgeBaseId, "KNOWLEDGE_BASE", llmClient.complete("请总结以下知识库内容，输出核心主题和重要内容概览：\n" + content));
    }
}
