package com.knowflow.modules.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MultiKnowledgeAskRequest(
        @NotEmpty List<Long> knowledgeBaseIds,
        Long sessionId,
        @NotBlank @Size(max = 2000) String question
) {
}
