package com.knowflow.vo;

public record SearchResultVO(Long documentId, Long chunkId, String documentName, String content, double score) {
}
