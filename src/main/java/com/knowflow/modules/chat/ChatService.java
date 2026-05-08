package com.knowflow.modules.chat;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.common.BusinessException;
import com.knowflow.common.PageResponse;
import com.knowflow.common.enums.KnowledgeBaseStatus;
import com.knowflow.common.enums.MessageRole;
import com.knowflow.infrastructure.ai.EmbeddingClient;
import com.knowflow.infrastructure.ai.LlmClient;
import com.knowflow.modules.chat.dto.AskRequest;
import com.knowflow.modules.document.Document;
import com.knowflow.modules.document.DocumentChunk;
import com.knowflow.modules.document.DocumentChunkRepository;
import com.knowflow.modules.document.DocumentRepository;
import com.knowflow.modules.knowledge.KnowledgeBase;
import com.knowflow.modules.knowledge.KnowledgeBaseService;
import com.knowflow.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {
    private final KnowledgeBaseService knowledgeBaseService;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatMessageReferenceRepository referenceRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final EmbeddingClient embeddingClient;
    private final LlmClient llmClient;
    private final int topK;
    private final double minScore;

    public ChatService(KnowledgeBaseService knowledgeBaseService,
                       ChatSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       ChatMessageReferenceRepository referenceRepository,
                       DocumentChunkRepository chunkRepository,
                       DocumentRepository documentRepository,
                       EmbeddingClient embeddingClient,
                       LlmClient llmClient,
                       @Value("${knowflow.rag.top-k}") int topK,
                       @Value("${knowflow.rag.min-score}") double minScore) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.referenceRepository = referenceRepository;
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.embeddingClient = embeddingClient;
        this.llmClient = llmClient;
        this.topK = topK;
        this.minScore = minScore;
    }

    @Transactional
    public AskVO ask(AskRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        KnowledgeBase kb = knowledgeBaseService.requireOwned(request.knowledgeBaseId());
        if (kb.getStatus() != KnowledgeBaseStatus.NORMAL) {
            throw BusinessException.badRequest("知识库当前不可问答");
        }
        ChatSession session = resolveSession(request, userId);
        saveMessage(userId, session.getId(), MessageRole.USER, request.question(), null);

        List<ScoredChunk> scoredChunks = retrieve(userId, request.knowledgeBaseId(), request.question());
        String answer = scoredChunks.isEmpty()
                ? "当前知识库中没有找到足够依据。"
                : llmClient.complete(buildPrompt(request.question(), scoredChunks));

        ChatMessage assistantMessage = saveMessage(userId, session.getId(), MessageRole.ASSISTANT, answer, "mock-llm");
        List<ReferenceVO> references = scoredChunks.stream()
                .map(scored -> saveReference(userId, assistantMessage.getId(), scored))
                .map(ReferenceVO::from)
                .toList();
        return new AskVO(session.getId(), answer, references);
    }

    public PageResponse<ChatSessionVO> sessions(int pageNo, int pageSize) {
        return PageResponse.of(sessionRepository
                .findByUserIdAndDeletedFalse(SecurityUtils.getCurrentUserId(), new Page<>(pageNo, pageSize))
                .convert(ChatSessionVO::from));
    }

    public List<ChatMessageVO> messages(Long sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        requireSession(sessionId, userId);
        return messageRepository.findByUserIdAndSessionIdAndDeletedFalseOrderByCreateTimeAsc(userId, sessionId).stream()
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

    private ChatSession resolveSession(AskRequest request, Long userId) {
        if (request.sessionId() != null) {
            ChatSession session = requireSession(request.sessionId(), userId);
            if (!session.getKnowledgeBaseId().equals(request.knowledgeBaseId())) {
                throw BusinessException.badRequest("会话不属于当前知识库");
            }
            return session;
        }
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setKnowledgeBaseId(request.knowledgeBaseId());
        session.setTitle(request.question().length() > 30 ? request.question().substring(0, 30) : request.question());
        sessionRepository.insert(session);
        return session;
    }

    private ChatSession requireSession(Long sessionId, Long userId) {
        return sessionRepository.findByIdAndUserIdAndDeletedFalse(sessionId, userId)
                .orElseThrow(() -> BusinessException.notFound("会话不存在"));
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

    private List<ScoredChunk> retrieve(Long userId, Long knowledgeBaseId, String question) {
        double[] query = embeddingClient.embed(question);
        List<DocumentChunk> chunks = chunkRepository.findByUserIdAndKnowledgeBaseIdAndDeletedFalse(userId, knowledgeBaseId);
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

    private String buildPrompt(String question, List<ScoredChunk> chunks) {
        String context = chunks.stream()
                .map(scored -> "[" + scored.chunk().getId() + "] " + scored.chunk().getContent())
                .collect(Collectors.joining("\n\n"));
        return "你是 KnowFlow AI 的知识库问答助手。只能基于给定文档片段回答。\n"
                + "用户问题：" + question + "\n\n相关文档片段：\n" + context;
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
