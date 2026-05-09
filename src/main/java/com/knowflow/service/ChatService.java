package com.knowflow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.common.BusinessException;
import com.knowflow.common.PageResponse;
import com.knowflow.dto.AskRequest;
import com.knowflow.dto.DocumentAskRequest;
import com.knowflow.dto.FeedbackRequest;
import com.knowflow.dto.MultiKnowledgeAskRequest;
import com.knowflow.entity.ChatFeedback;
import com.knowflow.entity.ChatMessage;
import com.knowflow.entity.ChatMessageReference;
import com.knowflow.entity.ChatSession;
import com.knowflow.entity.AiModelConfig;
import com.knowflow.entity.Document;
import com.knowflow.entity.DocumentChunk;
import com.knowflow.entity.KnowledgeBase;
import com.knowflow.enums.KnowledgeBaseStatus;
import com.knowflow.enums.MessageRole;
import com.knowflow.infrastructure.ai.EmbeddingClient;
import com.knowflow.infrastructure.ai.LlmClient;
import com.knowflow.mapper.ChatFeedbackRepository;
import com.knowflow.mapper.ChatMessageReferenceRepository;
import com.knowflow.mapper.ChatMessageRepository;
import com.knowflow.mapper.ChatSessionRepository;
import com.knowflow.mapper.DocumentChunkRepository;
import com.knowflow.mapper.DocumentRepository;
import com.knowflow.security.SecurityUtils;
import com.knowflow.service.ConfigService;
import com.knowflow.service.DocumentService;
import com.knowflow.service.KnowledgeBaseService;
import com.knowflow.service.LogService;
import com.knowflow.service.RuntimeConfigService;
import com.knowflow.vo.AskVO;
import com.knowflow.vo.ChatMessageVO;
import com.knowflow.vo.ChatSessionVO;
import com.knowflow.vo.ReferenceVO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;



@Service
public class ChatService {
    private static final String NO_EVIDENCE = "The current knowledge base has no sufficient evidence.";
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatMessageReferenceRepository referenceRepository;
    private final ChatFeedbackRepository feedbackRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final EmbeddingClient embeddingClient;
    private final LlmClient llmClient;
    private final LogService logService;
    private final ConfigService configService;
    private final RuntimeConfigService runtimeConfigService;
    private final int defaultTopK;
    private final double defaultMinScore;

    public ChatService(KnowledgeBaseService knowledgeBaseService,
                       DocumentService documentService,
                       ChatSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       ChatMessageReferenceRepository referenceRepository,
                       ChatFeedbackRepository feedbackRepository,
                       DocumentChunkRepository chunkRepository,
                       DocumentRepository documentRepository,
                       EmbeddingClient embeddingClient,
                       LlmClient llmClient,
                       LogService logService,
                       ConfigService configService,
                       RuntimeConfigService runtimeConfigService,
                       @Value("${knowflow.rag.top-k}") int topK,
                       @Value("${knowflow.rag.min-score}") double minScore) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentService = documentService;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.referenceRepository = referenceRepository;
        this.feedbackRepository = feedbackRepository;
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.embeddingClient = embeddingClient;
        this.llmClient = llmClient;
        this.logService = logService;
        this.configService = configService;
        this.runtimeConfigService = runtimeConfigService;
        this.defaultTopK = topK;
        this.defaultMinScore = minScore;
    }

    @Transactional
    public AskVO ask(AskRequest request) {
        KnowledgeBase kb = requireUsableKnowledgeBase(request.knowledgeBaseId());
        Long userId = SecurityUtils.getCurrentUserId();
        ChatSession session = resolveSession(request.sessionId(), kb.getId(), userId, request.question());
        return answer(userId, session, request.question(), retrieveByKnowledgeBases(userId, List.of(kb.getId()), request.question()));
    }

    @Transactional
    public AskVO askDocument(DocumentAskRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Document document = documentService.requireOwned(request.documentId());
        requireUsableKnowledgeBase(document.getKnowledgeBaseId());
        ChatSession session = resolveSession(request.sessionId(), document.getKnowledgeBaseId(), userId, request.question());
        return answer(userId, session, request.question(), retrieveByDocument(userId, request.documentId(), request.question()));
    }

    @Transactional
    public AskVO askMulti(MultiKnowledgeAskRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Long> kbIds = request.knowledgeBaseIds().stream().distinct().toList();
        if (kbIds.isEmpty()) {
            throw BusinessException.badRequest("knowledgeBaseIds cannot be empty");
        }
        kbIds.forEach(this::requireUsableKnowledgeBase);
        ChatSession session = resolveSession(request.sessionId(), kbIds.get(0), userId, request.question());
        return answer(userId, session, request.question(), retrieveByKnowledgeBases(userId, kbIds, request.question()));
    }

    public PageResponse<ChatSessionVO> sessions(int pageNo, int pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        Page<ChatSession> page = sessionRepository.findByUserIdAndDeletedFalse(userId, new Page<>(pageNo, pageSize));
        Map<Long, String> kbNames = knowledgeBaseService.page("", 1, 1000, "updateTime").list().stream()
                .collect(Collectors.toMap(com.knowflow.vo.KnowledgeBaseVO::id, com.knowflow.vo.KnowledgeBaseVO::name));
        return PageResponse.of(page.convert(session -> {
            List<ChatMessage> sessionMessages = messagesForSession(userId, session.getId());
            String latestQuestion = sessionMessages.stream()
                    .filter(m -> m.getRole() == MessageRole.USER)
                    .reduce((a, b) -> b)
                    .map(ChatMessage::getContent)
                    .orElse("");
            return new ChatSessionVO(session.getId(), session.getKnowledgeBaseId(),
                    kbNames.getOrDefault(session.getKnowledgeBaseId(), ""),
                    session.getTitle(), latestQuestion, (long) sessionMessages.size(),
                    session.getCreateTime(), session.getUpdateTime(), session.getUpdateTime());
        }));
    }

    public List<ChatMessageVO> messages(Long sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        requireSession(sessionId, userId);
        return messagesForSession(userId, sessionId).stream()
                .map(message -> new ChatMessageVO(
                        message.getId(),
                        message.getRole(),
                        message.getContent(),
                        message.getModelName(),
                        message.getCreateTime(),
                        referenceRepository.findByUserIdAndMessageIdAndDeletedFalse(userId, message.getId()).stream().map(ReferenceVO::from).toList()
                ))
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
        return answer(userId, session, lastQuestion, retrieveByKnowledgeBases(userId, List.of(session.getKnowledgeBaseId()), lastQuestion));
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

    private AskVO answer(Long userId, ChatSession session, String question, List<ScoredChunk> scoredChunks) {
        saveMessage(userId, session.getId(), MessageRole.USER, question, null);
        String answer;
        AiModelConfig modelConfig = configService.requireEnabledLlmConfig();
        boolean hasApiKey = StringUtils.hasText(modelConfig.getApiKey());
        String modelType = StringUtils.hasText(modelConfig.getModelType())
                ? modelConfig.getModelType().toUpperCase()
                : (modelConfig.getModelName() != null && modelConfig.getModelName().toLowerCase().contains("embedding") ? "EMBEDDING" : "LLM");
        log.debug("Chat selectedModelId={}, provider={}, modelType={}, modelName={}, baseUrl={}, hasApiKey={}",
                modelConfig.getId(), modelConfig.getProvider(), modelType, modelConfig.getModelName(), modelConfig.getBaseUrl(), hasApiKey);
        if (scoredChunks.isEmpty()) {
            answer = NO_EVIDENCE;
        } else {
            long start = System.currentTimeMillis();
            try {
                answer = llmClient.complete(buildPrompt(question, scoredChunks, session.getId(), userId), modelConfig);
                logService.recordAiCall(userId, modelConfig.getModelName(), "CHAT", System.currentTimeMillis() - start, true, null);
            } catch (RuntimeException ex) {
                logService.recordAiCall(userId, modelConfig.getModelName(), "CHAT", System.currentTimeMillis() - start, false, ex.getMessage());
                throw ex;
            }
        }
        ChatMessage assistantMessage = saveMessage(userId, session.getId(), MessageRole.ASSISTANT, answer, modelConfig.getModelName());
        List<ReferenceVO> references = scoredChunks.stream()
                .map(scored -> saveReference(userId, assistantMessage.getId(), scored))
                .map(ReferenceVO::from)
                .toList();
        return new AskVO(session.getId(), question, answer, assistantMessage.getId(), references);
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
            normalizedTitle = "新会话";
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

    private ChatMessage saveMessage(Long userId, Long sessionId, MessageRole role, String content, String modelName) {
        ChatMessage message = new ChatMessage();
        message.setUserId(userId);
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setModelName(modelName);
        message.setTokenCount(Math.max(1, content.length() / 2));
        messageRepository.insert(message);
        return message;
    }

    private List<ScoredChunk> retrieveByKnowledgeBases(Long userId, List<Long> knowledgeBaseIds, String question) {
        double[] query = embeddingClient.embed(question);
        Set<Long> allowed = Set.copyOf(knowledgeBaseIds);
        List<DocumentChunk> chunks = knowledgeBaseIds.stream()
                .flatMap(kbId -> chunkRepository.findByUserIdAndKnowledgeBaseIdAndDeletedFalse(userId, kbId).stream())
                .filter(chunk -> allowed.contains(chunk.getKnowledgeBaseId()))
                .toList();
        return scoreChunks(query, chunks);
    }

    private List<ScoredChunk> retrieveByDocument(Long userId, Long documentId, String question) {
        double[] query = embeddingClient.embed(question);
        List<DocumentChunk> chunks = chunkRepository.findByDocumentIdAndDeletedFalseOrderByChunkIndexAsc(documentId).stream()
                .filter(chunk -> userId.equals(chunk.getUserId()))
                .toList();
        return scoreChunks(query, chunks);
    }

    private List<ScoredChunk> scoreChunks(double[] query, List<DocumentChunk> chunks) {
        if (chunks.isEmpty()) {
            return List.of();
        }
        Map<Long, Document> documents = documentRepository.selectBatchIds(
                chunks.stream().map(DocumentChunk::getDocumentId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Document::getId, d -> d));
        return chunks.stream()
                .map(chunk -> new ScoredChunk(chunk, documents.get(chunk.getDocumentId()), cosine(query, parseVector(chunk.getEmbedding()))))
                .filter(scored -> scored.score() >= minScore())
                .sorted(Comparator.comparing(ScoredChunk::score).reversed())
                .limit(topK())
                .toList();
    }

    private ChatMessageReference saveReference(Long userId, Long messageId, ScoredChunk scored) {
        ChatMessageReference reference = new ChatMessageReference();
        reference.setUserId(userId);
        reference.setMessageId(messageId);
        reference.setDocumentId(scored.chunk().getDocumentId());
        reference.setChunkId(scored.chunk().getId());
        reference.setDocumentName(scored.document() == null ? "" : scored.document().getName());
        reference.setContent(scored.chunk().getContent());
        reference.setScore(scored.score());
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

    private double[] parseVector(String text) {
        if (text == null || text.isBlank()) {
            return new double[0];
        }
        String[] parts = text.split(",");
        double[] vector = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Double.parseDouble(parts[i]);
        }
        return vector;
    }

    private double cosine(double[] a, double[] b) {
        if (a.length == 0 || b.length == 0 || a.length != b.length) {
            return 0;
        }
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return na == 0 || nb == 0 ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private int topK() {
        return runtimeConfigService.intValue("rag.topK", defaultTopK);
    }

    private double minScore() {
        return runtimeConfigService.doubleValue("rag.minScore", defaultMinScore);
    }

    private String sanitizePdfLine(String line) {
        String ascii = line.replaceAll("[^\\x20-\\x7E]", "?");
        return ascii.length() > 100 ? ascii.substring(0, 100) : ascii;
    }

    private record ScoredChunk(DocumentChunk chunk, Document document, double score) {
    }
}
