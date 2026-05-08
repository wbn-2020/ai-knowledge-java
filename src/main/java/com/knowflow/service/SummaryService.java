package com.knowflow.service;

import com.knowflow.entity.Document;
import com.knowflow.infrastructure.ai.LlmClient;
import com.knowflow.mapper.DocumentChunkRepository;
import com.knowflow.security.SecurityUtils;
import com.knowflow.service.DocumentService;
import com.knowflow.service.KnowledgeBaseService;
import com.knowflow.vo.SummaryVO;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;



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
        return new SummaryVO(documentId, "DOCUMENT", llmClient.complete("Summarize the following document content clearly and accurately:\n" + content));
    }

    public SummaryVO knowledgeBaseSummary(Long knowledgeBaseId) {
        knowledgeBaseService.requireOwned(knowledgeBaseId);
        Long userId = SecurityUtils.getCurrentUserId();
        String content = chunkRepository.findByUserIdAndKnowledgeBaseIdAndDeletedFalse(userId, knowledgeBaseId).stream()
                .limit(12)
                .map(chunk -> chunk.getContent())
                .collect(Collectors.joining("\n\n"));
        return new SummaryVO(knowledgeBaseId, "KNOWLEDGE_BASE", llmClient.complete("Summarize this knowledge base and list the core topics and important points:\n" + content));
    }
}
