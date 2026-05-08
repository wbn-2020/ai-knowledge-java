package com.knowflow.modules.chat;

import com.knowflow.common.BaseEntity;
import com.knowflow.common.enums.MessageRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_message")
public class ChatMessage extends BaseEntity {
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Long sessionId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MessageRole role;
    @Lob
    @Column(nullable = false)
    private String content;
    @Column(length = 128)
    private String modelName;
    @Column(nullable = false)
    private Integer tokenCount = 0;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public MessageRole getRole() { return role; }
    public void setRole(MessageRole role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
}
