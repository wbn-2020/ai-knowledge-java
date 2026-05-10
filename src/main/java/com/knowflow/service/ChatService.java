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
import com.knowflow.enums.KnowledgeBaseStatus;
import com.knowflow.enums.MessageRole;
import com.knowflow.enums.NoAnswerReason;
import com.knowflow.infrastructure.ai.LlmClient;
import com.knowflow.mapper.ChatFeedbackRepository;
import com.knowflow.mapper.ChatMessageReferenceRepository;
import com.knowflow.mapper.ChatMessageRepository;
import com.knowflow.mapper.ChatSessionRepository;
import com.knowflow.mapper.DocumentChunkRepository;
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
import java.util.Collections;
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
    private static final String NO_EVIDENCE_ANSWER = "当前知识库未找到足够相关资料，无法基于知识库回答。";
    private static final String GENERAL_ANSWER_PREFIX = "以下回答未基于当前知识库资料：";
    private static final double MIN_REFERENCE_SCORE = 0.70d;
    private static final int MAX_REFERENCES = 3;

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatMessageReferenceRepository referenceRepository;
    private final ChatFeedbackRepository feedbackRepository;
    private final DocumentChunkRepository chunkRepository;
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
                       LlmClient llmClient,
                       LogService logService,
                       ConfigService configService,
                       RuntimeConfigService runtimeConfigService,
                       RetrievalService retrievalService,
                       ObjectMapper objectMapper,
                       @Value("${knowflow.rag.top-k}") int topK,
                       @Value("${knowflow.rag.similarity-threshold:0.8}") double similarityThreshold) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentService = documentService;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.referenceRepository = referenceRepository;
        this.feedbackRepository = feedbackRepository;
        this.chunkRepository = chunkRepository;
        this.llmClient = llmClient;
        this.logService = logService;
        this.configService = configService;
        this.runtimeConfigService = runtimeConfigService;
        this.retrievalService = retrievalService;
        this.objectMapper = objectMapper;
        this.defaultTopK = topK;
        this.defaultSimilarityThreshold = similarityThreshold;
    }

    @Transactional
    public AskVO ask(AskRequest request) {
        KnowledgeBase kb = requireUsableKnowledgeBase(request.knowledgeBaseId());
        Long userId = SecurityUtils.getCurrentUserId();
        ChatSession session = resolveSession(request.sessionId(), kb.getId(), userId, request.question());
        RetrievalResult retrievalResult = retrieveByKnowledgeBases(userId, List.of(kb.getId()), request.question());
        boolean allowGeneralAnswer = Boolean.TRUE.equals(request.allowGeneralAnswer());
        NoAnswerReason emptyReason = kb.getDocumentCount() != null && kb.getDocumentCount() == 0
                ? NoAnswerReason.EMPTY_KNOWLEDGE_BASE
                : null;
        return answer(userId, session, request.question(), retrievalResult, allowGeneralAnswer, emptyReason);
    }

    @Transactional
    public AskVO askDocument(DocumentAskRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Document document = documentService.requireOwned(request.documentId());
        requireUsableKnowledgeBase(document.getKnowledgeBaseId());
        ChatSession session = resolveSession(request.sessionId(), document.getKnowledgeBaseId(), userId, request.question());
        return answer(userId, session, request.question(),
                retrieveByDocument(userId, request.documentId(), request.question()),
                false,
                null);
    }

    @Transactional
    public AskVO askMulti(MultiKnowledgeAskRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Long> kbIds = request.knowledgeBaseIds().stream().distinct().toList();
        if (kbIds.isEmpty()) {
            throw BusinessException.badRequest("knowledgeBaseIds cannot be empty");
        }
        List<KnowledgeBase> knowledgeBases = kbIds.stream().map(this::requireUsableKnowledgeBase).toList();
        ChatSession session = resolveSession(request.sessionId(), kbIds.get(0), userId, request.question());
        int totalDocumentCount = knowledgeBases.stream()
                .map(KnowledgeBase::getDocumentCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        boolean emptyKnowledgeBase = totalDocumentCount == 0;
        return answer(userId, session, request.question(),
                retrieveByKnowledgeBases(userId, kbIds, request.question()),
                false,
                emptyKnowledgeBase ? NoAnswerReason.EMPTY_KNOWLEDGE_BASE : null);
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
        String lastQuestion = messagesForSession(userId, sessionId).stream()
                .filter(message -> message.getRole() == MessageRole.USER)
                .reduce((a, b) -> b)
                .map(ChatMessage::getContent)
                .orElseThrow(() -> BusinessException.badRequest("no question to regenerate"));
        return answer(userId, session, lastQuestion,
                retrieveByKnowledgeBases(userId, List.of(session.getKnowledgeBaseId()), lastQuestion),
                false,
                null);
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
                         boolean allowGeneralAnswer,
                         NoAnswerReason preferredNoAnswerReason) {
        saveMessage(userId, session.getId(), MessageRole.USER, question, null, null, Boolean.FALSE, Collections.emptyList());

        double threshold = similarityThreshold();
        double evidenceThreshold = Math.max(threshold, MIN_REFERENCE_SCORE);

        List<ScoredChunk> sortedChunks = retrievalResult.scoredChunks().stream()
                .sorted((a, b) -> Double.compare(b.finalScore(), a.finalScore()))
                .toList();
        List<ScoredChunk> validChunks = sortedChunks.stream()
                .filter(chunk -> chunk.finalScore() >= MIN_REFERENCE_SCORE)
                .limit(MAX_REFERENCES)
                .toList();
        int filteredLowScoreCount = Math.max(0, sortedChunks.size() - validChunks.size());
        boolean hasChunks = retrievalResult.totalChunks() > 0;
        boolean hasReliableEvidence = !validChunks.isEmpty()
                && (retrievalResult.hasStrongKeywordHit() || retrievalResult.maxFinalScore() >= evidenceThreshold);

        log.debug("Chat retrieval summary: query='{}', knowledgeBaseId={}, retrievalTopK={}, similarityThreshold={}, validReferencesCount={}, filteredLowScoreReferencesCount={}",
                question, session.getKnowledgeBaseId(), topK(), evidenceThreshold, validChunks.size(), filteredLowScoreCount);

        if (!hasReliableEvidence) {
            NoAnswerReason noAnswerReason = !hasChunks
                    ? (preferredNoAnswerReason == null ? NoAnswerReason.NO_CHUNKS : preferredNoAnswerReason)
                    : NoAnswerReason.LOW_SIMILARITY;

            if (!allowGeneralAnswer) {
                ChatMessage assistantMessage = saveMessage(
                        userId,
                        session.getId(),
                        MessageRole.ASSISTANT,
                        NO_EVIDENCE_ANSWER,
                        null,
                        AnswerType.NO_CONTEXT,
                        Boolean.TRUE,
                        Collections.emptyList()
                );
                log.debug("Chat answerType={}, canUseGeneralAnswer={}, valid references forced empty",
                        AnswerType.NO_CONTEXT.name(), true);
                return new AskVO(
                        session.getId(),
                        question,
                        NO_EVIDENCE_ANSWER,
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
            String modelName = modelConfig.getModelName();
            String prompt = "User question: " + question + "\nPlease provide a concise and safe general answer.";
            long start = System.currentTimeMillis();
            String answer;
            try {
                answer = GENERAL_ANSWER_PREFIX + "\n" + llmClient.complete(prompt, modelConfig);
                logService.recordAiCall(
                        userId, session.getKnowledgeBaseId(), session.getId(), modelConfig.getModelName(),
                        resolveModelType(modelConfig), modelConfig.getProvider(), "QA",
                        System.currentTimeMillis() - start, true, null, estimateTokens(prompt), estimateTokens(answer)
                );
            } catch (RuntimeException ex) {
                logService.recordAiCall(
                        userId, session.getKnowledgeBaseId(), session.getId(), modelConfig.getModelName(),
                        resolveModelType(modelConfig), modelConfig.getProvider(), "QA",
                        System.currentTimeMillis() - start, false, ex.getMessage(), estimateTokens(prompt), null
                );
                throw ex;
            }
            ChatMessage assistantMessage = saveMessage(
                    userId,
                    session.getId(),
                    MessageRole.ASSISTANT,
                    answer,
                    modelName,
                    AnswerType.GENERAL,
                    Boolean.FALSE,
                    Collections.emptyList()
            );
            log.debug("Chat answerType={}, canUseGeneralAnswer={}, valid references forced empty",
                    AnswerType.GENERAL.name(), false);
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
                    noAnswerReason.name()
            );
        }

        AiModelConfig modelConfig = configService.requireEnabledLlmConfig();
        boolean hasApiKey = StringUtils.hasText(modelConfig.getApiKey());
        log.debug("Chat selectedModelId={}, provider={}, modelType={}, modelName={}, baseUrl={}, hasApiKey={}",
                modelConfig.getId(), modelConfig.getProvider(), resolveModelType(modelConfig), modelConfig.getModelName(),
                modelConfig.getBaseUrl(), hasApiKey);

        String prompt = buildPrompt(question, validChunks, session.getId(), userId);
        long start = System.currentTimeMillis();
        String answer;
        try {
            answer = llmClient.complete(prompt, modelConfig);
            logService.recordAiCall(
                    userId, session.getKnowledgeBaseId(), session.getId(), modelConfig.getModelName(),
                    resolveModelType(modelConfig), modelConfig.getProvider(), "QA",
                    System.currentTimeMillis() - start, true, null, estimateTokens(prompt), estimateTokens(answer)
            );
        } catch (RuntimeException ex) {
            logService.recordAiCall(
                    userId, session.getKnowledgeBaseId(), session.getId(), modelConfig.getModelName(),
                    resolveModelType(modelConfig), modelConfig.getProvider(), "QA",
                    System.currentTimeMillis() - start, false, ex.getMessage(), estimateTokens(prompt), null
            );
            throw ex;
        }

        List<ReferenceVO> responseReferences = validChunks.stream()
                .map(this::toRetrievedReference)
                .toList();
        responseReferences.forEach(reference -> log.debug(
                "Chat valid reference: chunkIndex={}, score={}, hitReason={}, snippet={}",
                reference.chunkIndex(), reference.finalScore(), reference.hitReason(), brief(reference.content(), 100)));

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
        validChunks.forEach(scored -> saveReference(userId, assistantMessage.getId(), scored));

        log.debug("Chat answerType={}, canUseGeneralAnswer={}, retrievalTopK={}, validReferencesCount={}, filteredLowScoreReferencesCount={}",
                AnswerType.RAG.name(), false, topK(), responseReferences.size(), filteredLowScoreCount);
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

    private List<ReferenceVO> readMessageReferences(Long userId, ChatMessage message) {
        List<ChatMessageReference> refs = referenceRepository.findByUserIdAndMessageIdAndDeletedFalse(userId, message.getId());
        if (CollectionUtils.isEmpty(refs)) {
            return parseReferencesJson(message.getReferencesJson());
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
        return refs.stream()
                .map(ref -> ReferenceVO.from(ref, chunkIndexMap.get(ref.getChunkId())))
                .toList();
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
        if (StringUtils.hasText(message.getContent()) && message.getContent().startsWith(GENERAL_ANSWER_PREFIX)) {
            return AnswerType.GENERAL.name();
        }
        if (StringUtils.hasText(message.getContent()) && message.getContent().contains("未找到足够相关资料")) {
            return AnswerType.NO_CONTEXT.name();
        }
        return AnswerType.RAG.name();
    }

    private ReferenceVO toRetrievedReference(ScoredChunk scored) {
        DocumentChunk chunk = scored.chunk();
        return ReferenceVO.fromRetrievedChunk(new ReferenceVO.RetrievalReference(
                chunk.getDocumentId(),
                scored.documentName(),
                chunk.getId(),
                chunk.getChunkIndex(),
                scored.vectorId(),
                chunk.getContent(),
                roundScore(scored.finalScore()),
                scored.hitReason()
        ));
    }

    private KnowledgeBase requireUsableKnowledgeBase(Long id) {
        KnowledgeBase kb = knowledgeBaseService.requireOwned(id);
        if (kb.getStatus() != KnowledgeBaseStatus.NORMAL) {
            throw BusinessException.badRequest("knowledge base is not available");
        }
        return kb;
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
        List<ScoredChunk> scoredChunks = result.chunks().stream()
                .map(item -> new ScoredChunk(
                        chunkRepository.selectById(item.chunkId()),
                        item.documentName(),
                        item.finalScore(),
                        item.vectorId(),
                        item.hitReason()))
                .filter(item -> item.chunk() != null)
                .toList();
        Map<Long, Integer> chunkIndexMap = result.chunks().stream()
                .filter(item -> item.chunkId() != null && item.chunkIndex() != null)
                .collect(Collectors.toMap(RetrievalService.RetrievedChunk::chunkId, RetrievalService.RetrievedChunk::chunkIndex, (a, b) -> a));
        double maxFinalScore = result.chunks().stream()
                .mapToDouble(RetrievalService.RetrievedChunk::finalScore)
                .max()
                .orElse(0d);
        return new RetrievalResult(
                scoredChunks,
                chunkIndexMap,
                result.mergedResultsCount(),
                result.keywordResultsCount(),
                result.vectorResultsCount(),
                result.mergedResultsCount(),
                maxFinalScore,
                result.hasStrongKeywordHit()
        );
    }

    private RetrievalResult retrieveByDocument(Long userId, Long documentId, String question) {
        Document document = documentService.requireOwned(documentId);
        RetrievalService.RetrievalResult result = retrievalService.retrieveByDocument(
                document.getKnowledgeBaseId(), userId, documentId, question, topK(), "hybrid", similarityThreshold());
        List<ScoredChunk> scoredChunks = result.chunks().stream()
                .map(item -> new ScoredChunk(
                        chunkRepository.selectById(item.chunkId()),
                        item.documentName(),
                        item.finalScore(),
                        item.vectorId(),
                        item.hitReason()))
                .filter(item -> item.chunk() != null)
                .toList();
        Map<Long, Integer> chunkIndexMap = result.chunks().stream()
                .filter(item -> item.chunkId() != null && item.chunkIndex() != null)
                .collect(Collectors.toMap(RetrievalService.RetrievedChunk::chunkId, RetrievalService.RetrievedChunk::chunkIndex, (a, b) -> a));
        double maxFinalScore = result.chunks().stream()
                .mapToDouble(RetrievalService.RetrievedChunk::finalScore)
                .max()
                .orElse(0d);
        return new RetrievalResult(
                scoredChunks,
                chunkIndexMap,
                result.mergedResultsCount(),
                result.keywordResultsCount(),
                result.vectorResultsCount(),
                result.mergedResultsCount(),
                maxFinalScore,
                result.hasStrongKeywordHit()
        );
    }

    private ChatMessageReference saveReference(Long userId, Long messageId, ScoredChunk scored) {
        ChatMessageReference reference = new ChatMessageReference();
        reference.setUserId(userId);
        reference.setMessageId(messageId);
        reference.setDocumentId(scored.chunk().getDocumentId());
        reference.setChunkId(scored.chunk().getId());
        reference.setDocumentName(scored.documentName() == null ? "" : scored.documentName());
        reference.setContent(scored.chunk().getContent());
        reference.setScore(roundScore(scored.finalScore()));
        referenceRepository.insert(reference);
        return reference;
    }

    private String buildPrompt(String question, List<ScoredChunk> chunks, Long sessionId, Long userId) {
        String context = chunks.stream()
                .map(scored -> "[" + scored.chunk().getId() + "] " + scored.chunk().getContent())
                .collect(Collectors.joining("\n\n"));
        String history = messagesForSession(userId, sessionId).stream()
                .limit(8)
                .map(message -> message.getRole() + ": " + message.getContent())
                .collect(Collectors.joining("\n"));
        return configService.defaultRagPrompt()
                + "\nConversation history:\n" + history
                + "\n\nQuestion: " + question
                + "\n\nDocument chunks:\n" + context;
    }

    private int topK() {
        return runtimeConfigService.intValue("rag.topK", defaultTopK);
    }

    private double similarityThreshold() {
        return runtimeConfigService.doubleValue("rag.similarityThreshold",
                runtimeConfigService.doubleValue("rag.minScore", defaultSimilarityThreshold));
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

    private String brief(String content, int maxLen) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLen ? normalized : normalized.substring(0, maxLen);
    }

    private record ScoredChunk(DocumentChunk chunk,
                               String documentName,
                               double finalScore,
                               String vectorId,
                               String hitReason) {
    }

    private record RetrievalResult(List<ScoredChunk> scoredChunks,
                                   Map<Long, Integer> chunkIndexMap,
                                   int totalChunks,
                                   int keywordResultsCount,
                                   int vectorResultsCount,
                                   int mergedResultsCount,
                                   double maxFinalScore,
                                   boolean hasStrongKeywordHit) {
    }
}
