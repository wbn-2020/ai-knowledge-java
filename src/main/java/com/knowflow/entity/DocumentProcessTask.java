package com.knowflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.knowflow.common.BaseEntity;
import com.knowflow.enums.TaskStatus;
import java.time.LocalDateTime;

@TableName("document_process_task")
public class DocumentProcessTask extends BaseEntity {
    private Long userId;
    private Long knowledgeBaseId;
    private Long documentId;
    @TableField("task_type")
    private String taskType;
    private TaskStatus status = TaskStatus.PENDING;
    private String failReason;
    @TableField("document_name_snapshot")
    private String documentNameSnapshot;
    @TableField("logs_json")
    private String logsJson;
    @TableField("started_at")
    private LocalDateTime startedAt;
    @TableField("finished_at")
    private LocalDateTime finishedAt;
    @TableField("duration_ms")
    private Long durationMs;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
    public String getDocumentNameSnapshot() { return documentNameSnapshot; }
    public void setDocumentNameSnapshot(String documentNameSnapshot) { this.documentNameSnapshot = documentNameSnapshot; }
    public String getLogsJson() { return logsJson; }
    public void setLogsJson(String logsJson) { this.logsJson = logsJson; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}
