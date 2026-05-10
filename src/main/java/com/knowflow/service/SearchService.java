package com.knowflow.service;

import com.knowflow.security.SecurityUtils;
import com.knowflow.vo.SearchResultVO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SearchService {
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
                runtimeConfigService.doubleValue("rag.minScore", 0.8));
        RetrievalService.RetrievalResult result = retrievalService.retrieve(
                knowledgeBaseId, userId, query, topK, "semantic", threshold);
        return toSearchResult(result.chunks());
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
                        chunk.vectorScore(),
                        chunk.keywordScore(),
                        chunk.finalScore(),
                        chunk.finalScore(),
                        chunk.hitReason()
                ))
                .toList();
    }
}
