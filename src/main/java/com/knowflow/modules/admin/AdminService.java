package com.knowflow.modules.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.common.BusinessException;
import com.knowflow.common.PageResponse;
import com.knowflow.common.enums.DocumentParseStatus;
import com.knowflow.common.enums.KnowledgeBaseStatus;
import com.knowflow.common.enums.UserStatus;
import com.knowflow.modules.document.DocumentProcessTaskRepository;
import com.knowflow.modules.document.DocumentProcessTask;
import com.knowflow.modules.document.DocumentRepository;
import com.knowflow.modules.document.DocumentService;
import com.knowflow.modules.document.DocumentTaskVO;
import com.knowflow.modules.document.DocumentVO;
import com.knowflow.modules.knowledge.KnowledgeBaseRepository;
import com.knowflow.modules.knowledge.KnowledgeBaseService;
import com.knowflow.modules.knowledge.KnowledgeBaseVO;
import com.knowflow.modules.log.OperationLogService;
import com.knowflow.modules.user.User;
import com.knowflow.modules.user.UserRepository;
import com.knowflow.modules.user.UserVO;
import com.knowflow.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
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
        SecurityUtils.requireAdmin();
        return PageResponse.of(knowledgeBaseRepository
                .findByDeletedFalseAndNameContaining(keyword == null ? "" : keyword, new Page<>(pageNo, pageSize))
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
        return PageResponse.of(documentRepository
                .findByDeletedFalseAndNameContaining(keyword == null ? "" : keyword, new Page<>(pageNo, pageSize))
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
        return PageResponse.of(taskRepository
                .findByDeletedFalse(new Page<>(pageNo, pageSize))
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
}
