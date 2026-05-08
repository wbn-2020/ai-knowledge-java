package com.knowflow.modules.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FeedbackRequest(@NotNull Long messageId, @NotBlank String feedbackType, String reason) {
}
