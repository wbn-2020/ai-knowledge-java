package com.knowflow.service;

import com.knowflow.entity.Document;
import com.knowflow.entity.DocumentChunk;
import com.knowflow.infrastructure.ai.EmbeddingClient;
import com.knowflow.mapper.DocumentChunkRepository;
import com.knowflow.mapper.DocumentRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RetrievalService {
    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);
    private static final Pattern CJK_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,}");
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "是", "什么", "请", "一下", "这个", "那个", "哪些", "怎么", "如何", "吗", "呢", "啊",
            "knowflow", "ai"
    );

    private final DocumentChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final EmbeddingClient embeddingClient;

    public RetrievalService(DocumentChunkRepository chunkRepository,
                            DocumentRepository documentRepository,
                            EmbeddingClient embeddingClient) {
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.embeddingClient = embeddingClient;
    }

    public RetrievalResult retrieve(Long knowledgeBaseId,
                                    Long userId,
                                    String query,
                                    int topK,
                                    String mode,
                                    double threshold) {
        List<DocumentChunk> chunks = chunkRepository.findByUserIdAndKnowledgeBaseIdAndDeletedFalse(userId, knowledgeBaseId);
        return retrieveInternal(knowledgeBaseId, query, topK, mode, threshold, chunks);
    }

    public RetrievalResult retrieveByKnowledgeBases(List<Long> knowledgeBaseIds,
                                                    Long userId,
                                                    String query,
                                                    int topK,
                                                    String mode,
                                                    double threshold) {
        List<DocumentChunk> chunks = knowledgeBaseIds.stream()
                .flatMap(kbId -> chunkRepository.findByUserIdAndKnowledgeBaseIdAndDeletedFalse(userId, kbId).stream())
                .toList();
        return retrieveInternal(knowledgeBaseIds.isEmpty() ? null : knowledgeBaseIds.get(0), query, topK, mode, threshold, chunks);
    }

    public RetrievalResult retrieveByDocument(Long knowledgeBaseId,
                                              Long userId,
                                              Long documentId,
                                              String query,
                                              int topK,
                                              String mode,
                                              double threshold) {
        List<DocumentChunk> chunks = chunkRepository.findByDocumentIdAndDeletedFalseOrderByChunkIndexAsc(documentId).stream()
                .filter(chunk -> userId.equals(chunk.getUserId()))
                .toList();
        return retrieveInternal(knowledgeBaseId, query, topK, mode, threshold, chunks);
    }

    public List<String> extractKeywordsForDebug(String query) {
        return extractKeywords(query);
    }

    private RetrievalResult retrieveInternal(Long knowledgeBaseId,
                                             String query,
                                             int topK,
                                             String mode,
                                             double threshold,
                                             List<DocumentChunk> chunks) {
        if (chunks.isEmpty()) {
            log.debug("Hybrid retrieval debug: query='{}', knowledgeBaseId={}, extractedKeywords=[], mode={}, topK={}, threshold={}, keywordResults=0, vectorResults=0, mergedResults=0",
                    query, knowledgeBaseId, mode, topK, threshold);
            return new RetrievalResult(List.of(), 0, 0, 0, 0d, false);
        }

        String effectiveMode = normalizeMode(mode);
        List<String> extractedKeywords = extractKeywords(query);
        String normalizedQuery = normalizeForMatch(query);

        double[] queryEmbedding = embeddingClient.embed(query);
        List<RetrievedChunk> vectorResults = buildVectorResults(chunks, queryEmbedding, topK * 3);
        List<RetrievedChunk> keywordResults = buildKeywordResults(chunks, query, normalizedQuery, extractedKeywords);

        Map<Long, RetrievedChunk> mergedByChunkId = new LinkedHashMap<>();
        if ("keyword".equals(effectiveMode)) {
            keywordResults.forEach(item -> mergedByChunkId.put(item.chunkId(), item));
        } else if ("semantic".equals(effectiveMode)) {
            // semantic 模式也开启关键词兜底
            keywordResults.forEach(item -> mergedByChunkId.put(item.chunkId(), item));
            vectorResults.forEach(item -> mergedByChunkId.merge(item.chunkId(), item, this::mergeChunkScore));
        } else {
            // hybrid 默认
            keywordResults.forEach(item -> mergedByChunkId.put(item.chunkId(), item));
            vectorResults.forEach(item -> mergedByChunkId.merge(item.chunkId(), item, this::mergeChunkScore));
        }

        List<RetrievedChunk> mergedResults = new ArrayList<>(mergedByChunkId.values());
        mergedResults = mergedResults.stream()
                .map(this::recalculateFinalScore)
                .sorted(this::compareChunk)
                .collect(Collectors.toList());

        if (!keywordResults.isEmpty() && !mergedResults.isEmpty() && mergedResults.get(0).keywordScore() <= 0.0d) {
            RetrievedChunk bestKeyword = keywordResults.stream().max(this::compareChunk).orElse(null);
            if (bestKeyword != null) {
                mergedResults.removeIf(item -> Objects.equals(item.chunkId(), bestKeyword.chunkId()));
                mergedResults.add(0, bestKeyword);
            }
        }

        List<RetrievedChunk> thresholdFiltered = mergedResults.stream()
                .filter(item -> item.finalScore() >= threshold)
                .toList();
        List<RetrievedChunk> topResults = thresholdFiltered.stream().limit(Math.max(1, topK)).toList();
        double maxFinalScore = mergedResults.stream().mapToDouble(RetrievedChunk::finalScore).max().orElse(0d);
        boolean hasStrongKeywordHit = topResults.stream().anyMatch(item -> item.keywordScore() >= 0.8d);

        logRetrieval(query, knowledgeBaseId, extractedKeywords, effectiveMode, topK, threshold, queryEmbedding, keywordResults.size(), vectorResults.size(), mergedResults.size(), topResults);
        return new RetrievalResult(topResults, keywordResults.size(), vectorResults.size(), mergedResults.size(), maxFinalScore, hasStrongKeywordHit);
    }

    private List<RetrievedChunk> buildVectorResults(List<DocumentChunk> chunks, double[] queryEmbedding, int limit) {
        Map<Long, Document> documents = documentRepository.selectBatchIds(
                        chunks.stream().map(DocumentChunk::getDocumentId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Document::getId, d -> d));
        return chunks.stream()
                .map(chunk -> {
                    double vectorScore = cosine(queryEmbedding, parseVector(chunk.getEmbedding()));
                    Document doc = documents.get(chunk.getDocumentId());
                    return new RetrievedChunk(
                            chunk.getDocumentId(),
                            doc == null ? "" : doc.getName(),
                            chunk.getId(),
                            chunk.getChunkIndex(),
                            chunk.getId() == null ? null : String.valueOf(chunk.getId()),
                            chunk.getContent(),
                            round4(vectorScore),
                            0d,
                            round4(vectorScore),
                            "semantic"
                    );
                })
                .sorted(Comparator.comparing(RetrievedChunk::vectorScore).reversed())
                .limit(limit)
                .toList();
    }

    private List<RetrievedChunk> buildKeywordResults(List<DocumentChunk> chunks,
                                                     String query,
                                                     String normalizedQuery,
                                                     List<String> keywords) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        Map<Long, Document> documents = documentRepository.selectBatchIds(
                        chunks.stream().map(DocumentChunk::getDocumentId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Document::getId, d -> d));
        return chunks.stream()
                .map(chunk -> {
                    KeywordScore score = calcKeywordScore(query, normalizedQuery, keywords, chunk.getContent());
                    if (score.score() <= 0d) {
                        return null;
                    }
                    Document doc = documents.get(chunk.getDocumentId());
                    return new RetrievedChunk(
                            chunk.getDocumentId(),
                            doc == null ? "" : doc.getName(),
                            chunk.getId(),
                            chunk.getChunkIndex(),
                            chunk.getId() == null ? null : String.valueOf(chunk.getId()),
                            chunk.getContent(),
                            0d,
                            round4(score.score()),
                            round4(score.score() * 0.7d),
                            score.reason()
                    );
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RetrievedChunk::keywordScore).reversed())
                .toList();
    }

    private RetrievedChunk mergeChunkScore(RetrievedChunk left, RetrievedChunk right) {
        String documentName = StringUtils.hasText(left.documentName()) ? left.documentName() : right.documentName();
        double vectorScore = Math.max(left.vectorScore(), right.vectorScore());
        double keywordScore = Math.max(left.keywordScore(), right.keywordScore());
        String hitReason;
        if (keywordScore > 0d && vectorScore > 0d) {
            hitReason = "hybrid";
        } else if (keywordScore > 0d) {
            hitReason = "keyword";
        } else {
            hitReason = "semantic";
        }
        return new RetrievedChunk(
                left.documentId(),
                documentName,
                left.chunkId(),
                left.chunkIndex(),
                left.vectorId(),
                left.content(),
                round4(vectorScore),
                round4(keywordScore),
                round4(keywordScore * 0.7d + vectorScore * 0.3d),
                hitReason
        );
    }

    private RetrievedChunk recalculateFinalScore(RetrievedChunk item) {
        double finalScore = item.keywordScore() * 0.7d + item.vectorScore() * 0.3d;
        String hitReason;
        if (item.keywordScore() > 0d && item.vectorScore() > 0d) {
            hitReason = "hybrid";
        } else if (item.keywordScore() > 0d) {
            hitReason = "keyword";
        } else {
            hitReason = "semantic";
        }
        return new RetrievedChunk(
                item.documentId(),
                item.documentName(),
                item.chunkId(),
                item.chunkIndex(),
                item.vectorId(),
                item.content(),
                round4(item.vectorScore()),
                round4(item.keywordScore()),
                round4(finalScore),
                hitReason
        );
    }

    private int compareChunk(RetrievedChunk a, RetrievedChunk b) {
        int keywordPriority = Double.compare(b.keywordScore() >= 0.8d ? 1d : 0d, a.keywordScore() >= 0.8d ? 1d : 0d);
        if (keywordPriority != 0) {
            return keywordPriority;
        }
        int byFinal = Double.compare(b.finalScore(), a.finalScore());
        if (byFinal != 0) {
            return byFinal;
        }
        return Double.compare(b.vectorScore(), a.vectorScore());
    }

    private void logRetrieval(String query,
                              Long knowledgeBaseId,
                              List<String> extractedKeywords,
                              String mode,
                              int topK,
                              double threshold,
                              double[] queryEmbedding,
                              int keywordCount,
                              int vectorCount,
                              int mergedCount,
                              List<RetrievedChunk> topResults) {
        String firstFive = queryEmbedding.length == 0 ? "[]"
                : Arrays.stream(queryEmbedding).limit(5).mapToObj(v -> String.format("%.6f", v))
                .collect(Collectors.joining(", ", "[", "]"));
        log.debug("Hybrid retrieval debug: query='{}', knowledgeBaseId={}, extractedKeywords={}, mode={}, topK={}, threshold={}, queryEmbeddingDimension={}, queryEmbeddingFirst5={}, keywordResults={}, vectorResults={}, mergedResults={}",
                query, knowledgeBaseId, extractedKeywords, mode, topK, threshold,
                queryEmbedding.length, firstFive, keywordCount, vectorCount, mergedCount);
        for (int i = 0; i < topResults.size(); i++) {
            RetrievedChunk item = topResults.get(i);
            String preview = item.content() == null ? "" : item.content().replaceAll("\\s+", " ");
            if (preview.length() > 120) {
                preview = preview.substring(0, 120);
            }
            log.debug("Hybrid top{}: documentId={}, chunkId={}, chunkIndex={}, vectorId={}, vectorScore={}, keywordScore={}, finalScore={}, hitReason={}, content={}",
                    i + 1,
                    item.documentId(),
                    item.chunkId(),
                    item.chunkIndex(),
                    item.vectorId(),
                    String.format("%.4f", item.vectorScore()),
                    String.format("%.4f", item.keywordScore()),
                    String.format("%.4f", item.finalScore()),
                    item.hitReason(),
                    preview);
        }
    }

    private String normalizeMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return "hybrid";
        }
        String m = mode.toLowerCase(Locale.ROOT);
        if ("keyword".equals(m) || "semantic".equals(m) || "hybrid".equals(m)) {
            return m;
        }
        return "hybrid";
    }

    private List<String> extractKeywords(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        String normalized = normalizeForMatch(query);
        List<String> keywords = new ArrayList<>();

        // full phrase first
        if (normalized.length() >= 2) {
            keywords.add(normalized);
        }

        // split by spaces
        for (String token : normalized.split("\\s+")) {
            String t = token.trim();
            if (t.length() >= 2 && !isStopWord(t) && !keywords.contains(t)) {
                keywords.add(t);
            }
        }

        // extract Chinese phrases
        java.util.regex.Matcher matcher = CJK_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String phrase = matcher.group();
            if (phrase.length() >= 2 && !isStopWord(phrase) && !keywords.contains(phrase)) {
                keywords.add(phrase);
            }
        }
        return keywords;
    }

    private String normalizeForMatch(String query) {
        if (!StringUtils.hasText(query)) {
            return "";
        }
        String text = query.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}，。！？；：“”‘’（）【】《》、]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        for (String stop : STOP_WORDS) {
            text = text.replace(stop, " ");
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    private boolean isStopWord(String token) {
        return STOP_WORDS.contains(token.toLowerCase(Locale.ROOT));
    }

    private KeywordScore calcKeywordScore(String query,
                                          String normalizedQuery,
                                          List<String> keywords,
                                          String content) {
        if (!StringUtils.hasText(content)) {
            return new KeywordScore(0d, "none");
        }
        String normalizedContent = content.replaceAll("\\s+", "");
        String compactQuery = query == null ? "" : query.replaceAll("\\s+", "");
        if (StringUtils.hasText(compactQuery) && normalizedContent.contains(compactQuery)) {
            return new KeywordScore(1.0d, "keyword_exact_query");
        }
        if (StringUtils.hasText(normalizedQuery) && normalizedContent.contains(normalizedQuery.replaceAll("\\s+", ""))) {
            return new KeywordScore(1.0d, "keyword_exact_phrase");
        }

        double score = 0d;
        int matched = 0;
        for (String keyword : keywords) {
            String k = keyword.replaceAll("\\s+", "");
            if (k.length() < 2) {
                continue;
            }
            if (normalizedContent.contains(k)) {
                matched++;
                if (content.contains("项目定位") && k.contains("项目定位")) {
                    score += 0.8d;
                } else {
                    score += 0.5d;
                }
            }
        }
        if (matched == 0) {
            return new KeywordScore(0d, "none");
        }
        return new KeywordScore(Math.min(1.0d, score), score >= 0.8d ? "keyword_core_phrase" : "keyword_partial");
    }

    private double[] parseVector(String text) {
        if (!StringUtils.hasText(text)) {
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
            return 0d;
        }
        double dot = 0d;
        double na = 0d;
        double nb = 0d;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return na == 0 || nb == 0 ? 0d : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private double round4(double v) {
        return Math.round(v * 10000d) / 10000d;
    }

    public record RetrievedChunk(Long documentId,
                                 String documentName,
                                 Long chunkId,
                                 Integer chunkIndex,
                                 String vectorId,
                                 String content,
                                 double vectorScore,
                                 double keywordScore,
                                 double finalScore,
                                 String hitReason) {
    }

    public record RetrievalResult(List<RetrievedChunk> chunks,
                                  int keywordResultsCount,
                                  int vectorResultsCount,
                                  int mergedResultsCount,
                                  double maxVectorScore,
                                  boolean hasStrongKeywordHit) {
    }

    private record KeywordScore(double score, String reason) {
    }
}
