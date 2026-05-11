package com.knowflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.common.BusinessException;
import com.knowflow.common.PageResponse;
import com.knowflow.entity.AiCallLog;
import com.knowflow.entity.Document;
import com.knowflow.entity.DocumentProcessTask;
import com.knowflow.entity.LoginLog;
import com.knowflow.entity.User;
import com.knowflow.enums.DocumentParseStatus;
import com.knowflow.enums.EmbeddingStatus;
import com.knowflow.enums.KnowledgeBaseStatus;
import com.knowflow.enums.TaskStatus;
import com.knowflow.enums.UserStatus;
import com.knowflow.mapper.AiCallLogRepository;
import com.knowflow.mapper.DocumentChunkRepository;
import com.knowflow.mapper.DocumentProcessTaskRepository;
import com.knowflow.mapper.DocumentRepository;
import com.knowflow.mapper.KnowledgeBaseRepository;
import com.knowflow.mapper.LoginLogRepository;
import com.knowflow.mapper.UserRepository;
import com.knowflow.security.SecurityUtils;
import com.knowflow.vo.AdminOverviewVO;
import com.knowflow.vo.DocumentChunkVO;
import com.knowflow.vo.DocumentTaskVO;
import com.knowflow.vo.DocumentVO;
import com.knowflow.vo.KnowledgeBaseVO;
import com.knowflow.vo.LogVO;
import com.knowflow.vo.UserVO;
import java.util.Arrays;
import java.util.Collections;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final LoginLogRepository loginLogRepository;
    private final AiCallLogRepository aiCallLogRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentProcessTaskRepository taskRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;
    private final OperationLogService operationLogService;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository,
                        KnowledgeBaseRepository knowledgeBaseRepository,
                        DocumentRepository documentRepository,
                        LoginLogRepository loginLogRepository,
                        AiCallLogRepository aiCallLogRepository,
                        DocumentChunkRepository chunkRepository,
                        DocumentProcessTaskRepository taskRepository,
                        KnowledgeBaseService knowledgeBaseService,
                        DocumentService documentService,
                        OperationLogService operationLogService,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.documentRepository = documentRepository;
        this.loginLogRepository = loginLogRepository;
        this.aiCallLogRepository = aiCallLogRepository;
        this.chunkRepository = chunkRepository;
        this.taskRepository = taskRepository;
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentService = documentService;
        this.operationLogService = operationLogService;
        this.passwordEncoder = passwordEncoder;
    }

    public AdminOverviewVO overview() {
        SecurityUtils.requireAdmin();
        LocalDateTime today = LocalDate.now().atStartOfDay();
        long qaCount = aiCallLogRepository.selectCount(new LambdaQueryWrapper<AiCallLog>());
        long todayUserCount = userRepository.selectCount(new LambdaQueryWrapper<User>().ge(User::getCreateTime, today));
        long todayDocumentCount = documentRepository.selectCount(new LambdaQueryWrapper<Document>().ge(Document::getCreateTime, today));
        long todayQaCount = aiCallLogRepository.selectCount(new LambdaQueryWrapper<AiCallLog>()
                .ge(AiCallLog::getCreateTime, today));
        List<UserVO> recentUsers = userRepository.selectList(new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime).last("limit 5"))
                .stream().map(this::enrichUser).toList();
        List<KnowledgeBaseVO> recentKnowledgeBases = knowledgeBaseRepository.selectList(
                        new LambdaQueryWrapper<com.knowflow.entity.KnowledgeBase>()
                                .eq(com.knowflow.entity.KnowledgeBase::getDeleted, false)
                                .orderByDesc(com.knowflow.entity.KnowledgeBase::getUpdateTime)
                                .last("limit 5"))
                .stream().map(KnowledgeBaseVO::from).toList();
        List<DocumentVO> recentDocuments = documentRepository.selectList(new LambdaQueryWrapper<Document>()
                        .eq(Document::getDeleted, false)
                        .orderByDesc(Document::getCreateTime)
                        .last("limit 5"))
                .stream().map(DocumentVO::from).map(this::enrichDocument).toList();
        List<DocumentTaskVO> recentFailedTasks = taskRepository.selectList(new LambdaQueryWrapper<DocumentProcessTask>()
                        .eq(DocumentProcessTask::getStatus, TaskStatus.FAILED)
                        .orderByDesc(DocumentProcessTask::getCreateTime).last("limit 5"))
                .stream().map(this::toTaskVO).toList();
        List<LogVO> recentAiErrors = aiCallLogRepository.selectList(new LambdaQueryWrapper<AiCallLog>()
                        .eq(AiCallLog::getSuccess, false)
                        .orderByDesc(AiCallLog::getCreateTime).last("limit 5"))
                .stream().map(this::toAiErrorLog).toList();
        return new AdminOverviewVO(
                userRepository.count(),
                knowledgeBaseRepository.countByDeletedFalse(),
                documentRepository.countByDeletedFalse(),
                qaCount,
                todayUserCount,
                todayDocumentCount,
                todayQaCount,
                documentRepository.countByParseStatusAndDeletedFalse(DocumentParseStatus.FAILED),
                knowledgeBaseRepository.countByStatusAndDeletedFalse(KnowledgeBaseStatus.DISABLED),
                recentUsers,
                recentKnowledgeBases,
                recentDocuments,
                recentFailedTasks,
                recentAiErrors
        );
    }

    public PageResponse<UserVO> users(String keyword, UserStatus status, int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        Page<User> page = status == null
                ? userRepository.findByDeletedFalseAndUsernameContaining(keyword == null ? "" : keyword, new Page<>(pageNo, pageSize))
                : userRepository.findByDeletedFalseAndUsernameContainingAndStatus(keyword == null ? "" : keyword, status, new Page<>(pageNo, pageSize));
        return PageResponse.of(page.convert(this::enrichUser));
    }

    public UserVO userDetail(Long userId) {
        SecurityUtils.requireAdmin();
        return userRepository.findByIdAndDeletedFalse(userId).map(this::enrichUser)
                .orElseThrow(() -> BusinessException.notFound("user not found"));
    }

    @Transactional
    public UserVO setUserStatus(Long userId, UserStatus status) {
        SecurityUtils.requireAdmin();
        User user = userRepository.findByIdAndDeletedFalse(userId).orElseThrow(() -> BusinessException.notFound("user not found"));
        user.setStatus(status);
        userRepository.updateById(user);
        operationLogService.record("SET_USER_STATUS", "USER", userId, status.name());
        return enrichUser(user);
    }

    @Transactional
    public void resetPassword(Long userId, String password) {
        SecurityUtils.requireAdmin();
        User user = userRepository.findByIdAndDeletedFalse(userId).orElseThrow(() -> BusinessException.notFound("user not found"));
        user.setPassword(passwordEncoder.encode(password));
        userRepository.updateById(user);
        operationLogService.record("RESET_PASSWORD", "USER", userId, "reset user password");
    }

    public PageResponse<KnowledgeBaseVO> knowledgeBases(String keyword, KnowledgeBaseStatus status, int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        return PageResponse.of(knowledgeBaseRepository
                .findByAdminFilters(keyword == null ? "" : keyword, status, new Page<>(pageNo, pageSize))
                .convert(KnowledgeBaseVO::from));
    }

    public KnowledgeBaseVO knowledgeBaseDetail(Long id) {
        SecurityUtils.requireAdmin();
        return knowledgeBaseRepository.findByIdAndDeletedFalse(id).map(KnowledgeBaseVO::from)
                .orElseThrow(() -> BusinessException.notFound("knowledge base not found"));
    }

    @Transactional
    public KnowledgeBaseVO setKnowledgeBaseStatus(Long id, KnowledgeBaseStatus status) {
        SecurityUtils.requireAdmin();
        var kb = knowledgeBaseRepository.findByIdAndDeletedFalse(id).orElseThrow(() -> BusinessException.notFound("knowledge base not found"));
        kb.setStatus(status);
        knowledgeBaseRepository.updateById(kb);
        operationLogService.record("SET_KB_STATUS", "KNOWLEDGE_BASE", id, status.name());
        return KnowledgeBaseVO.from(kb);
    }

    @Transactional
    public void deleteKnowledgeBase(Long id) {
        SecurityUtils.requireAdmin();
        var kb = knowledgeBaseRepository.findByIdAndDeletedFalse(id).orElseThrow(() -> BusinessException.notFound("knowledge base not found"));
        chunkRepository.deleteByKnowledgeBaseId(id);
        taskRepository.deleteByKnowledgeBaseId(id);
        documentRepository.deleteByKnowledgeBaseId(id);
        knowledgeBaseRepository.deleteById(kb.getId());
        operationLogService.record("DELETE_KB", "KNOWLEDGE_BASE", id, "admin delete knowledge base");
    }

    public PageResponse<DocumentVO> documents(String keyword, Long knowledgeBaseId, Long userId, String username,
                                              DocumentParseStatus parseStatus, EmbeddingStatus embeddingStatus,
                                              String fileType, int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        List<Long> userIds = null;
        if (username != null && !username.isBlank()) {
            userIds = userRepository.selectList(new LambdaQueryWrapper<User>().like(User::getUsername, username))
                    .stream().map(User::getId).toList();
            if (userIds.isEmpty()) {
                return new PageResponse<>(List.of(), 0, pageNo, pageSize);
            }
        }
        return PageResponse.of(documentRepository
                .findByAdminFilters(keyword == null ? "" : keyword, knowledgeBaseId, userId, userIds, parseStatus, embeddingStatus, fileType, new Page<>(pageNo, pageSize))
                .convert(DocumentVO::from)
                .convert(this::enrichDocument));
    }

    public DocumentVO documentDetail(Long id) {
        SecurityUtils.requireAdmin();
        return documentRepository.findByIdAndDeletedFalse(id).map(DocumentVO::from)
                .map(this::enrichDocument)
                .orElseThrow(() -> BusinessException.notFound("document not found"));
    }

    public DownloadFile adminDownload(Long id) {
        SecurityUtils.requireAdmin();
        Document document = documentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> BusinessException.notFound("document not found"));
        Path path = documentService.adminDownloadPath(id);
        String filename = (document.getOriginalName() != null && !document.getOriginalName().isBlank())
                ? document.getOriginalName()
                : ((document.getName() != null && !document.getName().isBlank()) ? document.getName() : "document-" + id);
        operationLogService.record("DOWNLOAD_DOCUMENT", "DOCUMENT", id, "admin download document");
        return new DownloadFile(path, filename);
    }

    public PageResponse<DocumentChunkVO> documentChunks(Long id, int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        return documentService.adminPageChunks(id, pageNo, pageSize);
    }

    @Transactional
    public void deleteDocument(Long id) {
        SecurityUtils.requireAdmin();
        documentService.adminDelete(id);
        operationLogService.record("DELETE_DOCUMENT", "DOCUMENT", id, "admin delete document");
    }

    @Transactional
    public DocumentVO retryDocument(Long id) {
        SecurityUtils.requireAdmin();
        return documentService.adminRetry(id);
    }

    public PageResponse<DocumentTaskVO> tasks(TaskStatus status, String taskType, Long documentId, String keyword, int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        String keywordValue = keyword == null ? "" : keyword;
        List<Long> matchedDocumentIds = resolveMatchedDocumentIds(keywordValue);
        Page<DocumentProcessTask> page = taskRepository.findByFilters(status, taskType, documentId, keywordValue, matchedDocumentIds, new Page<>(pageNo, pageSize));
        return PageResponse.of(page.convert(this::toTaskVO));
    }

    private List<Long> resolveMatchedDocumentIds(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return documentRepository.selectList(new QueryWrapper<Document>()
                        .select("id")
                        .apply("lower(name) like {0}", pattern))
                .stream()
                .map(Document::getId)
                .toList();
    }

    public DocumentTaskVO taskDetail(Long id) {
        SecurityUtils.requireAdmin();
        DocumentProcessTask task = taskRepository.selectById(id);
        if (task == null) {
            throw BusinessException.notFound("task not found");
        }
        return toTaskVO(task);
    }

    @Transactional
    public DocumentTaskVO retryTask(Long id) {
        SecurityUtils.requireAdmin();
        DocumentProcessTask task = taskRepository.selectById(id);
        if (task == null) {
            throw BusinessException.notFound("task not found");
        }
        if (task.getStatus() != TaskStatus.FAILED) {
            throw BusinessException.badRequest("当前任务无需重试");
        }
        Long documentId = task.getDocumentId();
        if (documentId == null) {
            throw BusinessException.badRequest("task does not bind document");
        }
        if (taskRepository.existsActiveByDocumentId(documentId)) {
            throw BusinessException.badRequest("document already has active task");
        }
        documentService.adminRetry(documentId);
        DocumentProcessTask created = taskRepository.findLatestByDocumentId(documentId)
                .orElseThrow(() -> BusinessException.badRequest("retry task create failed"));
        operationLogService.record("RETRY_DOCUMENT_TASK", "DOCUMENT_TASK", id, "retry by admin, new taskId=" + created.getId());
        return toTaskVO(created);
    }

    private DocumentTaskVO toTaskVO(DocumentProcessTask task) {
        Document doc = task.getDocumentId() == null ? null : documentRepository.selectById(task.getDocumentId());
        boolean documentDeleted = doc == null;
        String documentName = doc == null ? task.getDocumentNameSnapshot() : doc.getName();
        if (documentName == null || documentName.isBlank()) {
            documentName = "未命名文档";
        }
        String failureReason = task.getFailReason();
        if ((failureReason == null || failureReason.isBlank()) && task.getStatus() == TaskStatus.FAILED) {
            List<String> logs = parseLogs(task.getLogsJson());
            failureReason = logs.isEmpty() ? "任务执行失败" : logs.get(logs.size() - 1);
        }
        return new DocumentTaskVO(
                task.getId(),
                task.getTaskType(),
                task.getDocumentId(),
                documentName,
                documentDeleted,
                task.getStatus(),
                task.getCreateTime(),
                task.getStartedAt(),
                task.getFinishedAt(),
                task.getDurationMs(),
                failureReason,
                parseLogs(task.getLogsJson())
        );
    }

    private LogVO toAiErrorLog(AiCallLog log) {
        String failureReason = (log.getFailReason() == null || log.getFailReason().isBlank())
                ? "AI 调用失败"
                : log.getFailReason();
        return new LogVO(
                log.getId(),
                log.getUserId(),
                "AI_CALL",
                "AI",
                log.getCallType(),
                "",
                "FAILED",
                (log.getModelName() == null || log.getModelName().isBlank())
                        ? failureReason
                        : (log.getModelName() + "：" + failureReason),
                null,
                null,
                null,
                log.getModel(),
                log.getModelName(),
                log.getModelType(),
                log.getProvider(),
                log.getPromptTokens(),
                log.getCompletionTokens(),
                log.getTotalTokens(),
                log.getElapsedMs(),
                log.getCreateTime()
        );
    }

    private List<String> parseLogs(String logsJson) {
        if (logsJson == null || logsJson.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(logsJson.split("\\R"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private UserVO enrichUser(User user) {
        long kbCount = knowledgeBaseRepository.selectCount(new LambdaQueryWrapper<com.knowflow.entity.KnowledgeBase>()
                .eq(com.knowflow.entity.KnowledgeBase::getUserId, user.getId()));
        long docCount = documentRepository.countByUserIdAndDeletedFalse(user.getId());
        long qaCount = 0L;
        LoginLog lastLogin = loginLogRepository.selectOne(new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getUserId, user.getId())
                .eq(LoginLog::getSuccess, true)
                .orderByDesc(LoginLog::getCreateTime)
                .last("limit 1"));
        return UserVO.enrich(UserVO.from(user), kbCount, docCount, qaCount, lastLogin == null ? null : lastLogin.getCreateTime());
    }

    private DocumentVO enrichDocument(DocumentVO base) {
        String knowledgeBaseName = null;
        if (base.knowledgeBaseId() != null) {
            knowledgeBaseName = knowledgeBaseRepository.findByIdAndDeletedFalse(base.knowledgeBaseId())
                    .map(com.knowflow.entity.KnowledgeBase::getName)
                    .orElse(null);
        }
        Long uid = base.userId() != null ? base.userId() : base.uploaderId();
        String uploaderName = null;
        if (uid != null) {
            uploaderName = userRepository.findByIdAndDeletedFalse(uid)
                    .map(u -> (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername())
                    .orElse(null);
        }
        Long chunkCount = chunkRepository.countByDocumentId(base.id());
        return DocumentVO.enrich(base, knowledgeBaseName, uploaderName, chunkCount);
    }

    public record DownloadFile(Path path, String filename) {
    }
}
