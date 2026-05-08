package com.knowflow.modules.chat;

public record ReferenceVO(Long documentId, Long chunkId, String documentName, String content, Double score) {
    public static ReferenceVO from(ChatMessageReference reference) {
        return new ReferenceVO(reference.getDocumentId(), reference.getChunkId(), reference.getDocumentName(), reference.getContent(), reference.getScore());
    }
}
