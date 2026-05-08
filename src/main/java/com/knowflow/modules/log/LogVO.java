package com.knowflow.modules.log;

import java.time.LocalDateTime;

public record LogVO(Long id, Long userId, String type, String action, String detail, Boolean success, Long elapsedMs, LocalDateTime createTime) {
    public static LogVO from(OperationLog log) {
        return new LogVO(log.getId(), log.getUserId(), "OPERATION", log.getAction(), log.getDetail(), true, null, log.getCreateTime());
    }

    public static LogVO from(LoginLog log) {
        return new LogVO(log.getId(), log.getUserId(), "LOGIN", log.getAccount(), log.getMessage(), log.getSuccess(), null, log.getCreateTime());
    }

    public static LogVO from(AiCallLog log) {
        return new LogVO(log.getId(), log.getUserId(), "AI_CALL", log.getCallType(), log.getModelName(), log.getSuccess(), log.getElapsedMs(), log.getCreateTime());
    }
}
