package com.knowflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AskRequest(
        @NotNull Long knowledgeBaseId,
        Long sessionId,
        @NotBlank @Size(max = 2000) String question,
        Boolean allowGeneralAnswer
) {
}
