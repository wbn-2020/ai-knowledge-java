package com.knowflow.modules.log;

import com.knowflow.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "operation_log")
public class OperationLog extends BaseEntity {
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false, length = 64)
    private String action;
    @Column(length = 128)
    private String targetType;
    private Long targetId;
    @Column(length = 1024)
    private String detail;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
