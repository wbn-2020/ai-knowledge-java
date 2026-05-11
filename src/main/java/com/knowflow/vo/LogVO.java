package com.knowflow.vo;

import com.knowflow.entity.AiCallLog;
import com.knowflow.entity.LoginLog;
import com.knowflow.entity.OperationLog;
import java.time.LocalDateTime;

public record LogVO(
        Long id,
        Long userId,
        Long knowledgeBaseId,
        Long sessionId,
        String username,
        String question,
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
        String scene,
        String answerType,
        Boolean llmCalled,
        Integer retrieveCount,
        Integer effectiveRetrieveCount,
        Integer topK,
        Double similarityThreshold,
        Double maxSimilarityScore,
        String failReason,
        String noAnswerReason,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        Long durationMs,
        Long costMs,
        LocalDateTime createTime
) {
    public static LogVO from(OperationLog log) {
        return new LogVO(
                log.getId(),
                log.getUserId(),
                null,
                null,
                null,
                null,
                "OPERATION",
                log.getModule(),
                log.getAction(),
                log.getPath(),
                log.getResult(),
                log.getFailureReason(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                log.getCreateTime()
        );
    }

    public static LogVO from(LoginLog log) {
        return new LogVO(
                log.getId(),
                log.getUserId(),
                null,
                null,
                null,
                null,
                "LOGIN",
                "AUTH",
                "LOGIN",
                "/auth/login",
                Boolean.TRUE.equals(log.getSuccess()) ? "SUCCESS" : "FAILED",
                log.getFailureReason(),
                log.getAccount(),
                log.getIp(),
                log.getUserAgent(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                log.getCreateTime()
        );
    }

    public static LogVO from(AiCallLog log) {
        String answerType = resolveAnswerType(log);
        return new LogVO(
                log.getId(), log.getUserId(), log.getKnowledgeBaseId(), log.getSessionId(), log.getUsername(), log.getQuestion(), "AI_CALL",
                "AI", log.getCallType(), "",
                Boolean.TRUE.equals(log.getSuccess()) ? "SUCCESS" : "FAILED", log.getFailReason(),
                null, null, null,
                log.getModel(), log.getModelName(), log.getModelType(), log.getProvider(), log.getCallType(),
                answerType, resolveLlmCalled(log), log.getRetrieveCount(), log.getEffectiveRetrieveCount(),
                log.getTopK(), log.getSimilarityThreshold(), log.getMaxSimilarityScore(),
                log.getFailReason(), resolveNoAnswerReason(answerType, log.getFailReason()),
                log.getPromptTokens(), log.getCompletionTokens(), log.getTotalTokens(),
                log.getElapsedMs(), log.getElapsedMs(),
                log.getCreateTime()
        );
    }

    private static String resolveAnswerType(AiCallLog log) {
        String scene = log.getCallType();
        if (scene == null) {
            return null;
        }
        if (scene.contains("CHAT_QA_GENERAL")) {
            return "GENERAL";
        }
        if (scene.contains("CHAT_QA")) {
            if (!Boolean.TRUE.equals(log.getLlmCalled())) {
                return "NO_CONTEXT";
            }
            return "RAG";
        }
        return null;
    }

    private static Boolean resolveLlmCalled(AiCallLog log) {
        if (log.getLlmCalled() != null) {
            return log.getLlmCalled();
        }
        String scene = log.getCallType();
        if (scene == null) {
            return null;
        }
        if (scene.startsWith("CHAT")) {
            return true;
        }
        if (scene.contains("EMBEDDING") || scene.contains("RETRIEVAL") || scene.contains("SEARCH")) {
            return false;
        }
        return null;
    }

    private static String resolveNoAnswerReason(String answerType, String failReason) {
        if ("NO_CONTEXT".equals(answerType) && "NO_RELEVANT_CONTEXT".equals(failReason)) {
            return "NO_RELEVANT_CONTEXT";
        }
        return null;
    }
}
