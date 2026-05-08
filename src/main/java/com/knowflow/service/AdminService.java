package com.knowflow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.common.BusinessException;
import com.knowflow.common.PageResponse;
import com.knowflow.entity.DocumentProcessTask;
import com.knowflow.entity.User;
import com.knowflow.enums.DocumentParseStatus;
import com.knowflow.enums.KnowledgeBaseStatus;
import com.knowflow.enums.TaskStatus;
import com.knowflow.enums.UserStatus;
import com.knowflow.mapper.DocumentProcessTaskRepository;
import com.knowflow.mapper.DocumentRepository;
import com.knowflow.mapper.KnowledgeBaseRepository;
import com.knowflow.mapper.UserRepository;
import com.knowflow.security.SecurityUtils;
import com.knowflow.service.DocumentService;
import com.knowflow.service.KnowledgeBaseService;
import com.knowflow.service.OperationLogService;
import com.knowflow.vo.AdminOverviewVO;
import com.knowflow.vo.DocumentTaskVO;
import com.knowflow.vo.DocumentVO;
import com.knowflow.vo.KnowledgeBaseVO;
import com.knowflow.vo.UserVO;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AdminService {
    private final UserRepository userRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final DocumentProcessTaskRepository taskRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;
    private final OperationLogService operationLogService;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository,
                        KnowledgeBaseRepository knowledgeBaseRepository,
                        DocumentRepository documentRepository,
                        DocumentProcessTaskRepository taskRepository,
                        KnowledgeBaseService knowledgeBaseService,
                        DocumentService documentService,
                        OperationLogService operationLogService,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.documentRepository = documentRepository;
        this.taskRepository = taskRepository;
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentService = documentService;
        this.operationLogService = operationLogService;
        this.passwordEncoder = passwordEncoder;
    }

    public AdminOverviewVO overview() {
        SecurityUtils.requireAdmin();
        return new AdminOverviewVO(
                userRepository.count(),
                knowledgeBaseRepository.countByDeletedFalse(),
                documentRepository.countByDeletedFalse(),
                documentRepository.countByParseStatusAndDeletedFalse(DocumentParseStatus.FAILED),
                knowledgeBaseRepository.countByStatusAndDeletedFalse(KnowledgeBaseStatus.DISABLED)
        );
    }

    public PageResponse<UserVO> users(String keyword, UserStatus status, int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        Page<User> page = status == null
                ? userRepository.findByDeletedFalseAndUsernameContaining(keyword == null ? "" : keyword, new Page<>(pageNo, pageSize))
                : userRepository.findByDeletedFalseAndUsernameContainingAndStatus(keyword == null ? "" : keyword, status, new Page<>(pageNo, pageSize));
        return PageResponse.of(page.convert(UserVO::from));
    }

    public UserVO userDetail(Long userId) {
        SecurityUtils.requireAdmin();
        return userRepository.findByIdAndDeletedFalse(userId).map(UserVO::from)
                .orElseThrow(() -> BusinessException.notFound("user not found"));
    }

    @Transactional
    public UserVO setUserStatus(Long userId, UserStatus status) {
        SecurityUtils.requireAdmin();
        User user = userRepository.findByIdAndDeletedFalse(userId).orElseThrow(() -> BusinessException.notFound("用户不存在"));
        user.setStatus(status);
        userRepository.updateById(user);
        operationLogService.record("SET_USER_STATUS", "USER", userId, status.name());
        return UserVO.from(user);
    }

    @Transactional
    public void resetPassword(Long userId, String password) {
        SecurityUtils.requireAdmin();
        User user = userRepository.findByIdAndDeletedFalse(userId).orElseThrow(() -> BusinessException.notFound("user not found"));
        user.setPassword(passwordEncoder.encode(password));
        userRepository.updateById(user);
        operationLogService.record("RESET_PASSWORD", "USER", userId, "reset user password");
    }

    public PageResponse<KnowledgeBaseVO> knowledgeBases(String keyword, int pageNo, int pageSize) {
        return knowledgeBases(keyword, "", null, pageNo, pageSize);
    }

    public PageResponse<KnowledgeBaseVO> knowledgeBases(String keyword,
                                                        String username,
                                                        KnowledgeBaseStatus status,
                                                        int pageNo,
                                                        int pageSize) {
        SecurityUtils.requireAdmin();
        List<Long> userIds = null;
        if (username != null && !username.isBlank()) {
            userIds = userRepository.findIdsByUsernameContaining(username);
            if (userIds.isEmpty()) {
                return PageResponse.of(new Page<>(pageNo, pageSize));
            }
        }
        return PageResponse.of(knowledgeBaseRepository
                .findByAdminFilters(keyword == null ? "" : keyword, status, userIds, new Page<>(pageNo, pageSize))
                .convert(KnowledgeBaseVO::from));
    }

    public KnowledgeBaseVO knowledgeBaseDetail(Long id) {
        SecurityUtils.requireAdmin();
        return knowledgeBaseRepository.findByIdAndDeletedFalse(id).map(KnowledgeBaseVO::from)
                .orElseThrow(() -> BusinessException.notFound("知识库不存在"));
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
        kb.setDeleted(true);
        knowledgeBaseRepository.updateById(kb);
        operationLogService.record("DELETE_KB", "KNOWLEDGE_BASE", id, "admin delete knowledge base");
    }

    public PageResponse<DocumentVO> documents(String keyword, int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        return documents(keyword, null, null, null, pageNo, pageSize);
    }

    public PageResponse<DocumentVO> documents(String keyword,
                                              Long knowledgeBaseId,
                                              DocumentParseStatus parseStatus,
                                              String fileType,
                                              int pageNo,
                                              int pageSize) {
        SecurityUtils.requireAdmin();
        return PageResponse.of(documentRepository
                .findByAdminFilters(
                        keyword == null ? "" : keyword,
                        knowledgeBaseId,
                        parseStatus,
                        fileType,
                        new Page<>(pageNo, pageSize))
                .convert(DocumentVO::from));
    }

    public DocumentVO documentDetail(Long id) {
        SecurityUtils.requireAdmin();
        return documentRepository.findByIdAndDeletedFalse(id).map(DocumentVO::from)
                .orElseThrow(() -> BusinessException.notFound("文档不存在"));
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

    public PageResponse<DocumentTaskVO> tasks(int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        return tasks(null, "", pageNo, pageSize);
    }

    public PageResponse<DocumentTaskVO> tasks(TaskStatus status, String keyword, int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        return PageResponse.of(taskRepository
                .findByFilters(status, keyword == null ? "" : keyword, new Page<>(pageNo, pageSize))
                .convert(DocumentTaskVO::from));
    }

    public DocumentTaskVO taskDetail(Long id) {
        SecurityUtils.requireAdmin();
        DocumentProcessTask task = taskRepository.selectById(id);
        if (task == null) {
            throw BusinessException.notFound("任务不存在");
        }
        return DocumentTaskVO.from(task);
    }

    @Transactional
    public DocumentTaskVO retryTask(Long id) {
        SecurityUtils.requireAdmin();
        DocumentProcessTask task = taskRepository.selectById(id);
        if (task == null) {
            throw BusinessException.notFound("任务不存在");
        }
        Long documentId = task.getDocumentId();
        if (documentId == null) {
            throw BusinessException.badRequest("任务未关联文档，无法重试");
        }
        if (taskRepository.existsActiveByDocumentId(documentId)) {
            throw BusinessException.badRequest("当前文档已有处理中任务，请稍后再试");
        }

        documentService.adminRetry(documentId);
        DocumentProcessTask created = taskRepository.findLatestByDocumentId(documentId)
                .orElseThrow(() -> BusinessException.badRequest("重试任务创建失败"));
        operationLogService.record("RETRY_DOCUMENT_TASK", "DOCUMENT_TASK", id, "retry by admin, new taskId=" + created.getId());
        return DocumentTaskVO.from(created);
    }
}
