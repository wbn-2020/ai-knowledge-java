package com.knowflow.modules.chat;

import com.knowflow.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_message_reference")
public class ChatMessageReference extends BaseEntity {
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Long messageId;
    @Column(nullable = false)
    private Long documentId;
    @Column(nullable = false)
    private Long chunkId;
    @Column(nullable = false, length = 255)
    private String documentName;
    @Lob
    @Column(nullable = false)
    private String content;
    @Column(nullable = false)
    private Double score;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Long getChunkId() { return chunkId; }
    public void setChunkId(Long chunkId) { this.chunkId = chunkId; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}
