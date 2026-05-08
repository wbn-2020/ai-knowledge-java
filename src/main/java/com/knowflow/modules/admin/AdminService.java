package com.knowflow.modules.admin;

import com.knowflow.common.BusinessException;
import com.knowflow.common.PageResponse;
import com.knowflow.common.enums.DocumentParseStatus;
import com.knowflow.common.enums.KnowledgeBaseStatus;
import com.knowflow.common.enums.UserStatus;
import com.knowflow.modules.document.DocumentRepository;
import com.knowflow.modules.document.DocumentTaskVO;
import com.knowflow.modules.document.DocumentVO;
import com.knowflow.modules.document.DocumentProcessTaskRepository;
import com.knowflow.modules.knowledge.KnowledgeBaseRepository;
import com.knowflow.modules.knowledge.KnowledgeBaseVO;
import com.knowflow.modules.log.OperationLogService;
import com.knowflow.modules.user.User;
import com.knowflow.modules.user.UserRepository;
import com.knowflow.modules.user.UserVO;
import com.knowflow.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
        if (status == null) {
            return PageResponse.of(userRepository.findByDeletedFalseAndUsernameContaining(keyword == null ? "" : keyword, PageRequest.of(pageNo - 1, pageSize)).map(UserVO::from));
        }
        return PageResponse.of(userRepository.findByDeletedFalseAndUsernameContainingAndStatus(keyword == null ? "" : keyword, status, PageRequest.of(pageNo - 1, pageSize)).map(UserVO::from));
    }

    @Transactional
    public UserVO setUserStatus(Long userId, UserStatus status) {
        SecurityUtils.requireAdmin();
        User user = userRepository.findByIdAndDeletedFalse(userId).orElseThrow(() -> BusinessException.notFound("用户不存在"));
        user.setStatus(status);
        operationLogService.record("SET_USER_STATUS", "USER", userId, status.name());
        return UserVO.from(userRepository.save(user));
    }

    public PageResponse<KnowledgeBaseVO> knowledgeBases(String keyword, int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        return PageResponse.of(knowledgeBaseRepository
                .findByDeletedFalseAndNameContaining(keyword == null ? "" : keyword, PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime")))
                .map(KnowledgeBaseVO::from));
    }

    public PageResponse<DocumentVO> documents(String keyword, int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        return PageResponse.of(documentRepository
                .findByDeletedFalseAndNameContaining(keyword == null ? "" : keyword, PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime")))
                .map(DocumentVO::from));
    }

    public PageResponse<DocumentTaskVO> tasks(int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        return PageResponse.of(taskRepository
                .findByDeletedFalse(PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime")))
                .map(DocumentTaskVO::from));
    }
}
