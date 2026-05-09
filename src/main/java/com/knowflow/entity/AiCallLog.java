package com.knowflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.knowflow.common.BaseEntity;

@TableName("ai_call_log")
public class AiCallLog extends BaseEntity {
    private Long userId;
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
