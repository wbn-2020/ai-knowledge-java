package com.knowflow.dto;

import jakarta.validation.constraints.NotBlank;

public record PromptTemplateRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String content,
        @NotBlank String scene,
        Boolean enabled,
        Boolean defaultTemplate,
        String description
) {
}
