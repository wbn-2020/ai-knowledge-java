package com.knowflow.vo;

import com.knowflow.enums.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;

public record DocumentTaskVO(
        Long taskId,
        String taskType,
        Long documentId,
        String documentName,
        Boolean documentDeleted,
        TaskStatus status,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        Long durationMs,
        String failureReason,
        List<String> logs
) {
}
