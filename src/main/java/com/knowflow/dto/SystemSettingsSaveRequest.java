package com.knowflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SystemSettingsSaveRequest(
        @NotNull @Min(1) Integer maxFileSize,
        @NotNull List<String> allowedTypes,
        @NotNull @Min(1) Integer chunkSize,
        @NotNull @Min(0) Integer chunkOverlap,
        @NotNull @Min(1) Integer topK,
        @NotNull @DecimalMin("0.0") Double similarityThreshold,
        @Min(200) Integer contextMaxLength,
        @NotNull String platformName,
        String adminEmail
) {
}
