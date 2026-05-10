package com.knowflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowflow.common.PageResponse;
import com.knowflow.entity.AiCallLog;
import com.knowflow.entity.Document;
import com.knowflow.entity.LoginLog;
import com.knowflow.entity.OperationLog;
import com.knowflow.enums.DocumentParseStatus;
import com.knowflow.mapper.AiCallLogRepository;
import com.knowflow.mapper.DocumentRepository;
import com.knowflow.mapper.LoginLogRepository;
import com.knowflow.mapper.OperationLogRepository;
import com.knowflow.security.SecurityUtils;
import com.knowflow.vo.AlertVO;
import com.knowflow.vo.LogVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.StringJoiner;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;



@Service
public class LogService {
    private final OperationLogRepository operationLogRepository;
    private final LoginLogRepository loginLogRepository;
    private final AiCallLogRepository aiCallLogRepository;
    private final DocumentRepository documentRepository;

    public LogService(OperationLogRepository operationLogRepository,
                      LoginLogRepository loginLogRepository,
                      AiCallLogRepository aiCallLogRepository,
                      DocumentRepository documentRepository) {
        this.operationLogRepository = operationLogRepository;
        this.loginLogRepository = loginLogRepository;
        this.aiCallLogRepository = aiCallLogRepository;
        this.documentRepository = documentRepository;
    }

    public PageResponse<LogVO> operationLogs(String action, Long userId, LocalDateTime startTime, LocalDateTime endTime, int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        return PageResponse.of(operationLogRepository.selectPage(new Page<>(pageNo, pageSize),
                operationQuery(action, userId, startTime, endTime)).convert(LogVO::from));
    }

    public PageResponse<LogVO> loginLogs(String account, Boolean success, LocalDateTime startTime, LocalDateTime endTime, int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        return PageResponse.of(loginLogRepository.selectPage(new Page<>(pageNo, pageSize),
                loginQuery(account, success, startTime, endTime)).convert(LogVO::from));
    }

    public PageResponse<LogVO> aiCallLogs(String modelName, String callType, Boolean success, LocalDateTime startTime, LocalDateTime endTime, int pageNo, int pageSize) {
        SecurityUtils.requireAdmin();
        return PageResponse.of(aiCallLogRepository.selectPage(new Page<>(pageNo, pageSize),
                aiQuery(modelName, callType, success, startTime, endTime)).convert(LogVO::from));
    }

    public String exportOperations(String action, Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        SecurityUtils.requireAdmin();
        StringBuilder csv = new StringBuilder("id,userId,action,targetType,targetId,detail,createTime\n");
        operationLogRepository.selectList(operationQuery(action, userId, startTime, endTime))
                .forEach(log -> csv.append(csv(log.getId(), log.getUserId(), log.getAction(), log.getTargetType(), log.getTargetId(), log.getDetail(), log.getCreateTime())).append('\n'));
        return csv.toString();
    }

    public String exportLogins(String account, Boolean success, LocalDateTime startTime, LocalDateTime endTime) {
        SecurityUtils.requireAdmin();
        StringBuilder csv = new StringBuilder("id,userId,account,success,message,createTime\n");
        loginLogRepository.selectList(loginQuery(account, success, startTime, endTime))
                .forEach(log -> csv.append(csv(log.getId(), log.getUserId(), log.getAccount(), log.getSuccess(), log.getMessage(), log.getCreateTime())).append('\n'));
        return csv.toString();
    }

    public String exportAiCalls(String modelName, String callType, Boolean success, LocalDateTime startTime, LocalDateTime endTime) {
        SecurityUtils.requireAdmin();
        StringBuilder csv = new StringBuilder("id,userId,modelName,callType,elapsedMs,success,failReason,createTime\n");
        aiCallLogRepository.selectList(aiQuery(modelName, callType, success, startTime, endTime))
                .forEach(log -> csv.append(csv(log.getId(), log.getUserId(), log.getModelName(), log.getCallType(), log.getElapsedMs(), log.getSuccess(), log.getFailReason(), log.getCreateTime())).append('\n'));
        return csv.toString();
    }

    public List<AlertVO> alerts() {
        SecurityUtils.requireAdmin();
        long aiTotal = aiCallLogRepository.selectCount(new LambdaQueryWrapper<AiCallLog>());
        long aiFailed = aiCallLogRepository.selectCount(new LambdaQueryWrapper<AiCallLog>().eq(AiCallLog::getSuccess, false));
        long parseTotal = documentRepository.selectCount(new LambdaQueryWrapper<Document>());
        long parseFailed = documentRepository.selectCount(new LambdaQueryWrapper<Document>().eq(Document::getParseStatus, DocumentParseStatus.FAILED));
        long loginTotal = loginLogRepository.selectCount(new LambdaQueryWrapper<LoginLog>());
        long loginFailed = loginLogRepository.selectCount(new LambdaQueryWrapper<LoginLog>().eq(LoginLog::getSuccess, false));
        return List.of(
                alert("AI_CALL_FAILURE", aiTotal, aiFailed, 0.2),
                alert("DOCUMENT_PARSE_FAILURE", parseTotal, parseFailed, 0.2),
                alert("LOGIN_FAILURE", loginTotal, loginFailed, 0.3)
        );
    }

    public void recordAiCall(Long userId, String modelName, String callType, long elapsedMs, boolean success, String failReason) {
        recordAiCall(userId, null, null, modelName, "CHAT".equalsIgnoreCase(callType) ? "LLM" : "EMBEDDING",
                "DEEPSEEK", callType, elapsedMs, success, failReason, null, null);
    }

    public void recordAiCall(Long userId,
                             Long knowledgeBaseId,
                             Long sessionId,
                             String modelName,
                             String modelType,
                             String provider,
                             String scene,
                             long durationMs,
                             boolean success,
                             String errorMessage,
                             Integer inputTokens,
                             Integer outputTokens) {
        AiCallLog log = new AiCallLog();
        log.setUserId(userId);
        log.setKnowledgeBaseId(knowledgeBaseId);
        log.setSessionId(sessionId);
        log.setModel(modelName);
        log.setModelName(modelName);
        log.setModelType(modelType);
        log.setProvider(provider);
        log.setCallType(scene);
        log.setPromptTokens(inputTokens);
        log.setCompletionTokens(outputTokens);
        if (inputTokens != null || outputTokens != null) {
            log.setTotalTokens((inputTokens == null ? 0 : inputTokens) + (outputTokens == null ? 0 : outputTokens));
        }
        log.setElapsedMs(durationMs);
        log.setSuccess(success);
        log.setFailReason(errorMessage);
        aiCallLogRepository.insert(log);
    }

    public void recordLogin(Long userId, String account, boolean success, String message) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setAccount(account);
        log.setIp(clientIp());
        log.setUserAgent(parseDeviceInfo(userAgent()));
        log.setSuccess(success);
        log.setMessage(message);
        log.setFailureReason(success ? null : message);
        loginLogRepository.insert(log);
    }

    private String clientIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return "";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent() {
        HttpServletRequest request = currentRequest();
        return request == null ? "" : StringUtils.trimWhitespace(request.getHeader("User-Agent"));
    }

    private String parseDeviceInfo(String ua) {
        if (!StringUtils.hasText(ua)) {
            return "Unknown / Unknown";
        }
        String lower = ua.toLowerCase();
        String browser = "Other";
        if (lower.contains("edg/")) {
            browser = "Edge";
        } else if (lower.contains("chrome/")) {
            browser = "Chrome";
        } else if (lower.contains("firefox/")) {
            browser = "Firefox";
        } else if (lower.contains("safari/") && !lower.contains("chrome/")) {
            browser = "Safari";
        }
        String os = "Other";
        if (lower.contains("windows")) {
            os = "Windows";
        } else if (lower.contains("mac os")) {
            os = "macOS";
        } else if (lower.contains("android")) {
            os = "Android";
        } else if (lower.contains("iphone") || lower.contains("ios")) {
            os = "iOS";
        } else if (lower.contains("linux")) {
            os = "Linux";
        }
        return browser + " / " + os;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private AlertVO alert(String type, long total, long failed, double threshold) {
        double rate = total == 0 ? 0 : (double) failed / total;
        String level = rate >= threshold ? "WARN" : "OK";
        String message = type + " failure rate is " + String.format("%.2f", rate * 100) + "%";
        return new AlertVO(type, level, message, total, failed, rate);
    }

    private LambdaQueryWrapper<OperationLog> operationQuery(String action, Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return new LambdaQueryWrapper<OperationLog>()
                .like(StringUtils.hasText(action), OperationLog::getAction, action)
                .eq(userId != null, OperationLog::getUserId, userId)
                .ge(startTime != null, OperationLog::getCreateTime, startTime)
                .le(endTime != null, OperationLog::getCreateTime, endTime)
                .orderByDesc(OperationLog::getCreateTime);
    }

    private LambdaQueryWrapper<LoginLog> loginQuery(String account, Boolean success, LocalDateTime startTime, LocalDateTime endTime) {
        return new LambdaQueryWrapper<LoginLog>()
                .like(StringUtils.hasText(account), LoginLog::getAccount, account)
                .eq(success != null, LoginLog::getSuccess, success)
                .ge(startTime != null, LoginLog::getCreateTime, startTime)
                .le(endTime != null, LoginLog::getCreateTime, endTime)
                .orderByDesc(LoginLog::getCreateTime);
    }

    private LambdaQueryWrapper<AiCallLog> aiQuery(String modelName, String callType, Boolean success, LocalDateTime startTime, LocalDateTime endTime) {
        return new LambdaQueryWrapper<AiCallLog>()
                .like(StringUtils.hasText(modelName), AiCallLog::getModelName, modelName)
                .eq(StringUtils.hasText(callType), AiCallLog::getCallType, callType)
                .eq(success != null, AiCallLog::getSuccess, success)
                .ge(startTime != null, AiCallLog::getCreateTime, startTime)
                .le(endTime != null, AiCallLog::getCreateTime, endTime)
                .orderByDesc(AiCallLog::getCreateTime);
    }

    private String csv(Object... values) {
        StringJoiner joiner = new StringJoiner(",");
        for (Object value : values) {
            String text = value == null ? "" : value.toString();
            joiner.add("\"" + text.replace("\"", "\"\"") + "\"");
        }
        return joiner.toString();
    }
}
