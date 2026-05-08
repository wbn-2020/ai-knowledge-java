package com.knowflow.modules.chat;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.common.BusinessException;
import com.knowflow.common.PageResponse;
import com.knowflow.common.enums.KnowledgeBaseStatus;
import com.knowflow.common.enums.MessageRole;
import com.knowflow.infrastructure.ai.EmbeddingClient;
import com.knowflow.infrastructure.ai.LlmClient;
import com.knowflow.modules.chat.dto.AskRequest;
import com.knowflow.modules.chat.dto.DocumentAskRequest;
import com.knowflow.modules.chat.dto.FeedbackRequest;
import com.knowflow.modules.chat.dto.MultiKnowledgeAskRequest;
import com.knowflow.modules.config.ConfigService;
import com.knowflow.modules.document.Document;
import com.knowflow.modules.document.DocumentChunk;
import com.knowflow.modules.document.DocumentChunkRepository;
import com.knowflow.modules.document.DocumentRepository;
import com.knowflow.modules.document.DocumentService;
import com.knowflow.modules.knowledge.KnowledgeBase;
import com.knowflow.modules.knowledge.KnowledgeBaseService;
import com.knowflow.modules.log.LogService;
import com.knowflow.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatService {
    private static final String NO_EVIDENCE = "The current knowledge base has no sufficient evidence.";

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
    private final int topK;
    private final double minScore;

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
        this.topK = topK;
        this.minScore = minScore;
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
        return PageResponse.of(sessionRepository
                .findByUserIdAndDeletedFalse(SecurityUtils.getCurrentUserId(), new Page<>(pageNo, pageSize))
                .convert(ChatSessionVO::from));
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

    public String exportText(Long sessionId, String format) {
        String title = "KnowFlow chat export (" + format.toUpperCase() + " placeholder)\n\n";
        return title + exportMarkdown(sessionId);
    }

    private AskVO answer(Long userId, ChatSession session, String question, List<ScoredChunk> scoredChunks) {
        saveMessage(userId, session.getId(), MessageRole.USER, question, null);
        String answer;
        if (scoredChunks.isEmpty()) {
            answer = NO_EVIDENCE;
        } else {
            long start = System.currentTimeMillis();
            try {
                answer = llmClient.complete(buildPrompt(question, scoredChunks, session.getId(), userId));
                logService.recordAiCall(userId, "deepseek", "CHAT", System.currentTimeMillis() - start, true, null);
            } catch (RuntimeException ex) {
                logService.recordAiCall(userId, "deepseek", "CHAT", System.currentTimeMillis() - start, false, ex.getMessage());
                throw ex;
            }
        }
        ChatMessage assistantMessage = saveMessage(userId, session.getId(), MessageRole.ASSISTANT, answer, "deepseek");
        List<ReferenceVO> references = scoredChunks.stream()
                .map(scored -> saveReference(userId, assistantMessage.getId(), scored))
                .map(ReferenceVO::from)
                .toList();
        return new AskVO(session.getId(), answer, references);
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
        session.setTitle(question.length() > 30 ? question.substring(0, 30) : question);
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
                .filter(scored -> scored.score() >= minScore)
                .sorted(Comparator.comparing(ScoredChunk::score).reversed())
                .limit(topK)
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

    private record ScoredChunk(DocumentChunk chunk, Document document, double score) {
    }
}
