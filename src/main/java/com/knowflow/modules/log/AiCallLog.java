package com.knowflow.modules.log;

import com.knowflow.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_call_log")
public class AiCallLog extends BaseEntity {
    @Column(nullable = false)
    private Long userId;
    @Column(length = 128)
    private String modelName;
    @Column(length = 64)
    private String callType;
    private Long elapsedMs;
    @Column(nullable = false)
    private Boolean success;
    @Column(length = 1024)
    private String failReason;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getCallType() { return callType; }
    public void setCallType(String callType) { this.callType = callType; }
    public Long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(Long elapsedMs) { this.elapsedMs = elapsedMs; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
}
