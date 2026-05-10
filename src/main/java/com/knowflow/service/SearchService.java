package com.knowflow.service;

import com.knowflow.security.SecurityUtils;
import com.knowflow.vo.SearchResultVO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final KnowledgeBaseService knowledgeBaseService;
    private final RuntimeConfigService runtimeConfigService;
    private final RetrievalService retrievalService;

    public SearchService(KnowledgeBaseService knowledgeBaseService,
                         RuntimeConfigService runtimeConfigService,
                         RetrievalService retrievalService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.runtimeConfigService = runtimeConfigService;
        this.retrievalService = retrievalService;
    }

    public List<SearchResultVO> semanticSearch(Long knowledgeBaseId, String query, int topK) {
        Long userId = SecurityUtils.getCurrentUserId();
        knowledgeBaseService.requireOwned(knowledgeBaseId);
        double threshold = runtimeConfigService.doubleValue("rag.similarityThreshold",
                runtimeConfigService.doubleValue("rag.minScore", 0.65));
        RetrievalService.RetrievalResult result = retrievalService.retrieve(
                knowledgeBaseId, userId, query, topK, "hybrid", threshold);
        List<SearchResultVO> allResults = toSearchResult(result.chunks());
        List<SearchResultVO> validResults = allResults.stream()
                .filter(item -> item.finalScore() >= threshold)
                .sorted((a, b) -> Double.compare(b.finalScore(), a.finalScore()))
                .toList();

        log.debug("Semantic search summary: query='{}', knowledgeBaseId={}, searchMode=hybrid, threshold={}, extractedKeywords={}, keywordResultsCount={}, vectorResultsCount={}, mergedResultsCount={}, validResultsCount={}",
                query, knowledgeBaseId, threshold, retrievalService.extractKeywordsForDebug(query),
                result.keywordResultsCount(), result.vectorResultsCount(), result.mergedResultsCount(), validResults.size());
        validResults.forEach(item -> log.debug(
                "Semantic result: documentId={}, documentName={}, chunkId={}, chunkIndex={}, score={}, finalScore={}, hitReason={}, content={}",
                item.documentId(), item.documentName(), item.chunkId(), item.chunkIndex(),
                String.format("%.4f", item.score()), String.format("%.4f", item.finalScore()),
                item.hitReason(), snippet(item.content(), 100)
        ));
        if (validResults.isEmpty()) {
            log.debug("Semantic search no valid result: query='{}', knowledgeBaseId={}, message=未找到足够相关内容", query, knowledgeBaseId);
        }
        return validResults;
    }

    public List<SearchResultVO> keywordSearch(Long knowledgeBaseId, String keyword, int topK) {
        Long userId = SecurityUtils.getCurrentUserId();
        knowledgeBaseService.requireOwned(knowledgeBaseId);
        RetrievalService.RetrievalResult result = retrievalService.retrieve(
                knowledgeBaseId, userId, keyword, topK, "keyword", 0d);
        return toSearchResult(result.chunks());
    }

    private List<SearchResultVO> toSearchResult(List<RetrievalService.RetrievedChunk> chunks) {
        return chunks.stream()
                .map(chunk -> new SearchResultVO(
                        chunk.documentId(),
                        chunk.documentName(),
                        chunk.chunkId(),
                        chunk.chunkIndex(),
                        chunk.vectorId(),
                        chunk.content(),
                        snippet(chunk.content(), 180),
                        chunk.vectorScore(),
                        chunk.keywordScore(),
                        chunk.finalScore(),
                        chunk.finalScore(),
                        chunk.hitReason()
                ))
                .toList();
    }

    private String snippet(String content, int maxLen) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLen ? normalized : normalized.substring(0, maxLen);
    }
}
