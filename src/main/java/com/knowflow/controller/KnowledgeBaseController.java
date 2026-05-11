package com.knowflow.controller;

import com.knowflow.common.ApiResponse;
import com.knowflow.common.PageResponse;
import com.knowflow.dto.KnowledgeBaseRequest;
import com.knowflow.service.KnowledgeBaseService;
import com.knowflow.service.SummaryService;
import com.knowflow.vo.KeywordVO;
import com.knowflow.vo.KnowledgeBaseDetailVO;
import com.knowflow.vo.KnowledgeBaseSummaryVO;
import com.knowflow.vo.KnowledgeBaseVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/knowledge-bases")
public class KnowledgeBaseController {
    private final KnowledgeBaseService service;
    private final SummaryService summaryService;

    public KnowledgeBaseController(KnowledgeBaseService service, SummaryService summaryService) {
        this.service = service;
        this.summaryService = summaryService;
    }

    @PostMapping
    public ApiResponse<KnowledgeBaseVO> create(@Valid @RequestBody KnowledgeBaseRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<KnowledgeBaseVO>> page(@RequestParam(defaultValue = "") String keyword,
                                                           @RequestParam(defaultValue = "1") int pageNo,
                                                           @RequestParam(defaultValue = "10") int pageSize,
                                                           @RequestParam(defaultValue = "updateTime") String sortBy) {
        return ApiResponse.ok(service.page(keyword, pageNo, pageSize, sortBy));
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBaseVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.detail(id));
    }

    @GetMapping("/{id}/detail")
    public ApiResponse<KnowledgeBaseDetailVO> detailFull(@PathVariable Long id) {
        return ApiResponse.ok(service.detailFull(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeBaseVO> update(@PathVariable Long id, @Valid @RequestBody KnowledgeBaseRequest request) {
        return ApiResponse.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/summary")
    public ApiResponse<KnowledgeBaseSummaryVO> summary(@PathVariable Long id) {
        return ApiResponse.ok(summaryService.getKnowledgeBaseSummaryV13(id));
    }

    @PostMapping("/{id}/summary/generate")
    public ApiResponse<KnowledgeBaseSummaryVO> generateSummary(@PathVariable Long id) {
        return ApiResponse.ok(summaryService.generateKnowledgeBaseSummary(id));
    }

    @PostMapping("/{id}/summary/regenerate")
    public ApiResponse<KnowledgeBaseSummaryVO> regenerateSummary(@PathVariable Long id) {
        return ApiResponse.ok(summaryService.regenerateKnowledgeBaseSummary(id));
    }

    @GetMapping("/{id}/keywords")
    public ApiResponse<java.util.List<KeywordVO>> keywords(@PathVariable Long id) {
        return ApiResponse.ok(summaryService.getKnowledgeBaseKeywords(id));
    }

    @PostMapping("/{id}/keywords/extract")
    public ApiResponse<java.util.List<KeywordVO>> extractKeywords(@PathVariable Long id) {
        return ApiResponse.ok(summaryService.extractKnowledgeBaseKeywords(id));
    }

    @PostMapping("/{id}/keywords/reextract")
    public ApiResponse<java.util.List<KeywordVO>> reextractKeywords(@PathVariable Long id) {
        return ApiResponse.ok(summaryService.reextractKnowledgeBaseKeywords(id));
    }
}
