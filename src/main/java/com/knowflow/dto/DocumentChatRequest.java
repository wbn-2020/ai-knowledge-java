package com.knowflow.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentChatRequest(
        Long sessionId,
        @NotBlank @Size(max = 2000) String question,
        Boolean allowGeneralAnswer,
        Integer topK,
        @DecimalMin("0.0") @DecimalMax("1.0") Double similarityThreshold
) {
}
