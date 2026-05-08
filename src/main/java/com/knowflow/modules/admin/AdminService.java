package com.knowflow.modules.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.common.BusinessException;
import com.knowflow.common.PageResponse;
import com.knowflow.common.enums.DocumentParseStatus;
import com.knowflow.common.enums.KnowledgeBaseStatus;
import com.knowflow.common.enums.UserStatus;
import com.knowflow.modules.document.DocumentProcessTaskRepository;
import com.knowflow.modules.document.DocumentRepository;
import com.knowflow.modules.document.DocumentTaskVO;
import com.knowflow.modules.document.DocumentVO;
import com.knowflow.modules.knowledge.KnowledgeBaseRepository;
import com.knowflow.modules.knowledge.KnowledgeBaseVO;
import com.knowflow.modules.log.OperationLogService;
import com.knowflow.modules.user.User;
import com.knowflow.modules.user.UserRepository;
import com.knowflow.modules.user.UserVO;
import com.knowflow.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final DocumentProcessTaskRepository taskRepository;
    private final OperationLogService operationLogService;

    public AdminService(UserRepository userRepository,
                        KnowledgeBaseRepository knowledgeBaseRepository,
                        DocumentRepository documentRepository,
                        DocumentProcessTaskRepository taskRepository,
                        OperationLogService operationLogService) {
        this.userRepository = userRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.documentRepository = documentRepository;
        this.taskRepository = taskRepository;
        this.operationLogService = operationLogService;
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

    @Transactional
    public UserVO setUserStatus(Long userId, UserStatus status) {
        SecurityUtils.requireAdmin();
        User user = userRepository.findByIdAndDeletedFalse(userId).orElseThrow(() -> BusinessException.notFound("用户不存在"));
        user.setStatus(status);
        userRepository.updateById(user);
        operationLogService.record("SET_USER_STATUS", "USER", userId, status.name());
        return UserVO.from(user);
    }

    public PageResponse<KnowledgeBaseVO> knowledgeBases(String keyword, int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        return PageResponse.of(knowledgeBaseRepository
                .findByDeletedFalseAndNameContaining(keyword == null ? "" : keyword, new Page<>(pageNo, pageSize))
                .convert(KnowledgeBaseVO::from));
    }

    public PageResponse<DocumentVO> documents(String keyword, int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        return PageResponse.of(documentRepository
                .findByDeletedFalseAndNameContaining(keyword == null ? "" : keyword, new Page<>(pageNo, pageSize))
                .convert(DocumentVO::from));
    }

    public PageResponse<DocumentTaskVO> tasks(int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        return PageResponse.of(taskRepository
                .findByDeletedFalse(new Page<>(pageNo, pageSize))
                .convert(DocumentTaskVO::from));
    }
}
