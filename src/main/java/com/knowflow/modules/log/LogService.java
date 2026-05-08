package com.knowflow.modules.log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.common.PageResponse;
import com.knowflow.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

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

    public PageResponse<LogVO> operationLogs(String action, Long userId, LocalDateTime startTime, LocalDateTime endTime, int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        LambdaQueryWrapper<OperationLog> query = new LambdaQueryWrapper<OperationLog>()
                .like(StringUtils.hasText(action), OperationLog::getAction, action)
                .eq(userId != null, OperationLog::getUserId, userId)
                .ge(startTime != null, OperationLog::getCreateTime, startTime)
                .le(endTime != null, OperationLog::getCreateTime, endTime)
                .orderByDesc(OperationLog::getCreateTime);
        return PageResponse.of(operationLogRepository.selectPage(new Page<>(pageNo, pageSize), query).convert(LogVO::from));
    }

    public PageResponse<LogVO> loginLogs(String account, Boolean success, LocalDateTime startTime, LocalDateTime endTime, int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        LambdaQueryWrapper<LoginLog> query = new LambdaQueryWrapper<LoginLog>()
                .like(StringUtils.hasText(account), LoginLog::getAccount, account)
                .eq(success != null, LoginLog::getSuccess, success)
                .ge(startTime != null, LoginLog::getCreateTime, startTime)
                .le(endTime != null, LoginLog::getCreateTime, endTime)
                .orderByDesc(LoginLog::getCreateTime);
        return PageResponse.of(loginLogRepository.selectPage(new Page<>(pageNo, pageSize), query).convert(LogVO::from));
    }

    public PageResponse<LogVO> aiCallLogs(String modelName, String callType, Boolean success, LocalDateTime startTime, LocalDateTime endTime, int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        LambdaQueryWrapper<AiCallLog> query = new LambdaQueryWrapper<AiCallLog>()
                .like(StringUtils.hasText(modelName), AiCallLog::getModelName, modelName)
                .eq(StringUtils.hasText(callType), AiCallLog::getCallType, callType)
                .eq(success != null, AiCallLog::getSuccess, success)
                .ge(startTime != null, AiCallLog::getCreateTime, startTime)
                .le(endTime != null, AiCallLog::getCreateTime, endTime)
                .orderByDesc(AiCallLog::getCreateTime);
        return PageResponse.of(aiCallLogRepository.selectPage(new Page<>(pageNo, pageSize), query).convert(LogVO::from));
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
