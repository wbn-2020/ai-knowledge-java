package com.knowflow.vo;

import java.time.LocalDateTime;

public record SummaryVO(Long documentId, Long knowledgeBaseId, String summary, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
