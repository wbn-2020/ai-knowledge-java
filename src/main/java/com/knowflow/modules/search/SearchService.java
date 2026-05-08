package com.knowflow.modules.search;

import com.knowflow.infrastructure.ai.EmbeddingClient;
import com.knowflow.modules.document.Document;
import com.knowflow.modules.document.DocumentChunk;
import com.knowflow.modules.document.DocumentChunkRepository;
import com.knowflow.modules.document.DocumentRepository;
import com.knowflow.modules.knowledge.KnowledgeBaseService;
import com.knowflow.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {
    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final EmbeddingClient embeddingClient;

    public SearchService(KnowledgeBaseService knowledgeBaseService, DocumentChunkRepository chunkRepository, DocumentRepository documentRepository, EmbeddingClient embeddingClient) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.embeddingClient = embeddingClient;
    }

    public List<SearchResultVO> semanticSearch(Long knowledgeBaseId, String query, int topK) {
        Long userId = SecurityUtils.getCurrentUserId();
        knowledgeBaseService.requireOwned(knowledgeBaseId);
        double[] queryVector = embeddingClient.embed(query);
        List<DocumentChunk> chunks = chunkRepository.findByUserIdAndKnowledgeBaseIdAndDeletedFalse(userId, knowledgeBaseId);
        Map<Long, Document> documents = documentRepository.selectBatchIds(chunks.stream().map(DocumentChunk::getDocumentId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Document::getId, d -> d));
        return chunks.stream()
                .map(chunk -> new Scored(chunk, cosine(queryVector, parseVector(chunk.getEmbedding()))))
                .sorted(Comparator.comparing(Scored::score).reversed())
                .limit(topK)
                .map(scored -> {
                    Document doc = documents.get(scored.chunk().getDocumentId());
                    return new SearchResultVO(scored.chunk().getDocumentId(), scored.chunk().getId(), doc == null ? "" : doc.getName(), scored.chunk().getContent(), scored.score());
                })
                .toList();
    }

    public List<SearchResultVO> keywordSearch(Long knowledgeBaseId, String keyword, int topK) {
        Long userId = SecurityUtils.getCurrentUserId();
        knowledgeBaseService.requireOwned(knowledgeBaseId);
        List<DocumentChunk> chunks = chunkRepository.findByUserIdAndKnowledgeBaseIdAndDeletedFalse(userId, knowledgeBaseId);
        Map<Long, Document> documents = documentRepository.selectBatchIds(chunks.stream().map(DocumentChunk::getDocumentId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Document::getId, d -> d));
        return chunks.stream()
                .filter(chunk -> chunk.getContent() != null && chunk.getContent().contains(keyword))
                .limit(topK)
                .map(chunk -> {
                    Document doc = documents.get(chunk.getDocumentId());
                    return new SearchResultVO(chunk.getDocumentId(), chunk.getId(), doc == null ? "" : doc.getName(), chunk.getContent(), 1.0);
                })
                .toList();
    }

    private double[] parseVector(String text) {
        if (text == null || text.isBlank()) {
            return new double[0];
        }
        String[] parts = text.split(",");
        double[] vector = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Double.parseDouble(parts[i]);
        }
        return vector;
    }

    private double cosine(double[] a, double[] b) {
        if (a.length == 0 || b.length == 0 || a.length != b.length) {
            return 0;
        }
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return na == 0 || nb == 0 ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private record Scored(DocumentChunk chunk, double score) {
    }
}
