package com.knowflow.vo;

public record SearchResultVO(
        Long documentId,
        String documentName,
        Long chunkId,
        Integer chunkIndex,
        String vectorId,
        String content,
        double vectorScore,
        double keywordScore,
        double finalScore,
        double score,
        String hitReason
) {
}
