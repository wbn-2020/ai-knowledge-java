package com.knowflow.vo;

import com.knowflow.entity.AiCallLog;
import com.knowflow.entity.LoginLog;
import com.knowflow.entity.OperationLog;
import java.time.LocalDateTime;

public record LogVO(
        Long id,
        Long userId,
        String type,
        String module,
        String action,
        String path,
        String result,
        String failureReason,
        String account,
        String ip,
        String device,
        String model,
        String modelName,
        String modelType,
        String provider,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        Long costMs,
        LocalDateTime createTime
) {
    public static LogVO from(OperationLog log) {
        return new LogVO(
                log.getId(), log.getUserId(), "OPERATION",
                log.getModule(), log.getAction(), log.getPath(),
                log.getResult(), log.getFailureReason(),
                null, null, null,
                null, null, null, null,
                null, null, null, null,
                log.getCreateTime()
        );
    }

    public static LogVO from(LoginLog log) {
        return new LogVO(
                log.getId(), log.getUserId(), "LOGIN",
                "AUTH", "LOGIN", "/auth/login",
                Boolean.TRUE.equals(log.getSuccess()) ? "SUCCESS" : "FAILED", log.getFailureReason(),
                log.getAccount(), log.getIp(), log.getUserAgent(),
                null, null, null, null,
                null, null, null, null,
                log.getCreateTime()
        );
    }

    public static LogVO from(AiCallLog log) {
        return new LogVO(
                log.getId(), log.getUserId(), "AI_CALL",
                "AI", log.getCallType(), "",
                Boolean.TRUE.equals(log.getSuccess()) ? "SUCCESS" : "FAILED", log.getFailReason(),
                null, null, null,
                log.getModel(), log.getModelName(), log.getModelType(), log.getProvider(),
                log.getPromptTokens(), log.getCompletionTokens(), log.getTotalTokens(), log.getElapsedMs(),
                log.getCreateTime()
        );
    }
}
