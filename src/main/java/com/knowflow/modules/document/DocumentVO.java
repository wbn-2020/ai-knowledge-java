package com.knowflow.modules.document;

import com.knowflow.common.enums.DocumentParseStatus;
import com.knowflow.common.enums.EmbeddingStatus;

import java.time.LocalDateTime;

public record DocumentVO(
        Long id,
        Long knowledgeBaseId,
        String name,
        String originalName,
        String fileType,
        Long fileSize,
        DocumentParseStatus parseStatus,
        EmbeddingStatus embeddingStatus,
        String errorMessage,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
    public static DocumentVO from(Document document) {
        return new DocumentVO(
                document.getId(),
                document.getKnowledgeBaseId(),
                document.getName(),
                document.getOriginalName(),
                document.getFileType(),
                document.getFileSize(),
                document.getParseStatus(),
                document.getEmbeddingStatus(),
                document.getErrorMessage(),
                document.getCreateTime(),
                document.getUpdateTime()
        );
    }
}
