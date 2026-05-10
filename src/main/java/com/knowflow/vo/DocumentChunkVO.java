package com.knowflow.vo;

import com.knowflow.entity.DocumentChunk;
import java.time.LocalDateTime;

public record DocumentChunkVO(
        Long chunkId,
        Integer chunkIndex,
        String content,
        Integer tokenCount,
        String vectorId,
        LocalDateTime createdAt
) {
    public static DocumentChunkVO from(DocumentChunk chunk) {
        return new DocumentChunkVO(
                chunk.getId(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                chunk.getTokenCount(),
                chunk.getId() == null ? null : String.valueOf(chunk.getId()),
                chunk.getCreateTime()
        );
    }
}
