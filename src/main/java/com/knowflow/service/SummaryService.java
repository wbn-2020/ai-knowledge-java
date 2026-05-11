package com.knowflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowflow.common.BusinessException;
import com.knowflow.entity.AiModelConfig;
import com.knowflow.entity.Document;
import com.knowflow.entity.DocumentChunk;
import com.knowflow.entity.DocumentSummary;
import com.knowflow.entity.KeywordExtractResult;
import com.knowflow.entity.KnowledgeBase;
import com.knowflow.entity.KnowledgeBaseSummary;
import com.knowflow.enums.DocumentParseStatus;
import com.knowflow.infrastructure.ai.LlmClient;
import com.knowflow.mapper.DocumentChunkRepository;
import com.knowflow.mapper.DocumentRepository;
import com.knowflow.mapper.DocumentSummaryRepository;
import com.knowflow.mapper.KeywordExtractResultRepository;
import com.knowflow.mapper.KnowledgeBaseSummaryRepository;
import com.knowflow.security.SecurityUtils;
import com.knowflow.vo.DocumentSummaryVO;
import com.knowflow.vo.KeywordVO;
import com.knowflow.vo.KnowledgeBaseSummaryVO;
import com.knowflow.vo.SummaryVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SummaryService {
    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);
    private static final int DEFAULT_CONTEXT_MAX_LENGTH = 4000;
    private static final int DEFAULT_DOC_CHUNK_LIMIT = 8;
    private static final int KB_PER_DOC_CHUNK_LIMIT = 3;
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[.*]", Pattern.DOTALL);

    private final DocumentService documentService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentSummaryRepository documentSummaryRepository;
    private final KnowledgeBaseSummaryRepository knowledgeBaseSummaryRepository;
    private final KeywordExtractResultRepository keywordExtractResultRepository;
    private final ConfigService configService;
    private final RuntimeConfigService runtimeConfigService;
    private final LlmClient llmClient;
    private final LogService logService;
    private final ObjectMapper objectMapper;

    public SummaryService(DocumentService documentService,
                          KnowledgeBaseService knowledgeBaseService,
                          DocumentRepository documentRepository,
                          DocumentChunkRepository chunkRepository,
                          DocumentSummaryRepository documentSummaryRepository,
                          KnowledgeBaseSummaryRepository knowledgeBaseSummaryRepository,
                          KeywordExtractResultRepository keywordExtractResultRepository,
                          ConfigService configService,
                          RuntimeConfigService runtimeConfigService,
                          LlmClient llmClient,
                          LogService logService,
                          ObjectMapper objectMapper) {
        this.documentService = documentService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.documentSummaryRepository = documentSummaryRepository;
        this.knowledgeBaseSummaryRepository = knowledgeBaseSummaryRepository;
        this.keywordExtractResultRepository = keywordExtractResultRepository;
        this.configService = configService;
        this.runtimeConfigService = runtimeConfigService;
        this.llmClient = llmClient;
        this.logService = logService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DocumentSummaryVO generateDocumentSummary(Long documentId) {
        return doGenerateDocumentSummary(documentId);
    }

    @Transactional
    public DocumentSummaryVO regenerateDocumentSummary(Long documentId) {
        return doGenerateDocumentSummary(documentId);
    }

    public DocumentSummaryVO getDocumentSummaryV13(Long documentId) {
        Document document = requireSummaryReadyDocument(documentId);
        Long userId = SecurityUtils.getCurrentUserId();
        DocumentSummary summary = documentSummaryRepository.findByUserIdAndDocumentId(userId, documentId).orElse(null);
        if (summary == null) {
            return new DocumentSummaryVO(document.getId(), document.getName(), document.getKnowledgeBaseId(), "", null, null, null, null);
        }
        return new DocumentSummaryVO(document.getId(), document.getName(), document.getKnowledgeBaseId(),
                defaultText(summary.getSummary()), summary.getModelName(), summary.getStatus(), summary.getErrorMessage(), summary.getGeneratedAt());
    }

    @Transactional
    public KnowledgeBaseSummaryVO generateKnowledgeBaseSummary(Long knowledgeBaseId) {
        return doGenerateKnowledgeBaseSummary(knowledgeBaseId);
    }

    @Transactional
    public KnowledgeBaseSummaryVO regenerateKnowledgeBaseSummary(Long knowledgeBaseId) {
        return doGenerateKnowledgeBaseSummary(knowledgeBaseId);
    }

    public KnowledgeBaseSummaryVO getKnowledgeBaseSummaryV13(Long knowledgeBaseId) {
        KnowledgeBase kb = knowledgeBaseService.requireOwned(knowledgeBaseId);
        Long userId = SecurityUtils.getCurrentUserId();
        KnowledgeBaseSummary summary = knowledgeBaseSummaryRepository.findByUserIdAndKnowledgeBaseId(userId, knowledgeBaseId).orElse(null);
        if (summary == null) {
            return new KnowledgeBaseSummaryVO(kb.getId(), kb.getName(), "", 0, null, null, null, null);
        }
        return new KnowledgeBaseSummaryVO(kb.getId(), kb.getName(), defaultText(summary.getSummary()),
                summary.getCoveredDocumentCount(), summary.getModelName(), summary.getStatus(), summary.getErrorMessage(), summary.getGeneratedAt());
    }

    public List<KeywordVO> getDocumentKeywords(Long documentId) {
        Document document = requireSummaryReadyDocument(documentId);
        Long userId = SecurityUtils.getCurrentUserId();
        return keywordExtractResultRepository.findByUserIdAndTarget("DOCUMENT", document.getId(), userId).stream()
                .map(item -> new KeywordVO(item.getKeyword(), item.getWeight()))
                .toList();
    }

    @Transactional
    public List<KeywordVO> extractDocumentKeywords(Long documentId) {
        Document document = requireSummaryReadyDocument(documentId);
        return doExtractKeywords("DOCUMENT", document.getId(), document.getKnowledgeBaseId(),
                buildDocumentContext(document), "DOCUMENT_KEYWORDS:" + document.getName());
    }

    @Transactional
    public List<KeywordVO> reextractDocumentKeywords(Long documentId) {
        return extractDocumentKeywords(documentId);
    }

    public List<KeywordVO> getKnowledgeBaseKeywords(Long knowledgeBaseId) {
        knowledgeBaseService.requireOwned(knowledgeBaseId);
        Long userId = SecurityUtils.getCurrentUserId();
        return keywordExtractResultRepository.findByUserIdAndTarget("KNOWLEDGE_BASE", knowledgeBaseId, userId).stream()
                .map(item -> new KeywordVO(item.getKeyword(), item.getWeight()))
                .toList();
    }

    @Transactional
    public List<KeywordVO> extractKnowledgeBaseKeywords(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.requireOwned(knowledgeBaseId);
        ContextAndCoverage contextAndCoverage = buildKnowledgeBaseContext(knowledgeBase);
        if (!StringUtils.hasText(contextAndCoverage.context())) {
            throw BusinessException.badRequest("当前知识库没有可提取关键词的内容");
        }
        return doExtractKeywords("KNOWLEDGE_BASE", knowledgeBase.getId(), knowledgeBase.getId(),
                contextAndCoverage.context(), "KB_KEYWORDS:" + knowledgeBase.getName());
    }

    @Transactional
    public List<KeywordVO> reextractKnowledgeBaseKeywords(Long knowledgeBaseId) {
        return extractKnowledgeBaseKeywords(knowledgeBaseId);
    }

    // Backward-compatible old endpoints
    @Transactional
    public SummaryVO documentSummary(Long documentId) {
        DocumentSummaryVO vo = generateDocumentSummary(documentId);
        return new SummaryVO(vo.documentId(), null, vo.summary(), vo.generatedAt(), vo.generatedAt());
    }

    public SummaryVO getDocumentSummary(Long documentId) {
        DocumentSummaryVO vo = getDocumentSummaryV13(documentId);
        return new SummaryVO(vo.documentId(), null, vo.summary(), vo.generatedAt(), vo.generatedAt());
    }

    @Transactional
    public SummaryVO knowledgeBaseSummary(Long knowledgeBaseId) {
        KnowledgeBaseSummaryVO vo = generateKnowledgeBaseSummary(knowledgeBaseId);
        return new SummaryVO(null, vo.knowledgeBaseId(), vo.summary(), vo.generatedAt(), vo.generatedAt());
    }

    public SummaryVO getKnowledgeBaseSummary(Long knowledgeBaseId) {
        KnowledgeBaseSummaryVO vo = getKnowledgeBaseSummaryV13(knowledgeBaseId);
        return new SummaryVO(null, vo.knowledgeBaseId(), vo.summary(), vo.generatedAt(), vo.generatedAt());
    }

    private DocumentSummaryVO doGenerateDocumentSummary(Long documentId) {
        Document document = requireSummaryReadyDocument(documentId);
        Long userId = SecurityUtils.getCurrentUserId();
        AiModelConfig modelConfig = selectEnabledLlmConfig();
        String context = buildDocumentContext(document);
        if (!StringUtils.hasText(context)) {
            throw BusinessException.badRequest("当前文档没有可摘要内容");
        }

        String promptTemplate = configService.promptBySceneOrDefault("DOCUMENT_SUMMARY",
                "请基于以下文档内容生成结构化摘要，覆盖主题、核心观点、关键细节与适用场景。不要编造内容。");
        String prompt = promptTemplate + "\n\n【文档内容】\n" + context;
        long start = System.currentTimeMillis();

        DocumentSummary summary = documentSummaryRepository.findByUserIdAndDocumentId(userId, document.getId()).orElseGet(DocumentSummary::new);
        if (summary.getId() == null) {
            summary.setUserId(userId);
            summary.setDocumentId(document.getId());
            summary.setKnowledgeBaseId(document.getKnowledgeBaseId());
        }

        try {
            String summaryText = llmClient.complete(prompt, modelConfig);
            if (!StringUtils.hasText(summaryText)) {
                throw BusinessException.badRequest("文档摘要生成失败：模型返回为空");
            }
            summary.setSummary(summaryText.trim());
            summary.setModelName(modelConfig.getModelName());
            summary.setStatus(STATUS_SUCCESS);
            summary.setErrorMessage(null);
            summary.setGeneratedAt(LocalDateTime.now());
            upsertDocumentSummary(summary);

            logService.recordAiCall(userId, document.getKnowledgeBaseId(), null, modelConfig.getModelName(),
                    resolveModelType(modelConfig), modelConfig.getProvider(), "DOCUMENT_SUMMARY",
                    System.currentTimeMillis() - start, true, null, estimateTokens(prompt), estimateTokens(summaryText),
                    "DOCUMENT_SUMMARY:" + document.getName(), (int) chunkRepository.countByDocumentId(document.getId()), null,
                    null, null, null, true);
        } catch (RuntimeException ex) {
            summary.setModelName(modelConfig.getModelName());
            summary.setStatus(STATUS_FAILED);
            summary.setErrorMessage(ex.getMessage());
            summary.setGeneratedAt(LocalDateTime.now());
            upsertDocumentSummary(summary);

            logService.recordAiCall(userId, document.getKnowledgeBaseId(), null, modelConfig.getModelName(),
                    resolveModelType(modelConfig), modelConfig.getProvider(), "DOCUMENT_SUMMARY",
                    System.currentTimeMillis() - start, false, ex.getMessage(), estimateTokens(prompt), null,
                    "DOCUMENT_SUMMARY:" + document.getName(), (int) chunkRepository.countByDocumentId(document.getId()), null,
                    null, null, null, true);
            throw BusinessException.badRequest("文档摘要生成失败，请稍后重试");
        }

        return new DocumentSummaryVO(document.getId(), document.getName(), document.getKnowledgeBaseId(),
                summary.getSummary(), summary.getModelName(), summary.getStatus(), summary.getErrorMessage(), summary.getGeneratedAt());
    }

    private KnowledgeBaseSummaryVO doGenerateKnowledgeBaseSummary(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.requireOwned(knowledgeBaseId);
        Long userId = SecurityUtils.getCurrentUserId();
        AiModelConfig modelConfig = selectEnabledLlmConfig();
        ContextAndCoverage contextAndCoverage = buildKnowledgeBaseContext(knowledgeBase);
        if (!StringUtils.hasText(contextAndCoverage.context())) {
            throw BusinessException.badRequest("当前知识库没有可摘要内容");
        }

        String promptTemplate = configService.promptBySceneOrDefault("KB_SUMMARY",
                "请基于以下知识库内容输出主题概览、主要知识点、覆盖范围和适用场景，不要编造内容。");
        String prompt = promptTemplate + "\n\n【知识库内容】\n" + contextAndCoverage.context();
        long start = System.currentTimeMillis();

        KnowledgeBaseSummary summary = knowledgeBaseSummaryRepository.findByUserIdAndKnowledgeBaseId(userId, knowledgeBaseId)
                .orElseGet(KnowledgeBaseSummary::new);
        if (summary.getId() == null) {
            summary.setUserId(userId);
            summary.setKnowledgeBaseId(knowledgeBaseId);
        }

        try {
            String summaryText = llmClient.complete(prompt, modelConfig);
            if (!StringUtils.hasText(summaryText)) {
                throw BusinessException.badRequest("知识库摘要生成失败：模型返回为空");
            }
            summary.setSummary(summaryText.trim());
            summary.setCoveredDocumentCount(contextAndCoverage.coveredDocumentCount());
            summary.setModelName(modelConfig.getModelName());
            summary.setStatus(STATUS_SUCCESS);
            summary.setErrorMessage(null);
            summary.setGeneratedAt(LocalDateTime.now());
            upsertKnowledgeBaseSummary(summary);

            logService.recordAiCall(userId, knowledgeBaseId, null, modelConfig.getModelName(),
                    resolveModelType(modelConfig), modelConfig.getProvider(), "KB_SUMMARY",
                    System.currentTimeMillis() - start, true, null, estimateTokens(prompt), estimateTokens(summaryText),
                    "KB_SUMMARY:" + knowledgeBase.getName(), contextAndCoverage.rawChunkCount(), null,
                    null, null, null, true);
        } catch (RuntimeException ex) {
            summary.setCoveredDocumentCount(contextAndCoverage.coveredDocumentCount());
            summary.setModelName(modelConfig.getModelName());
            summary.setStatus(STATUS_FAILED);
            summary.setErrorMessage(ex.getMessage());
            summary.setGeneratedAt(LocalDateTime.now());
            upsertKnowledgeBaseSummary(summary);

            logService.recordAiCall(userId, knowledgeBaseId, null, modelConfig.getModelName(),
                    resolveModelType(modelConfig), modelConfig.getProvider(), "KB_SUMMARY",
                    System.currentTimeMillis() - start, false, ex.getMessage(), estimateTokens(prompt), null,
                    "KB_SUMMARY:" + knowledgeBase.getName(), contextAndCoverage.rawChunkCount(), null,
                    null, null, null, true);
            throw BusinessException.badRequest("知识库摘要生成失败，请稍后重试");
        }

        return new KnowledgeBaseSummaryVO(knowledgeBase.getId(), knowledgeBase.getName(), summary.getSummary(),
                summary.getCoveredDocumentCount(), summary.getModelName(), summary.getStatus(), summary.getErrorMessage(), summary.getGeneratedAt());
    }

    private List<KeywordVO> doExtractKeywords(String targetType,
                                              Long targetId,
                                              Long knowledgeBaseId,
                                              String context,
                                              String questionSummary) {
        Long userId = SecurityUtils.getCurrentUserId();
        AiModelConfig modelConfig = selectEnabledLlmConfig();

        String promptTemplate = configService.promptBySceneOrDefault("KEYWORD_EXTRACT",
                "请从给定内容中提取最多10个关键词，返回JSON数组，每项包含keyword和weight(0到1)。不要返回其他文本。");
        String prompt = promptTemplate + "\n\n【内容】\n" + context;
        long start = System.currentTimeMillis();

        try {
            String response = llmClient.complete(prompt, modelConfig);
            List<KeywordVO> keywords = parseKeywords(response);
            if (keywords.isEmpty()) {
                throw BusinessException.badRequest("关键词提取失败：模型返回为空或无法解析");
            }
            keywordExtractResultRepository.deleteByUserIdAndTarget(targetType, targetId, userId);
            for (KeywordVO keyword : keywords) {
                KeywordExtractResult row = new KeywordExtractResult();
                row.setTargetType(targetType);
                row.setTargetId(targetId);
                row.setKnowledgeBaseId(knowledgeBaseId);
                row.setUserId(userId);
                row.setKeyword(keyword.keyword());
                row.setWeight(keyword.weight());
                row.setModelName(modelConfig.getModelName());
                keywordExtractResultRepository.insert(row);
            }

            logService.recordAiCall(userId, knowledgeBaseId, null, modelConfig.getModelName(),
                    resolveModelType(modelConfig), modelConfig.getProvider(), "KEYWORD_EXTRACT",
                    System.currentTimeMillis() - start, true, null, estimateTokens(prompt), estimateTokens(response),
                    questionSummary, null, keywords.size(), null, null, null, true);
            return keywords;
        } catch (BusinessException ex) {
            logService.recordAiCall(userId, knowledgeBaseId, null, modelConfig.getModelName(),
                    resolveModelType(modelConfig), modelConfig.getProvider(), "KEYWORD_EXTRACT",
                    System.currentTimeMillis() - start, false, ex.getMessage(), estimateTokens(prompt), null,
                    questionSummary, null, 0, null, null, null, true);
            throw ex;
        } catch (RuntimeException ex) {
            logService.recordAiCall(userId, knowledgeBaseId, null, modelConfig.getModelName(),
                    resolveModelType(modelConfig), modelConfig.getProvider(), "KEYWORD_EXTRACT",
                    System.currentTimeMillis() - start, false, ex.getMessage(), estimateTokens(prompt), null,
                    questionSummary, null, 0, null, null, null, true);
            throw BusinessException.badRequest("关键词提取失败，请稍后重试");
        }
    }

    private Document requireSummaryReadyDocument(Long documentId) {
        Document document = documentService.requireOwned(documentId);
        if (document.getParseStatus() != DocumentParseStatus.SUCCESS) {
            throw BusinessException.badRequest("当前文档尚未解析成功，暂不可生成摘要或关键词");
        }
        long chunkCount = chunkRepository.countByDocumentId(documentId);
        if (chunkCount <= 0) {
            throw BusinessException.badRequest("当前文档没有可用切片内容");
        }
        return document;
    }

    private String buildDocumentContext(Document document) {
        int maxLength = contextMaxLength();
        List<DocumentChunk> chunks = chunkRepository.findByDocumentIdAndDeletedFalseOrderByChunkIndexAsc(document.getId());
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (DocumentChunk chunk : chunks) {
            if (!StringUtils.hasText(chunk.getContent())) {
                continue;
            }
            if (count >= DEFAULT_DOC_CHUNK_LIMIT) {
                break;
            }
            if (builder.length() + chunk.getContent().length() + 2 > maxLength) {
                break;
            }
            if (builder.length() != 0) {
                builder.append("\n\n");
            }
            builder.append(chunk.getContent());
            count++;
        }
        return builder.toString();
    }

    private ContextAndCoverage buildKnowledgeBaseContext(KnowledgeBase knowledgeBase) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Document> docs = documentRepository.findByUserIdAndKnowledgeBaseIdAndDeletedFalse(userId, knowledgeBase.getId()).stream()
                .filter(d -> d.getParseStatus() == DocumentParseStatus.SUCCESS)
                .toList();
        if (docs.isEmpty()) {
            throw BusinessException.badRequest("当前知识库没有解析成功的文档");
        }
        int maxLength = contextMaxLength();
        StringBuilder builder = new StringBuilder();
        int coveredDocCount = 0;
        int rawChunkCount = 0;
        for (Document doc : docs) {
            List<DocumentChunk> chunks = chunkRepository.findByDocumentIdAndDeletedFalseOrderByChunkIndexAsc(doc.getId());
            if (chunks.isEmpty()) {
                continue;
            }
            int used = 0;
            boolean docUsed = false;
            for (DocumentChunk chunk : chunks) {
                rawChunkCount++;
                if (!StringUtils.hasText(chunk.getContent())) {
                    continue;
                }
                if (used >= KB_PER_DOC_CHUNK_LIMIT) {
                    break;
                }
                String piece = "[" + doc.getName() + "#" + chunk.getChunkIndex() + "] " + chunk.getContent();
                if (builder.length() + piece.length() + 2 > maxLength) {
                    return new ContextAndCoverage(builder.toString(), coveredDocCount, rawChunkCount);
                }
                if (builder.length() != 0) {
                    builder.append("\n\n");
                }
                builder.append(piece);
                used++;
                docUsed = true;
            }
            if (docUsed) {
                coveredDocCount++;
            }
        }
        if (coveredDocCount == 0) {
            throw BusinessException.badRequest("当前知识库没有可摘要内容");
        }
        return new ContextAndCoverage(builder.toString(), coveredDocCount, rawChunkCount);
    }

    private List<KeywordVO> parseKeywords(String response) {
        if (!StringUtils.hasText(response)) {
            return Collections.emptyList();
        }
        String jsonPayload = extractJsonArray(response);
        if (!StringUtils.hasText(jsonPayload)) {
            return fallbackParseKeywords(response);
        }
        try {
            JsonNode node = objectMapper.readTree(jsonPayload);
            if (!node.isArray()) {
                return fallbackParseKeywords(response);
            }
            List<KeywordVO> result = new ArrayList<>();
            for (JsonNode item : node) {
                String keyword = text(item, "keyword");
                if (!StringUtils.hasText(keyword)) {
                    continue;
                }
                Double weight = number(item, "weight");
                if (weight == null) {
                    weight = defaultWeight(result.size());
                }
                result.add(new KeywordVO(keyword.trim(), normalizeWeight(weight)));
            }
            return trimKeywords(result);
        } catch (Exception ignored) {
            return fallbackParseKeywords(response);
        }
    }

    private List<KeywordVO> fallbackParseKeywords(String response) {
        try {
            List<String> lines = objectMapper.readValue(response, new TypeReference<List<String>>() {
            });
            List<KeywordVO> keywords = new ArrayList<>();
            for (String line : lines) {
                if (StringUtils.hasText(line)) {
                    keywords.add(new KeywordVO(line.trim(), defaultWeight(keywords.size())));
                }
            }
            return trimKeywords(keywords);
        } catch (Exception ignored) {
            String normalized = response.replace("\n", ",");
            String[] parts = normalized.split("[,，、;；]");
            List<KeywordVO> keywords = new ArrayList<>();
            for (String part : parts) {
                String keyword = part.replaceAll("[^\\p{L}\\p{N}_-]", "").trim();
                if (keyword.length() >= 2) {
                    keywords.add(new KeywordVO(keyword, defaultWeight(keywords.size())));
                }
            }
            return trimKeywords(keywords);
        }
    }

    private List<KeywordVO> trimKeywords(List<KeywordVO> keywords) {
        return keywords.stream()
                .filter(item -> StringUtils.hasText(item.keyword()))
                .collect(Collectors.toMap(item -> item.keyword().toLowerCase(Locale.ROOT), item -> item, (a, b) -> a))
                .values().stream()
                .sorted(Comparator.comparing(KeywordVO::weight).reversed())
                .limit(10)
                .toList();
    }

    private String extractJsonArray(String text) {
        Matcher matcher = JSON_ARRAY_PATTERN.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private Double number(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asDouble();
    }

    private Double defaultWeight(int index) {
        double value = 1.0d - (index * 0.08d);
        return value < 0.2d ? 0.2d : value;
    }

    private Double normalizeWeight(Double weight) {
        double value = weight == null ? 0.5d : weight;
        if (value > 1d) {
            value = value / 100d;
        }
        if (value < 0d) {
            value = 0d;
        }
        if (value > 1d) {
            value = 1d;
        }
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private int contextMaxLength() {
        int value = runtimeConfigService.intValue("rag.contextMaxLength", DEFAULT_CONTEXT_MAX_LENGTH);
        return value > 0 ? value : DEFAULT_CONTEXT_MAX_LENGTH;
    }

    private AiModelConfig selectEnabledLlmConfig() {
        return configService.requireEnabledLlmConfig();
    }

    private void upsertDocumentSummary(DocumentSummary summary) {
        if (summary.getId() == null) {
            documentSummaryRepository.insert(summary);
        } else {
            documentSummaryRepository.updateById(summary);
        }
    }

    private void upsertKnowledgeBaseSummary(KnowledgeBaseSummary summary) {
        if (summary.getId() == null) {
            knowledgeBaseSummaryRepository.insert(summary);
        } else {
            knowledgeBaseSummaryRepository.updateById(summary);
        }
    }

    private String resolveModelType(AiModelConfig config) {
        if (StringUtils.hasText(config.getModelType())) {
            return config.getModelType().toUpperCase();
        }
        String modelName = config.getModelName() == null ? "" : config.getModelName().toLowerCase(Locale.ROOT);
        return modelName.contains("embedding") ? "EMBEDDING" : "LLM";
    }

    private Integer estimateTokens(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        return Math.max(1, text.length() / 4);
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private record ContextAndCoverage(String context, int coveredDocumentCount, int rawChunkCount) {
    }
}

