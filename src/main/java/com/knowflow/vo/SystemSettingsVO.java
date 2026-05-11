package com.knowflow.vo;

import java.util.List;

public record SystemSettingsVO(
        Integer maxFileSize,
        List<String> allowedTypes,
        Integer chunkSize,
        Integer chunkOverlap,
        Integer topK,
        Double similarityThreshold,
        Integer contextMaxLength,
        String platformName,
        String adminEmail
) {
}
