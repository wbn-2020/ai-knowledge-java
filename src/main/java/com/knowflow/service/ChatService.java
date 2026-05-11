package com.knowflow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowflow.common.BusinessException;
import com.knowflow.common.PageResponse;
import com.knowflow.dto.AskRequest;
import com.knowflow.dto.DocumentAskRequest;
import com.knowflow.dto.FeedbackRequest;
import com.knowflow.dto.MultiKnowledgeAskRequest;
import com.knowflow.entity.AiModelConfig;
import com.knowflow.entity.ChatFeedback;
import com.knowflow.entity.ChatMessage;
import com.knowflow.entity.ChatMessageReference;
import com.knowflow.entity.ChatSession;
import com.knowflow.entity.Document;
import com.knowflow.entity.DocumentChunk;
import com.knowflow.entity.KnowledgeBase;
import com.knowflow.enums.AnswerType;
import com.knowflow.enums.DocumentParseStatus;
import com.knowflow.enums.KnowledgeBaseStatus;
import com.knowflow.enums.MessageRole;
import com.knowflow.enums.NoAnswerReason;
import com.knowflow.infrastructure.ai.LlmClient;
import com.knowflow.mapper.ChatFeedbackRepository;
import com.knowflow.mapper.ChatMessageReferenceRepository;
import com.knowflow.mapper.ChatMessageRepository;
import com.knowflow.mapper.ChatSessionRepository;
import com.knowflow.mapper.DocumentChunkRepository;
import com.knowflow.mapper.DocumentRepository;
import com.knowflow.security.SecurityUtils;
import com.knowflow.vo.AskVO;
import com.knowflow.vo.ChatMessageVO;
import com.knowflow.vo.ChatSessionVO;
import com.knowflow.vo.KnowledgeBaseVO;
import com.knowflow.vo.ReferenceVO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final String NO_RELEVANT_ANSWER = "当前知识库未找到与问题相关的内容，请上传相关文档或换个问题再试。";
    private static final String KB_EMPTY_ANSWER = "当前知识库还没有可问答文档，请先上传并完成解析。";
    private static final String DOC_PROCESSING_ANSWER = "当前知识库仍有文档处理中，请等待解析完成后再提问。";
    private static final String NO_EVIDENCE_PROMPT_ANSWER = "当前知识库未找到相关依据。";
    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.55d;
    private static final int DEFAULT_CONTEXT_MAX_LENGTH = 4000;
    private static final String GENERAL_ANSWER_PROMPT = "你是 KnowFlow AI 助手。用户允许你基于通用知识回答，请直接、准确、简洁地回答问题。不要提及你没有知识库上下文。";

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatMessageReferenceRepository referenceRepository;
    private final ChatFeedbackRepository feedbackRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final LlmClient llmClient;
    private final LogService logService;
    private final ConfigService configService;
    private final RuntimeConfigService runtimeConfigService;
    private final RetrievalService retrievalService;
    private final ObjectMapper objectMapper;
    private final int defaultTopK;
    private final double defaultSimilarityThreshold;

    public ChatService(KnowledgeBaseService knowledgeBaseService,
                       DocumentService documentService,
                       ChatSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       ChatMessageReferenceRepository referenceRepository,
                       ChatFeedbackRepository feedbackRepository,
                       DocumentChunkRepository chunkRepository,
                       DocumentRepository documentRepository,
                       LlmClient llmClient,
                       LogService logService,
                       ConfigService configService,
                       RuntimeConfigService runtimeConfigService,
                       RetrievalService retrievalService,
                       ObjectMapper objectMapper,
                       @Value("${knowflow.rag.top-k:5}") int topK,
                       @Value("${knowflow.rag.similarity-threshold:0.55}") double similarityThreshold) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentService = documentService;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.referenceRepository = referenceRepository;
        this.feedbackRepository = feedbackRepository;
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.llmClient = llmClient;
        this.logService = logService;
        this.configService = configService;
        this.runtimeConfigService = runtimeConfigService;
        this.retrievalService = retrievalService;
        this.objectMapper = objectMapper;
        this.defaultTopK = topK > 0 ? topK : DEFAULT_TOP_K;
        this.defaultSimilarityThreshold = similarityThreshold > 0 ? similarityThreshold : DEFAULT_SIMILARITY_THRESHOLD;
    }

    @Transactional
    public AskVO ask(AskRequest request) {
        KnowledgeBase kb = requireUsableKnowledgeBase(request.knowledgeBaseId());
        Long userId = SecurityUtils.getCurrentUserId();
        ensureKnowledgeBaseReady(kb);
        ChatSession session = resolveSession(request.sessionId(), kb.getId(), userId, request.question());
        RetrievalResult retrievalResult = retrieveByKnowledgeBases(userId, List.of(kb.getId()), request.question());
        NoAnswerReason emptyReason = kb.getDocumentCount() != null && kb.getDocumentCount() == 0
                ? NoAnswerReason.EMPTY_KNOWLEDGE_BASE
                : null;
        return answer(userId, session, request.question(), retrievalResult, emptyReason,
                Boolean.TRUE.equals(request.allowGeneralAnswer()));
    }

    @Transactional
    public AskVO askDocument(DocumentAskRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Document document = documentService.requireOwned(request.documentId());
        requireUsableKnowledgeBase(document.getKnowledgeBaseId());
        ensureKnowledgeBaseReadyById(document.getKnowledgeBaseId());
        ChatSession session = resolveSession(request.sessionId(), document.getKnowledgeBaseId(), userId, request.question());
        return answer(userId, session, request.question(),
                retrieveByDocument(userId, request.documentId(), request.question()),
                null,
                false);
    }

    @Transactional
    public AskVO askMulti(MultiKnowledgeAskRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Long> kbIds = request.knowledgeBaseIds().stream().distinct().toList();
        if (kbIds.isEmpty()) {
            throw BusinessException.badRequest("knowledgeBaseIds cannot be empty");
        }
        List<KnowledgeBase> knowledgeBases = kbIds.stream().map(this::requireUsableKnowledgeBase).toList();
        knowledgeBases.forEach(this::ensureKnowledgeBaseReady);
        ChatSession session = resolveSession(request.sessionId(), kbIds.get(0), userId, request.question());
        int totalDocumentCount = knowledgeBases.stream()
                .map(KnowledgeBase::getDocumentCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        boolean emptyKnowledgeBase = totalDocumentCount == 0;
        return answer(userId, session, request.question(),
                retrieveByKnowledgeBases(userId, kbIds, request.question()),
                emptyKnowledgeBase ? NoAnswerReason.EMPTY_KNOWLEDGE_BASE : null,
                false);
    }

    public PageResponse<ChatSessionVO> sessions(int pageNo, int pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        Page<ChatSession> page = sessionRepository.findByUserIdAndDeletedFalse(userId, new Page<>(pageNo, pageSize));
        Map<Long, String> kbNames = knowledgeBaseService.page("", 1, 1000, "updateTime").list().stream()
                .collect(Collectors.toMap(KnowledgeBaseVO::id, KnowledgeBaseVO::name));
        return PageResponse.of(page.convert(session -> {
            List<ChatMessage> sessionMessages = messagesForSession(userId, session.getId());
            String latestQuestion = sessionMessages.stream()
                    .filter(m -> m.getRole() == MessageRole.USER)
                    .reduce((a, b) -> b)
                    .map(ChatMessage::getContent)
                    .orElse("");
            return new ChatSessionVO(
                    session.getId(),
                    session.getKnowledgeBaseId(),
                    kbNames.getOrDefault(session.getKnowledgeBaseId(), ""),
                    session.getTitle(),
                    latestQuestion,
                    (long) sessionMessages.size(),
                    session.getCreateTime(),
                    session.getUpdateTime(),
                    session.getUpdateTime()
            );
        }));
    }

    public List<ChatMessageVO> messages(Long sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        requireSession(sessionId, userId);
        return messagesForSession(userId, sessionId).stream()
                .map(message -> {
                    if (message.getRole() != MessageRole.ASSISTANT) {
                        return new ChatMessageVO(
                                message.getId(),
                                message.getRole(),
                                message.getContent(),
                                message.getModelName(),
                                null,
                                Boolean.FALSE,
                                message.getCreateTime(),
                                Collections.emptyList()
                        );
                    }

                    List<ReferenceVO> references = readMessageReferences(userId, message);
                    String answerType = resolveHistoryAnswerType(message, references);
                    Boolean canUseGeneralAnswer = message.getCanUseGeneralAnswer();
                    if (canUseGeneralAnswer == null) {
                        canUseGeneralAnswer = AnswerType.NO_CONTEXT.name().equals(answerType);
                    }
                    return new ChatMessageVO(
                            message.getId(),
                            message.getRole(),
                            message.getContent(),
                            message.getModelName(),
                            answerType,
                            canUseGeneralAnswer,
                            message.getCreateTime(),
                            references
                    );
                })
                .toList();
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        ChatSession session = requireSession(sessionId, SecurityUtils.getCurrentUserId());
        session.setDeleted(true);
        sessionRepository.updateById(session);
    }

    @Transactional
    public ChatSessionVO renameSession(Long sessionId, String title) {
        ChatSession session = requireSession(sessionId, SecurityUtils.getCurrentUserId());
        session.setTitle(title);
        sessionRepository.updateById(session);
        return ChatSessionVO.from(session);
    }

    @Transactional
    public void clearSession(Long sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        requireSession(sessionId, userId);
        messagesForSession(userId, sessionId).forEach(message -> {
            message.setDeleted(true);
            messageRepository.updateById(message);
        });
    }

    @Transactional
    public AskVO regenerate(Long sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ChatSession session = requireSession(sessionId, userId);
        ensureKnowledgeBaseReadyById(session.getKnowledgeBaseId());
        String lastQuestion = messagesForSession(userId, sessionId).stream()
                .filter(message -> message.getRole() == MessageRole.USER)
                .reduce((a, b) -> b)
                .map(ChatMessage::getContent)
                .orElseThrow(() -> BusinessException.badRequest("no question to regenerate"));
        return answer(userId, session, lastQuestion,
                retrieveByKnowledgeBases(userId, List.of(session.getKnowledgeBaseId()), lastQuestion),
                null,
                false);
    }

    @Transactional
    public void feedback(FeedbackRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ChatMessage message = messageRepository.selectById(request.messageId());
        if (message == null || !userId.equals(message.getUserId())) {
            throw BusinessException.notFound("message not found");
        }
        ChatFeedback feedback = new ChatFeedback();
        feedback.setUserId(userId);
        feedback.setMessageId(request.messageId());
        feedback.setFeedbackType(request.feedbackType());
        feedback.setReason(request.reason());
        feedbackRepository.insert(feedback);
    }

    public String exportMarkdown(Long sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        ChatSession session = requireSession(sessionId, userId);
        StringBuilder markdown = new StringBuilder("# ").append(session.getTitle()).append("\n\n");
        for (ChatMessage message : messagesForSession(userId, sessionId)) {
            markdown.append("## ").append(message.getRole() == MessageRole.USER ? "Question" : "Answer").append("\n\n");
            markdown.append(message.getContent()).append("\n\n");
            List<ChatMessageReference> refs = referenceRepository.findByUserIdAndMessageIdAndDeletedFalse(userId, message.getId());
            if (!refs.isEmpty()) {
                markdown.append("### References\n\n");
                refs.forEach(ref -> markdown.append("- ").append(ref.getDocumentName()).append(" #").append(ref.getChunkId()).append("\n"));
                markdown.append("\n");
            }
        }
        return markdown.toString();
    }

    public byte[] exportPdf(Long sessionId) {
        String text = exportMarkdown(sessionId);
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(document, page);
            stream.setFont(PDType1Font.HELVETICA, 10);
            stream.beginText();
            stream.newLineAtOffset(40, 740);
            float y = 740;
            for (String line : text.replace("\r", "").split("\n")) {
                if (y < 50) {
                    stream.endText();
                    stream.close();
                    page = new PDPage(PDRectangle.LETTER);
                    document.addPage(page);
                    stream = new PDPageContentStream(document, page);
                    stream.setFont(PDType1Font.HELVETICA, 10);
                    stream.beginText();
                    stream.newLineAtOffset(40, 740);
                    y = 740;
                }
                stream.showText(sanitizePdfLine(line));
                stream.newLineAtOffset(0, -14);
                y -= 14;
            }
            stream.endText();
            stream.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw BusinessException.badRequest("failed to export PDF");
        }
    }

    public byte[] exportWord(Long sessionId) {
        String text = exportMarkdown(sessionId);
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (String line : text.replace("\r", "").split("\n")) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(line);
            }
            document.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw BusinessException.badRequest("failed to export Word document");
        }
    }

    private AskVO answer(Long userId,
                         ChatSession session,
                         String question,
                         RetrievalResult retrievalResult,
                         NoAnswerReason preferredNoAnswerReason,
                         boolean allowGeneralAnswer) {
        saveMessage(userId, session.getId(), MessageRole.USER, question, null, null, Boolean.FALSE, Collections.emptyList());

        double threshold = similarityThreshold();
        List<ScoredChunk> sortedChunks = retrievalResult.scoredChunks().stream()
                .sorted((a, b) -> Double.compare(b.finalScore(), a.finalScore()))
                .toList();
        List<ScoredChunk> validChunks = sortedChunks.stream()
                .filter(chunk -> chunk.finalScore() >= threshold)
                .limit(topK())
                .toList();
        boolean hasChunks = retrievalResult.totalChunks() > 0;
        boolean hasReliableEvidence = !validChunks.isEmpty()
                && retrievalResult.maxFinalScore() >= threshold;
        log.debug("Chat retrieval summary: sessionId={}, kbId={}, question='{}', topK={}, threshold={}, rawRetrieveCount={}, effectiveRetrieveCount={}, maxSimilarityScore={}, allowGeneralAnswer={}",
                session.getId(), session.getKnowledgeBaseId(), question, topK(), threshold,
                retrievalResult.totalChunks(), validChunks.size(), retrievalResult.maxFinalScore(), allowGeneralAnswer);

        if (!hasReliableEvidence) {
            if (allowGeneralAnswer && isGeneralAnswerAllowed()) {
                return generalAnswer(userId, session, question, retrievalResult, validChunks.size(), threshold);
            }
            NoAnswerReason noAnswerReason = !hasChunks
                    ? (preferredNoAnswerReason == null ? NoAnswerReason.NO_CHUNKS : preferredNoAnswerReason)
                    : NoAnswerReason.LOW_SIMILARITY;
            String noContextAnswer = resolveNoContextAnswer(noAnswerReason);
            logService.recordAiCall(
                    userId, session.getKnowledgeBaseId(), session.getId(), null,
                    "LLM", "KNOWFLOW", "CHAT_QA",
                    0L, true, "NO_RELEVANT_CONTEXT", null, null,
                    question, retrievalResult.totalChunks(), validChunks.size(), topK(), threshold,
                    retrievalResult.maxFinalScore(), false);
            ChatMessage assistantMessage = saveMessage(
                    userId,
                    session.getId(),
                    MessageRole.ASSISTANT,
                    noContextAnswer,
                    null,
                    AnswerType.NO_CONTEXT,
                    Boolean.TRUE,
                    Collections.emptyList()
            );
            return new AskVO(
                    session.getId(),
                    question,
                    noContextAnswer,
                    AnswerType.NO_CONTEXT.name(),
                    true,
                    assistantMessage.getId(),
                    Collections.emptyList(),
                    false,
                    false,
                    noAnswerReason.name()
            );
        }

        AiModelConfig modelConfig = configService.requireEnabledLlmConfig();
        String prompt = buildPrompt(question, validChunks, session.getId(), userId);
        long start = System.currentTimeMillis();
        String answer;
        try {
            answer = llmClient.complete(prompt, modelConfig);
            logService.recordAiCall(
                    userId, session.getKnowledgeBaseId(), session.getId(), modelConfig.getModelName(),
                    resolveModelType(modelConfig), modelConfig.getProvider(), "CHAT_QA",
                    System.currentTimeMillis() - start, true, null, estimateTokens(prompt), estimateTokens(answer),
                    question, retrievalResult.totalChunks(), validChunks.size(), topK(), threshold,
                    retrievalResult.maxFinalScore(), true
            );
        } catch (RuntimeException ex) {
            logService.recordAiCall(
                    userId, session.getKnowledgeBaseId(), session.getId(), modelConfig.getModelName(),
                    resolveModelType(modelConfig), modelConfig.getProvider(), "CHAT_QA",
                    System.currentTimeMillis() - start, false, ex.getMessage(), estimateTokens(prompt), null,
                    question, retrievalResult.totalChunks(), validChunks.size(), topK(), threshold,
                    retrievalResult.maxFinalScore(), true
            );
            throw ex;
        }

        List<ReferenceVO> responseReferences = enrichDocumentNames(toRankedReferences(validChunks));
        ChatMessage assistantMessage = saveMessage(
                userId,
                session.getId(),
                MessageRole.ASSISTANT,
                answer,
                modelConfig.getModelName(),
                AnswerType.RAG,
                Boolean.FALSE,
                responseReferences
        );
        responseReferences.forEach(reference -> saveReference(userId, assistantMessage.getId(), reference));

        return new AskVO(
                session.getId(),
                question,
                answer,
                AnswerType.RAG.name(),
                false,
                assistantMessage.getId(),
                responseReferences,
                true,
                true,
                null
        );
    }

    private AskVO generalAnswer(Long userId,
                                ChatSession session,
                                String question,
                                RetrievalResult retrievalResult,
                                int effectiveRetrieveCount,
                                double threshold) {
        AiModelConfig modelConfig = configService.requireEnabledLlmConfig();
        String prompt = GENERAL_ANSWER_PROMPT + "\n\n用户问题：\n" + question + "\n";
        long start = System.currentTimeMillis();
        String answer;
        try {
            answer = llmClient.complete(prompt, modelConfig);
            logService.recordAiCall(
                    userId, session.getKnowledgeBaseId(), session.getId(), modelConfig.getModelName(),
                    resolveModelType(modelConfig), modelConfig.getProvider(), "CHAT_QA_GENERAL",
                    System.currentTimeMillis() - start, true, null, estimateTokens(prompt), estimateTokens(answer),
                    question, retrievalResult.totalChunks(), effectiveRetrieveCount, topK(), threshold,
                    retrievalResult.maxFinalScore(), true
            );
        } catch (RuntimeException ex) {
            logService.recordAiCall(
                    userId, session.getKnowledgeBaseId(), session.getId(), modelConfig.getModelName(),
                    resolveModelType(modelConfig), modelConfig.getProvider(), "CHAT_QA_GENERAL",
                    System.currentTimeMillis() - start, false, ex.getMessage(), estimateTokens(prompt), null,
                    question, retrievalResult.totalChunks(), effectiveRetrieveCount, topK(), threshold,
                    retrievalResult.maxFinalScore(), true
            );
            throw BusinessException.badRequest("通用回答调用失败，请稍后重试");
        }

        ChatMessage assistantMessage = saveMessage(
                userId,
                session.getId(),
                MessageRole.ASSISTANT,
                answer,
                modelConfig.getModelName(),
                AnswerType.GENERAL,
                Boolean.FALSE,
                Collections.emptyList()
        );

        return new AskVO(
                session.getId(),
                question,
                answer,
                AnswerType.GENERAL.name(),
                false,
                assistantMessage.getId(),
                Collections.emptyList(),
                false,
                false,
                null
        );
    }

    private List<ReferenceVO> readMessageReferences(Long userId, ChatMessage message) {
        List<ChatMessageReference> refs = referenceRepository.findByUserIdAndMessageIdAndDeletedFalse(userId, message.getId());
        if (CollectionUtils.isEmpty(refs)) {
            return enrichDocumentNames(parseReferencesJson(message.getReferencesJson()));
        }
        List<Long> chunkIds = refs.stream()
                .map(ChatMessageReference::getChunkId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Integer> chunkIndexMap = CollectionUtils.isEmpty(chunkIds)
                ? Collections.emptyMap()
                : chunkRepository.selectBatchIds(chunkIds).stream()
                .collect(Collectors.toMap(DocumentChunk::getId, DocumentChunk::getChunkIndex, (a, b) -> a));
        List<ReferenceVO> references = refs.stream()
                .map(ref -> ReferenceVO.from(ref, chunkIndexMap.get(ref.getChunkId())))
                .toList();
        return enrichDocumentNames(references);
    }

    private List<ReferenceVO> parseReferencesJson(String referencesJson) {
        if (!StringUtils.hasText(referencesJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(
                    referencesJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ReferenceVO.class));
        } catch (Exception ex) {
            log.warn("Failed to parse message references json, fallback to empty list: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private String resolveHistoryAnswerType(ChatMessage message, List<ReferenceVO> references) {
        if (message.getAnswerType() != null) {
            return message.getAnswerType().name();
        }
        if (!CollectionUtils.isEmpty(references)) {
            return AnswerType.RAG.name();
        }
        if (StringUtils.hasText(message.getContent()) && message.getContent().contains("未找到相关依据")) {
            return AnswerType.NO_CONTEXT.name();
        }
        return AnswerType.RAG.name();
    }

    private List<ReferenceVO> toRankedReferences(List<ScoredChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return Collections.emptyList();
        }
        List<ScoredChunk> sorted = chunks.stream()
                .sorted(Comparator.comparingDouble(ScoredChunk::finalScore).reversed())
                .toList();
        java.util.ArrayList<ReferenceVO> references = new java.util.ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            ScoredChunk scored = sorted.get(i);
            DocumentChunk chunk = scored.chunk();
            references.add(ReferenceVO.fromRetrievedChunk(new ReferenceVO.RetrievalReference(
                    chunk.getDocumentId(),
                    scored.documentName(),
                    chunk.getId(),
                    chunk.getChunkIndex(),
                    scored.vectorId(),
                    chunk.getContent(),
                    roundScore(scored.finalScore()),
                    scored.hitReason(),
                    i + 1
            )));
        }
        return references;
    }

    private List<ReferenceVO> enrichDocumentNames(List<ReferenceVO> references) {
        if (CollectionUtils.isEmpty(references)) {
            return Collections.emptyList();
        }
        List<Long> documentIds = references.stream()
                .map(ReferenceVO::documentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(documentIds)) {
            return references;
        }
        Map<Long, String> nameMap = documentRepository.selectBatchIds(documentIds).stream()
                .collect(Collectors.toMap(Document::getId, Document::getName, (a, b) -> a));
        return references.stream()
                .map(reference -> {
                    if (StringUtils.hasText(reference.documentName()) || reference.documentId() == null) {
                        return reference;
                    }
                    String docName = nameMap.get(reference.documentId());
                    if (!StringUtils.hasText(docName)) {
                        return reference;
                    }
                    return new ReferenceVO(
                            reference.documentId(),
                            docName,
                            reference.chunkId(),
                            reference.chunkIndex(),
                            reference.vectorId(),
                            reference.content(),
                            reference.snippet(),
                            reference.score(),
                            reference.finalScore(),
                            reference.hitReason(),
                            reference.rank()
                    );
                })
                .toList();
    }

    private KnowledgeBase requireUsableKnowledgeBase(Long id) {
        KnowledgeBase kb = knowledgeBaseService.requireOwned(id);
        if (kb.getStatus() != KnowledgeBaseStatus.NORMAL) {
            throw BusinessException.badRequest("knowledge base is not available");
        }
        return kb;
    }

    private void ensureKnowledgeBaseReady(KnowledgeBase kb) {
        long totalDocs = documentRepository.countByKnowledgeBaseIdAndDeletedFalse(kb.getId());
        if (totalDocs <= 0) {
            throw BusinessException.badRequest(KB_EMPTY_ANSWER);
        }
        long processingDocs = documentRepository.countByKnowledgeBaseIdAndStatuses(
                kb.getId(), List.of(DocumentParseStatus.PENDING, DocumentParseStatus.PARSING));
        if (processingDocs > 0) {
            throw BusinessException.badRequest(DOC_PROCESSING_ANSWER);
        }
    }

    private void ensureKnowledgeBaseReadyById(Long knowledgeBaseId) {
        KnowledgeBase kb = requireUsableKnowledgeBase(knowledgeBaseId);
        ensureKnowledgeBaseReady(kb);
    }

    private String resolveNoContextAnswer(NoAnswerReason reason) {
        if (reason == NoAnswerReason.EMPTY_KNOWLEDGE_BASE) {
            return KB_EMPTY_ANSWER;
        }
        if (reason == NoAnswerReason.NO_CHUNKS) {
            return NO_RELEVANT_ANSWER;
        }
        return NO_EVIDENCE_PROMPT_ANSWER;
    }

    private ChatSession resolveSession(Long sessionId, Long knowledgeBaseId, Long userId, String question) {
        if (sessionId != null) {
            ChatSession session = requireSession(sessionId, userId);
            if (!session.getKnowledgeBaseId().equals(knowledgeBaseId)) {
                throw BusinessException.badRequest("session does not belong to this knowledge base");
            }
            return session;
        }
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setKnowledgeBaseId(knowledgeBaseId);
        String normalizedTitle = StringUtils.hasText(question) ? question.trim() : "";
        if (!StringUtils.hasText(normalizedTitle)) {
            normalizedTitle = "New Session";
        }
        session.setTitle(normalizedTitle.length() > 20 ? normalizedTitle.substring(0, 20) : normalizedTitle);
        sessionRepository.insert(session);
        return session;
    }

    private ChatSession requireSession(Long sessionId, Long userId) {
        return sessionRepository.findByIdAndUserIdAndDeletedFalse(sessionId, userId)
                .orElseThrow(() -> BusinessException.notFound("session not found"));
    }

    private List<ChatMessage> messagesForSession(Long userId, Long sessionId) {
        return messageRepository.findByUserIdAndSessionIdAndDeletedFalseOrderByCreateTimeAsc(userId, sessionId);
    }

    private ChatMessage saveMessage(Long userId,
                                    Long sessionId,
                                    MessageRole role,
                                    String content,
                                    String modelName,
                                    AnswerType answerType,
                                    Boolean canUseGeneralAnswer,
                                    List<ReferenceVO> references) {
        ChatMessage message = new ChatMessage();
        message.setUserId(userId);
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setModelName(modelName);
        message.setTokenCount(Math.max(1, content.length() / 2));
        message.setAnswerType(answerType);
        message.setCanUseGeneralAnswer(canUseGeneralAnswer);
        message.setReferencesJson(writeReferencesJson(references));
        messageRepository.insert(message);
        return message;
    }

    private String writeReferencesJson(List<ReferenceVO> references) {
        List<ReferenceVO> safeReferences = references == null ? Collections.emptyList() : references;
        try {
            return objectMapper.writeValueAsString(safeReferences);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize message references json: {}", ex.getMessage());
            return "[]";
        }
    }

    private RetrievalResult retrieveByKnowledgeBases(Long userId, List<Long> knowledgeBaseIds, String question) {
        RetrievalService.RetrievalResult result = retrievalService.retrieveByKnowledgeBases(
                knowledgeBaseIds, userId, question, topK(), "hybrid", similarityThreshold());
        return toRetrievalResult(result);
    }

    private RetrievalResult retrieveByDocument(Long userId, Long documentId, String question) {
        Document document = documentService.requireOwned(documentId);
        RetrievalService.RetrievalResult result = retrievalService.retrieveByDocument(
                document.getKnowledgeBaseId(), userId, documentId, question, topK(), "hybrid", similarityThreshold());
        return toRetrievalResult(result);
    }

    private RetrievalResult toRetrievalResult(RetrievalService.RetrievalResult result) {
        List<ScoredChunk> scoredChunks = result.chunks().stream()
                .map(item -> new ScoredChunk(
                        chunkRepository.selectById(item.chunkId()),
                        item.documentName(),
                        item.finalScore(),
                        item.vectorId(),
                        item.hitReason()))
                .filter(item -> item.chunk() != null)
                .toList();
        double maxFinalScore = result.chunks().stream().mapToDouble(RetrievalService.RetrievedChunk::finalScore).max().orElse(0d);
        return new RetrievalResult(
                scoredChunks,
                result.mergedResultsCount(),
                maxFinalScore,
                result.hasStrongKeywordHit()
        );
    }

    private void saveReference(Long userId, Long messageId, ReferenceVO reference) {
        ChatMessageReference entity = new ChatMessageReference();
        entity.setUserId(userId);
        entity.setMessageId(messageId);
        entity.setDocumentId(reference.documentId());
        entity.setChunkId(reference.chunkId());
        entity.setDocumentName(reference.documentName() == null ? "" : reference.documentName());
        entity.setContent(reference.content() == null ? "" : reference.content());
        entity.setScore(reference.finalScore());
        referenceRepository.insert(entity);
    }

    private String buildPrompt(String question, List<ScoredChunk> chunks, Long sessionId, Long userId) {
        String context = chunks.stream()
                .map(scored -> "[" + scored.chunk().getId() + "] " + scored.chunk().getContent())
                .collect(Collectors.joining("\n\n"));
        context = truncateContext(context, contextMaxLength());
        String history = messagesForSession(userId, sessionId).stream()
                .limit(8)
                .map(message -> message.getRole() + ": " + message.getContent())
                .collect(Collectors.joining("\n"));
        return configService.defaultRagPrompt()
                + "\n\n【知识库引用内容】\n" + context
                + "\n\n【用户问题】\n" + question
                + "\n\n请基于以上内容作答。"
                + "\n\nConversation history:\n" + history;
    }

    private int topK() {
        int configured = runtimeConfigService.intValue("rag.topK", defaultTopK);
        return configured > 0 ? configured : DEFAULT_TOP_K;
    }

    private double similarityThreshold() {
        double configured = runtimeConfigService.doubleValue("rag.similarityThreshold",
                runtimeConfigService.doubleValue("rag.minScore", defaultSimilarityThreshold));
        return configured >= 0 ? configured : DEFAULT_SIMILARITY_THRESHOLD;
    }

    private boolean isGeneralAnswerAllowed() {
        String configured = runtimeConfigService.value("rag.allowGeneralAnswer");
        if (!StringUtils.hasText(configured)) {
            configured = runtimeConfigService.value("chat.allowGeneralAnswer");
        }
        if (!StringUtils.hasText(configured)) {
            return true;
        }
        String normalized = configured.trim().toLowerCase();
        return !("false".equals(normalized) || "0".equals(normalized) || "off".equals(normalized) || "no".equals(normalized));
    }

    private int contextMaxLength() {
        return runtimeConfigService.intValue("rag.contextMaxLength", DEFAULT_CONTEXT_MAX_LENGTH);
    }

    private String truncateContext(String context, int maxLength) {
        if (!StringUtils.hasText(context)) {
            return "";
        }
        if (maxLength <= 0 || context.length() <= maxLength) {
            return context;
        }
        return context.substring(0, maxLength);
    }

    private String sanitizePdfLine(String line) {
        String ascii = line.replaceAll("[^\\x20-\\x7E]", "?");
        return ascii.length() > 100 ? ascii.substring(0, 100) : ascii;
    }

    private String resolveModelType(AiModelConfig config) {
        if (StringUtils.hasText(config.getModelType())) {
            return config.getModelType().toUpperCase();
        }
        String modelName = config.getModelName() == null ? "" : config.getModelName().toLowerCase();
        return modelName.contains("embedding") ? "EMBEDDING" : "LLM";
    }

    private Integer estimateTokens(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        return Math.max(1, text.length() / 4);
    }

    private Double roundScore(Double score) {
        if (score == null) {
            return null;
        }
        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private record ScoredChunk(DocumentChunk chunk,
                               String documentName,
                               double finalScore,
                               String vectorId,
                               String hitReason) {
    }

    private record RetrievalResult(List<ScoredChunk> scoredChunks,
                                   int totalChunks,
                                   double maxFinalScore,
                                   boolean hasStrongKeywordHit) {
    }
}
