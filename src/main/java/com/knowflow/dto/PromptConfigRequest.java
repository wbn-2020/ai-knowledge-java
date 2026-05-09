package com.knowflow.dto;

import jakarta.validation.constraints.NotBlank;

public record PromptConfigRequest(
        @NotBlank String name,
        @NotBlank String type,
        @NotBlank String content,
        Boolean enabled
) {
}
