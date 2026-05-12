package com.knowflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatFeedbackRequest(
        @NotBlank String feedbackType,
        String reason,
        @Size(max = 500) String remark
) {
}
