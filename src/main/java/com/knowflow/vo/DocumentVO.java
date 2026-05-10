package com.knowflow.vo;

import com.knowflow.entity.Document;
import com.knowflow.enums.DocumentParseStatus;
import com.knowflow.enums.EmbeddingStatus;
import java.time.LocalDateTime;

public record DocumentVO(
        Long id,
        Long userId,
        Long uploaderId,
        String uploaderName,
        Long knowledgeBaseId,
        String knowledgeBaseName,
        String ownerName,
        String name,
        String originalName,
        String fileType,
        Long fileSize,
        Long chunkCount,
        DocumentParseStatus parseStatus,
        EmbeddingStatus embeddingStatus,
        EmbeddingStatus vectorStatus,
        String errorMessage,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DocumentVO from(Document document) {
        return new DocumentVO(
                document.getId(),
                document.getUserId(),
                document.getUserId(),
                null,
                document.getKnowledgeBaseId(),
                null,
                null,
                document.getName(),
                document.getOriginalName(),
                document.getFileType(),
                document.getFileSize(),
                null,
                document.getParseStatus(),
                document.getEmbeddingStatus(),
                document.getEmbeddingStatus(),
                document.getErrorMessage(),
                document.getCreateTime(),
                document.getUpdateTime(),
                document.getCreateTime(),
                document.getUpdateTime()
        );
    }

    public static DocumentVO enrich(DocumentVO base, String knowledgeBaseName, String uploaderName, Long chunkCount) {
        return new DocumentVO(
                base.id(),
                base.userId(),
                base.uploaderId(),
                uploaderName,
                base.knowledgeBaseId(),
                knowledgeBaseName,
                uploaderName,
                base.name(),
                base.originalName(),
                base.fileType(),
                base.fileSize(),
                chunkCount,
                base.parseStatus(),
                base.embeddingStatus(),
                base.embeddingStatus(),
                base.errorMessage(),
                base.createTime(),
                base.updateTime(),
                base.createdAt(),
                base.updatedAt()
        );
    }
}
