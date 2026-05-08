package com.knowflow.modules.chat;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowflow.common.BaseEntity;
import com.knowflow.common.enums.MessageRole;

@TableName("chat_message")
public class ChatMessage extends BaseEntity {
    private Long userId;
    private Long sessionId;
    private MessageRole role;
    private String content;
    private String modelName;
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
