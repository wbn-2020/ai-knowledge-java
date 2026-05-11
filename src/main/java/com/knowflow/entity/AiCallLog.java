package com.knowflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.knowflow.common.BaseEntity;

@TableName("ai_call_log")
public class AiCallLog extends BaseEntity {
    private Long userId;
    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;
    @TableField("session_id")
    private Long sessionId;
    private String username;
    private String question;
    @TableField("retrieve_count")
    private Integer retrieveCount;
    @TableField("effective_retrieve_count")
    private Integer effectiveRetrieveCount;
    private Integer topK;
    @TableField("similarity_threshold")
    private Double similarityThreshold;
    @TableField("max_similarity_score")
    private Double maxSimilarityScore;
    @TableField("llm_called")
    private Boolean llmCalled;
    private String model;
    private String modelName;
    private String modelType;
    private String provider;
    private String callType;
    @TableField("prompt_tokens")
    private Integer promptTokens;
    @TableField("completion_tokens")
    private Integer completionTokens;
    @TableField("total_tokens")
    private Integer totalTokens;
    private Long elapsedMs;
    private Boolean success;
    private String failReason;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public Integer getRetrieveCount() { return retrieveCount; }
    public void setRetrieveCount(Integer retrieveCount) { this.retrieveCount = retrieveCount; }
    public Integer getEffectiveRetrieveCount() { return effectiveRetrieveCount; }
    public void setEffectiveRetrieveCount(Integer effectiveRetrieveCount) { this.effectiveRetrieveCount = effectiveRetrieveCount; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
    public Double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(Double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
    public Double getMaxSimilarityScore() { return maxSimilarityScore; }
    public void setMaxSimilarityScore(Double maxSimilarityScore) { this.maxSimilarityScore = maxSimilarityScore; }
    public Boolean getLlmCalled() { return llmCalled; }
    public void setLlmCalled(Boolean llmCalled) { this.llmCalled = llmCalled; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getCallType() { return callType; }
    public void setCallType(String callType) { this.callType = callType; }
    public Integer getPromptTokens() { return promptTokens; }
    public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
    public Integer getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
    public Long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(Long elapsedMs) { this.elapsedMs = elapsedMs; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
}
