package com.knowflow.dto;

import jakarta.validation.constraints.NotBlank;

public record AiModelConfigRequest(
        @NotBlank String name,
        @NotBlank String provider,
        @NotBlank String baseUrl,
        String apiKey,
        @NotBlank String modelName,
        Boolean enabled,
        Boolean defaultModel,
        Integer maxTokens,
        Double temperature,
        String description
) {
}
