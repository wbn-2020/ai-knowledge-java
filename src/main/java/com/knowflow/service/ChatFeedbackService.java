package com.knowflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.common.BusinessException;
import com.knowflow.common.PageResponse;
import com.knowflow.dto.ChatFeedbackRequest;
import com.knowflow.entity.ChatMessage;
import com.knowflow.entity.ChatMessageFeedback;
import com.knowflow.entity.ChatMessageReference;
import com.knowflow.entity.ChatSession;
import com.knowflow.entity.Document;
import com.knowflow.entity.KnowledgeBase;
import com.knowflow.entity.User;
import com.knowflow.enums.MessageRole;
import com.knowflow.mapper.ChatMessageFeedbackRepository;
import com.knowflow.mapper.ChatMessageReferenceRepository;
import com.knowflow.mapper.ChatMessageRepository;
import com.knowflow.mapper.ChatSessionRepository;
import com.knowflow.mapper.DocumentRepository;
import com.knowflow.mapper.KnowledgeBaseRepository;
import com.knowflow.mapper.UserRepository;
import com.knowflow.security.SecurityUtils;
import com.knowflow.vo.AdminFeedbackVO;
import com.knowflow.vo.ChatFeedbackVO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ChatFeedbackService {
    private static final Set<String> FEEDBACK_TYPES = Set.of("LIKE", "DISLIKE");
    private static final Set<String> FEEDBACK_REASONS = Set.of(
            "NOT_RELEVANT", "INCORRECT", "INCOMPLETE", "HALLUCINATION", "BAD_REFERENCE", "OTHER"
    );

    private final ChatMessageFeedbackRepository feedbackRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageReferenceRepository referenceRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final OperationLogService operationLogService;

    public ChatFeedbackService(ChatMessageFeedbackRepository feedbackRepository,
                               ChatMessageRepository messageRepository,
                               ChatSessionRepository sessionRepository,
                               ChatMessageReferenceRepository referenceRepository,
                               KnowledgeBaseRepository knowledgeBaseRepository,
                               DocumentRepository documentRepository,
                               UserRepository userRepository,
                               OperationLogService operationLogService) {
        this.feedbackRepository = feedbackRepository;
        this.messageRepository = messageRepository;
        this.sessionRepository = sessionRepository;
        this.referenceRepository = referenceRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public ChatFeedbackVO submit(Long messageId, ChatFeedbackRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ChatMessage message = validateFeedbackMessage(messageId, userId);
        String feedbackType = normalizeFeedbackType(request.feedbackType());
        String reason = normalizeReason(request.reason(), feedbackType);
        String remark = normalizeRemark(request.remark());

        ChatMessageFeedback feedback = feedbackRepository.findByUserIdAndMessageId(userId, messageId)
                .orElseGet(ChatMessageFeedback::new);
        if (feedback.getId() == null) {
            feedback.setUserId(userId);
            feedback.setMessageId(messageId);
            feedback.setSessionId(message.getSessionId());
            feedback.setFeedbackType(feedbackType);
            feedback.setReason(reason);
            feedback.setRemark(remark);
            feedbackRepository.insert(feedback);
        } else {
            feedback.setFeedbackType(feedbackType);
            feedback.setReason(reason);
            feedback.setRemark(remark);
            feedbackRepository.updateById(feedback);
        }
        operationLogService.record("CHAT_FEEDBACK", "CHAT_MESSAGE", messageId,
                "feedbackType=" + feedbackType + ", reason=" + (reason == null ? "" : reason));
        return ChatFeedbackVO.from(feedback);
    }

    public ChatFeedbackVO getMyFeedback(Long messageId) {
        Long userId = SecurityUtils.getCurrentUserId();
        validateFeedbackMessage(messageId, userId);
        return feedbackRepository.findByUserIdAndMessageId(userId, messageId)
                .map(ChatFeedbackVO::from)
                .orElse(null);
    }

    @Transactional
    public void deleteMyFeedback(Long messageId) {
        Long userId = SecurityUtils.getCurrentUserId();
        validateFeedbackMessage(messageId, userId);
        feedbackRepository.deleteByUserIdAndMessageId(userId, messageId);
        operationLogService.record("DELETE_CHAT_FEEDBACK", "CHAT_MESSAGE", messageId, "delete feedback");
    }

    public PageResponse<AdminFeedbackVO> adminPage(String feedbackType,
                                                   String reason,
                                                   String username,
                                                   String keyword,
                                                   int pageNo,
                                                   int pageSize) {
        SecurityUtils.requireAdmin();
        List<Long> filteredUserIds = null;
        if (StringUtils.hasText(username)) {
            List<Long> ids = userRepository.selectList(new LambdaQueryWrapper<User>()
                            .like(User::getUsername, username))
                    .stream().map(User::getId).toList();
            if (ids.isEmpty()) {
                return new PageResponse<>(List.of(), 0, pageNo, pageSize);
            }
            filteredUserIds = ids;
        }

        List<Long> messageIdsByKeyword = null;
        if (StringUtils.hasText(keyword)) {
            messageIdsByKeyword = messageRepository.selectList(new LambdaQueryWrapper<ChatMessage>()
                            .like(ChatMessage::getContent, keyword))
                    .stream().map(ChatMessage::getId).toList();
            if (messageIdsByKeyword.isEmpty()) {
                messageIdsByKeyword = List.of(-1L);
            }
        }

        Page<ChatMessageFeedback> page = feedbackRepository.pageForAdmin(
                normalizeNullable(feedbackType), normalizeNullable(reason), filteredUserIds, messageIdsByKeyword, normalizeNullable(keyword), pageNo, pageSize);

        List<ChatMessageFeedback> records = page.getRecords();
        if (records.isEmpty()) {
            return new PageResponse<>(List.of(), page.getTotal(), pageNo, pageSize);
        }

        Set<Long> messageIds = new HashSet<>();
        Set<Long> sessionIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        for (ChatMessageFeedback feedback : records) {
            messageIds.add(feedback.getMessageId());
            sessionIds.add(feedback.getSessionId());
            userIds.add(feedback.getUserId());
        }

        Map<Long, ChatMessage> messageMap = toMapById(messageRepository.selectBatchIds(messageIds));
        Map<Long, ChatSession> sessionMap = toMapById(sessionRepository.selectBatchIds(sessionIds));
        Map<Long, User> userMap = toMapById(userRepository.selectBatchIds(userIds));

        Map<Long, List<ChatMessageReference>> refMap = new HashMap<>();
        for (ChatMessageReference ref : referenceRepository.findByMessageIds(new ArrayList<>(messageIds))) {
            refMap.computeIfAbsent(ref.getMessageId(), k -> new ArrayList<>()).add(ref);
        }

        Set<Long> kbIds = new HashSet<>();
        Set<Long> docIds = new HashSet<>();
        for (ChatSession session : sessionMap.values()) {
            if (session != null && session.getKnowledgeBaseId() != null) {
                kbIds.add(session.getKnowledgeBaseId());
            }
            if (session != null && session.getDocumentId() != null) {
                docIds.add(session.getDocumentId());
            }
        }
        for (List<ChatMessageReference> refs : refMap.values()) {
            if (refs.isEmpty()) {
                continue;
            }
            ChatMessageReference first = refs.get(0);
            if (first.getDocumentId() != null) {
                docIds.add(first.getDocumentId());
            }
        }

        Map<Long, KnowledgeBase> kbMap = toMapById(knowledgeBaseRepository.selectBatchIds(kbIds));
        Map<Long, Document> docMap = toMapById(documentRepository.selectBatchIds(docIds));

        List<AdminFeedbackVO> list = records.stream().map(item -> {
            ChatMessage answerMsg = messageMap.get(item.getMessageId());
            ChatSession session = sessionMap.get(item.getSessionId());
            User user = userMap.get(item.getUserId());
            String question = resolveQuestion(answerMsg, item.getUserId());
            Long knowledgeBaseId = session == null ? null : session.getKnowledgeBaseId();
            String knowledgeBaseName = knowledgeBaseId == null ? null :
                    (kbMap.get(knowledgeBaseId) == null ? null : kbMap.get(knowledgeBaseId).getName());
            Long documentId = session == null ? null : session.getDocumentId();
            String documentName = null;
            if (documentId != null && docMap.get(documentId) != null) {
                documentName = docMap.get(documentId).getName();
            } else {
                List<ChatMessageReference> refs = refMap.getOrDefault(item.getMessageId(), List.of());
                if (!refs.isEmpty()) {
                    ChatMessageReference first = refs.get(0);
                    documentId = first.getDocumentId();
                    if (documentId != null && docMap.get(documentId) != null) {
                        documentName = docMap.get(documentId).getName();
                    }
                }
            }

            return new AdminFeedbackVO(
                    item.getId(),
                    item.getMessageId(),
                    item.getSessionId(),
                    item.getUserId(),
                    user == null ? null : user.getUsername(),
                    item.getFeedbackType(),
                    item.getReason(),
                    item.getRemark(),
                    question,
                    answerMsg == null ? null : answerMsg.getContent(),
                    answerMsg == null || answerMsg.getAnswerType() == null ? null : answerMsg.getAnswerType().name(),
                    knowledgeBaseId,
                    knowledgeBaseName,
                    documentId,
                    documentName,
                    item.getCreateTime(),
                    item.getUpdateTime()
            );
        }).toList();

        return new PageResponse<>(list, page.getTotal(), pageNo, pageSize);
    }

    private ChatMessage validateFeedbackMessage(Long messageId, Long userId) {
        ChatMessage message = messageRepository.selectById(messageId);
        if (message == null || !userId.equals(message.getUserId())) {
            throw BusinessException.notFound("message not found");
        }
        if (message.getRole() != MessageRole.ASSISTANT) {
            throw BusinessException.badRequest("only assistant answer can be feedback");
        }
        sessionRepository.findByIdAndUserIdAndDeletedFalse(message.getSessionId(), userId)
                .orElseThrow(() -> BusinessException.forbidden("no permission to access this message"));
        return message;
    }

    private String normalizeFeedbackType(String feedbackType) {
        String normalized = feedbackType == null ? "" : feedbackType.trim().toUpperCase(Locale.ROOT);
        if (!FEEDBACK_TYPES.contains(normalized)) {
            throw BusinessException.badRequest("invalid feedbackType, only LIKE or DISLIKE");
        }
        return normalized;
    }

    private String normalizeReason(String reason, String feedbackType) {
        if (!StringUtils.hasText(reason)) {
            if ("DISLIKE".equals(feedbackType)) {
                throw BusinessException.badRequest("reason is required when feedbackType=DISLIKE");
            }
            return null;
        }
        String normalized = reason.trim().toUpperCase(Locale.ROOT);
        if (!FEEDBACK_REASONS.contains(normalized)) {
            throw BusinessException.badRequest("invalid reason");
        }
        return normalized;
    }

    private String normalizeRemark(String remark) {
        if (!StringUtils.hasText(remark)) {
            return null;
        }
        String normalized = remark.trim();
        if (normalized.length() > 500) {
            throw BusinessException.badRequest("remark too long, max 500 chars");
        }
        return normalized;
    }

    private String normalizeNullable(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private String resolveQuestion(ChatMessage answerMessage, Long userId) {
        if (answerMessage == null) {
            return null;
        }
        List<ChatMessage> messages = messageRepository.findByUserIdAndSessionIdAndDeletedFalseOrderByCreateTimeAsc(userId, answerMessage.getSessionId());
        ChatMessage previousUser = null;
        for (ChatMessage message : messages) {
            if (message.getId().equals(answerMessage.getId())) {
                break;
            }
            if (message.getRole() == MessageRole.USER) {
                previousUser = message;
            }
        }
        return previousUser == null ? null : previousUser.getContent();
    }

    private <T> Map<Long, T> toMapById(List<T> list) {
        Map<Long, T> map = new HashMap<>();
        for (T item : list) {
            if (item == null) {
                continue;
            }
            Long id = extractId(item);
            if (id != null) {
                map.put(id, item);
            }
        }
        return map;
    }

    private Long extractId(Object item) {
        if (item instanceof ChatMessage message) {
            return message.getId();
        }
        if (item instanceof ChatSession session) {
            return session.getId();
        }
        if (item instanceof User user) {
            return user.getId();
        }
        if (item instanceof KnowledgeBase kb) {
            return kb.getId();
        }
        if (item instanceof Document doc) {
            return doc.getId();
        }
        return null;
    }
}
