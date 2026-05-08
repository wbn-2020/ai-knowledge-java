package com.knowflow.modules.document;

import com.knowflow.common.enums.TaskStatus;

import java.time.LocalDateTime;

public record DocumentTaskVO(Long id, Long userId, Long knowledgeBaseId, Long documentId, TaskStatus status, String failReason, LocalDateTime createTime) {
    public static DocumentTaskVO from(DocumentProcessTask task) {
        return new DocumentTaskVO(task.getId(), task.getUserId(), task.getKnowledgeBaseId(), task.getDocumentId(), task.getStatus(), task.getFailReason(), task.getCreateTime());
    }
}
