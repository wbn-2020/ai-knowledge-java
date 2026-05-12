package com.knowflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowflow.common.BaseEntity;

@TableName("chat_session")
public class ChatSession extends BaseEntity {
    private Long userId;
    private Long knowledgeBaseId;
    private String scopeType;
    private Long documentId;
    private String title;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
