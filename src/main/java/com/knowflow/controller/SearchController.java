package com.knowflow.controller;

import com.knowflow.common.ApiResponse;
import com.knowflow.service.SearchService;
import com.knowflow.vo.SearchResultVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/search")
public class SearchController {
    private final SearchService service;

    public SearchController(SearchService service) {
        this.service = service;
    }

    @GetMapping("/semantic")
    public ApiResponse<List<SearchResultVO>> semantic(@RequestParam Long knowledgeBaseId,
                                                      @RequestParam String query,
                                                      @RequestParam(defaultValue = "10") int topK) {
        return ApiResponse.ok(service.semanticSearch(knowledgeBaseId, query, topK));
    }

    @GetMapping("/keyword")
    public ApiResponse<List<SearchResultVO>> keyword(@RequestParam Long knowledgeBaseId,
                                                     @RequestParam String keyword,
                                                     @RequestParam(defaultValue = "10") int topK) {
        return ApiResponse.ok(service.keywordSearch(knowledgeBaseId, keyword, topK));
    }
}
