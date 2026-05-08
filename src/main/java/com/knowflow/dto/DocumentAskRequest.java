package com.knowflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DocumentAskRequest(
        @NotNull Long documentId,
        Long sessionId,
        @NotBlank @Size(max = 2000) String question
) {
}
