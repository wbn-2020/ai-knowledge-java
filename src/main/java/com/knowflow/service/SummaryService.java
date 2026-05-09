package com.knowflow.service;

import com.knowflow.entity.AiModelConfig;
import com.knowflow.entity.Document;
import com.knowflow.entity.DocumentSummary;
import com.knowflow.entity.KnowledgeBaseSummary;
import com.knowflow.infrastructure.ai.LlmClient;
import com.knowflow.mapper.DocumentChunkRepository;
import com.knowflow.mapper.DocumentSummaryRepository;
import com.knowflow.mapper.KnowledgeBaseSummaryRepository;
import com.knowflow.security.SecurityUtils;
import com.knowflow.vo.SummaryVO;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SummaryService {
    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

    private final DocumentService documentService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentSummaryRepository documentSummaryRepository;
    private final KnowledgeBaseSummaryRepository knowledgeBaseSummaryRepository;
    private final ConfigService configService;
    private final LlmClient llmClient;

    public SummaryService(DocumentService documentService,
                          KnowledgeBaseService knowledgeBaseService,
                          DocumentChunkRepository chunkRepository,
                          DocumentSummaryRepository documentSummaryRepository,
                          KnowledgeBaseSummaryRepository knowledgeBaseSummaryRepository,
                          ConfigService configService,
                          LlmClient llmClient) {
        this.documentService = documentService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.chunkRepository = chunkRepository;
        this.documentSummaryRepository = documentSummaryRepository;
        this.knowledgeBaseSummaryRepository = knowledgeBaseSummaryRepository;
        this.configService = configService;
        this.llmClient = llmClient;
    }

    public SummaryVO documentSummary(Long documentId) {
        Document document = documentService.requireOwned(documentId);
        AiModelConfig modelConfig = selectEnabledLlmConfig();
        String content = chunkRepository.findByDocumentIdAndDeletedFalseOrderByChunkIndexAsc(documentId).stream()
                .limit(8)
                .map(chunk -> chunk.getContent())
                .collect(Collectors.joining("\n\n"));
        String summaryText = llmClient.complete("Summarize the following document content clearly and accurately:\n" + content, modelConfig);
        Long userId = SecurityUtils.getCurrentUserId();
        DocumentSummary summary = documentSummaryRepository.findByUserIdAndDocumentId(userId, document.getId()).orElseGet(DocumentSummary::new);
        if (summary.getId() == null) {
            summary.setUserId(userId);
            summary.setDocumentId(document.getId());
            summary.setSummary(summaryText);
            documentSummaryRepository.insert(summary);
        } else {
            summary.setSummary(summaryText);
            documentSummaryRepository.updateById(summary);
        }
        log.debug("Document summary generated: documentId={}, summaryLength={}, saved={}", documentId, summaryText.length(), true);
        return new SummaryVO(documentId, null, summaryText, summary.getCreateTime(), summary.getUpdateTime());
    }

    public SummaryVO getDocumentSummary(Long documentId) {
        Document document = documentService.requireOwned(documentId);
        Long userId = SecurityUtils.getCurrentUserId();
        DocumentSummary summary = documentSummaryRepository.findByUserIdAndDocumentId(userId, document.getId()).orElse(null);
        boolean found = summary != null && StringUtils.hasText(summary.getSummary());
        log.debug("Document summary queried: documentId={}, found={}", documentId, found);
        if (summary == null) {
            return new SummaryVO(documentId, null, "", null, null);
        }
        return new SummaryVO(documentId, null, summary.getSummary(), summary.getCreateTime(), summary.getUpdateTime());
    }

    public SummaryVO knowledgeBaseSummary(Long knowledgeBaseId) {
        knowledgeBaseService.requireOwned(knowledgeBaseId);
        AiModelConfig modelConfig = selectEnabledLlmConfig();
        Long userId = SecurityUtils.getCurrentUserId();
        String content = chunkRepository.findByUserIdAndKnowledgeBaseIdAndDeletedFalse(userId, knowledgeBaseId).stream()
                .limit(12)
                .map(chunk -> chunk.getContent())
                .collect(Collectors.joining("\n\n"));
        String summaryText = llmClient.complete("Summarize this knowledge base and list the core topics and important points:\n" + content, modelConfig);
        KnowledgeBaseSummary summary = knowledgeBaseSummaryRepository.findByUserIdAndKnowledgeBaseId(userId, knowledgeBaseId)
                .orElseGet(KnowledgeBaseSummary::new);
        if (summary.getId() == null) {
            summary.setUserId(userId);
            summary.setKnowledgeBaseId(knowledgeBaseId);
            summary.setSummary(summaryText);
            knowledgeBaseSummaryRepository.insert(summary);
        } else {
            summary.setSummary(summaryText);
            knowledgeBaseSummaryRepository.updateById(summary);
        }
        log.debug("Knowledge base summary generated: knowledgeBaseId={}, summaryLength={}, saved={}",
                knowledgeBaseId, summaryText.length(), true);
        return new SummaryVO(null, knowledgeBaseId, summaryText, summary.getCreateTime(), summary.getUpdateTime());
    }

    public SummaryVO getKnowledgeBaseSummary(Long knowledgeBaseId) {
        knowledgeBaseService.requireOwned(knowledgeBaseId);
        Long userId = SecurityUtils.getCurrentUserId();
        KnowledgeBaseSummary summary = knowledgeBaseSummaryRepository.findByUserIdAndKnowledgeBaseId(userId, knowledgeBaseId).orElse(null);
        boolean found = summary != null && StringUtils.hasText(summary.getSummary());
        log.debug("Knowledge base summary queried: knowledgeBaseId={}, found={}", knowledgeBaseId, found);
        if (summary == null) {
            return new SummaryVO(null, knowledgeBaseId, "", null, null);
        }
        return new SummaryVO(null, knowledgeBaseId, summary.getSummary(), summary.getCreateTime(), summary.getUpdateTime());
    }

    private AiModelConfig selectEnabledLlmConfig() {
        AiModelConfig config = configService.requireEnabledLlmConfig();
        boolean hasApiKey = StringUtils.hasText(config.getApiKey());
        log.debug("Summary selected model: selectedModelId={}, provider={}, modelType={}, modelName={}, baseUrl={}, hasApiKey={}",
                config.getId(), config.getProvider(), resolveModelType(config), config.getModelName(), config.getBaseUrl(), hasApiKey);
        return config;
    }

    private String resolveModelType(AiModelConfig config) {
        if (StringUtils.hasText(config.getModelType())) {
            return config.getModelType().toUpperCase();
        }
        String modelName = config.getModelName() == null ? "" : config.getModelName().toLowerCase();
        return modelName.contains("embedding") ? "EMBEDDING" : "LLM";
    }
}
