package com.knowflow.vo;

import com.knowflow.entity.Document;
import com.knowflow.enums.DocumentParseStatus;
import com.knowflow.enums.EmbeddingStatus;
import java.time.LocalDateTime;

public record DocumentVO(
        Long id,
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
        String errorMessage,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DocumentVO from(Document document) {
        return new DocumentVO(
                document.getId(),
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
                document.getErrorMessage(),
                document.getCreateTime(),
                document.getUpdateTime(),
                document.getCreateTime(),
                document.getUpdateTime()
        );
    }

    public static DocumentVO enrich(DocumentVO base, String knowledgeBaseName, String ownerName, Long chunkCount) {
        return new DocumentVO(
                base.id(),
                base.knowledgeBaseId(),
                knowledgeBaseName,
                ownerName,
                base.name(),
                base.originalName(),
                base.fileType(),
                base.fileSize(),
                chunkCount,
                base.parseStatus(),
                base.embeddingStatus(),
                base.errorMessage(),
                base.createTime(),
                base.updateTime(),
                base.createdAt(),
                base.updatedAt()
        );
    }
}
