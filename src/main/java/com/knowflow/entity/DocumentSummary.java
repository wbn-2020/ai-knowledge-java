package com.knowflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowflow.common.BaseEntity;

@TableName("document_summary")
public class DocumentSummary extends BaseEntity {
    @TableField("user_id")
    private Long userId;

    @TableField("document_id")
    private Long documentId;

    private String summary;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
