package com.knowflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowflow.common.BaseEntity;

@TableName("knowledge_base_summary")
public class KnowledgeBaseSummary extends BaseEntity {
    @TableField("user_id")
    private Long userId;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    private String summary;
    @TableField("covered_document_count")
    private Integer coveredDocumentCount;
    @TableField("model_name")
    private String modelName;
    private String status;
    @TableField("error_message")
    private String errorMessage;
    @TableField("generated_at")
    private java.time.LocalDateTime generatedAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Integer getCoveredDocumentCount() { return coveredDocumentCount; }
    public void setCoveredDocumentCount(Integer coveredDocumentCount) { this.coveredDocumentCount = coveredDocumentCount; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public java.time.LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(java.time.LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
