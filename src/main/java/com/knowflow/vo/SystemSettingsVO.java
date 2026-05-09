package com.knowflow.vo;

import java.util.List;

public record SystemSettingsVO(
        Integer maxFileSize,
        List<String> allowedTypes,
        Integer chunkSize,
        Integer chunkOverlap,
        Integer topK,
        Double similarityThreshold,
        String platformName,
        String adminEmail
) {
}
