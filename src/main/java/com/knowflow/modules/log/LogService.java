package com.knowflow.modules.log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.common.PageResponse;
import com.knowflow.security.SecurityUtils;
import org.springframework.stereotype.Service;

@Service
public class LogService {
    private final OperationLogRepository operationLogRepository;
    private final LoginLogRepository loginLogRepository;
    private final AiCallLogRepository aiCallLogRepository;

    public LogService(OperationLogRepository operationLogRepository, LoginLogRepository loginLogRepository, AiCallLogRepository aiCallLogRepository) {
        this.operationLogRepository = operationLogRepository;
        this.loginLogRepository = loginLogRepository;
        this.aiCallLogRepository = aiCallLogRepository;
    }

    public PageResponse<LogVO> operationLogs(int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        return PageResponse.of(operationLogRepository.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<OperationLog>().orderByDesc(OperationLog::getCreateTime)).convert(LogVO::from));
    }

    public PageResponse<LogVO> loginLogs(int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        return PageResponse.of(loginLogRepository.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<LoginLog>().orderByDesc(LoginLog::getCreateTime)).convert(LogVO::from));
    }

    public PageResponse<LogVO> aiCallLogs(int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        return PageResponse.of(aiCallLogRepository.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<AiCallLog>().orderByDesc(AiCallLog::getCreateTime)).convert(LogVO::from));
    }

    public void recordAiCall(Long userId, String modelName, String callType, long elapsedMs, boolean success, String failReason) {
        AiCallLog log = new AiCallLog();
        log.setUserId(userId);
        log.setModelName(modelName);
        log.setCallType(callType);
        log.setElapsedMs(elapsedMs);
        log.setSuccess(success);
        log.setFailReason(failReason);
        aiCallLogRepository.insert(log);
    }

    public void recordLogin(Long userId, String account, boolean success, String message) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setAccount(account);
        log.setSuccess(success);
        log.setMessage(message);
        loginLogRepository.insert(log);
    }
}
