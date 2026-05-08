package com.knowflow.modules.search;

public record SearchResultVO(Long documentId, Long chunkId, String documentName, String content, double score) {
}
