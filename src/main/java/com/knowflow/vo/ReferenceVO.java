package com.knowflow.vo;

import com.knowflow.entity.ChatMessageReference;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record ReferenceVO(Long documentId,
                          String documentName,
                          Long chunkId,
                          Integer chunkIndex,
                          String vectorId,
                          String content,
                          String snippet,
                          Double score,
                          Double finalScore,
                          String hitReason,
                          Integer rank) {
    public static ReferenceVO from(ChatMessageReference reference, Integer chunkIndex) {
        String content = reference.getContent();
        return new ReferenceVO(
                reference.getDocumentId(),
                reference.getDocumentName(),
                reference.getChunkId(),
                chunkIndex,
                reference.getChunkId() == null ? null : String.valueOf(reference.getChunkId()),
                content,
                snippet(content),
                roundScore(reference.getScore()),
                roundScore(reference.getScore()),
                null,
                null
        );
    }

    public static ReferenceVO fromRetrievedChunk(RetrievalReference reference) {
        return new ReferenceVO(
                reference.documentId(),
                reference.documentName(),
                reference.chunkId(),
                reference.chunkIndex(),
                reference.vectorId(),
                reference.content(),
                snippet(reference.content()),
                roundScore(reference.finalScore()),
                roundScore(reference.finalScore()),
                reference.hitReason(),
                reference.rank()
        );
    }

    private static String snippet(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 180);
    }

    private static Double roundScore(Double score) {
        if (score == null) {
            return null;
        }
        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    public record RetrievalReference(Long documentId,
                                     String documentName,
                                     Long chunkId,
                                     Integer chunkIndex,
                                     String vectorId,
                                     String content,
                                     Double finalScore,
                                     String hitReason,
                                     Integer rank) {
    }
}
