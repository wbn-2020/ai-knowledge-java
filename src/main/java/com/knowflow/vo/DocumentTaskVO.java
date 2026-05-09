package com.knowflow.vo;

import com.knowflow.enums.TaskStatus;
import java.time.LocalDateTime;

public record DocumentTaskVO(
        Long id,
        String taskType,
        TaskStatus status,
        Long documentId,
        String documentName,
        String failureReason,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
