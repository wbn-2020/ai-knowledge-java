package com.knowflow.modules.document;

import com.knowflow.common.BaseEntity;
import com.knowflow.common.enums.DocumentParseStatus;
import com.knowflow.common.enums.EmbeddingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "document")
public class Document extends BaseEntity {
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Long knowledgeBaseId;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(nullable = false, length = 255)
    private String originalName;
    @Column(nullable = false, length = 32)
    private String fileType;
    @Column(nullable = false)
    private Long fileSize;
    @Column(nullable = false, length = 1024)
    private String filePath;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DocumentParseStatus parseStatus = DocumentParseStatus.PENDING;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EmbeddingStatus embeddingStatus = EmbeddingStatus.PENDING;
    @Column(length = 1024)
    private String errorMessage;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public DocumentParseStatus getParseStatus() { return parseStatus; }
    public void setParseStatus(DocumentParseStatus parseStatus) { this.parseStatus = parseStatus; }
    public EmbeddingStatus getEmbeddingStatus() { return embeddingStatus; }
    public void setEmbeddingStatus(EmbeddingStatus embeddingStatus) { this.embeddingStatus = embeddingStatus; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
