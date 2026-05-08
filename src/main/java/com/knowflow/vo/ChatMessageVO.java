package com.knowflow.vo;

import com.knowflow.enums.MessageRole;

import java.time.LocalDateTime;
import java.util.List;

public record ChatMessageVO(Long id, MessageRole role, String content, String modelName, LocalDateTime createTime, List<ReferenceVO> references) {
}
