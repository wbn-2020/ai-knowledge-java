package com.knowflow.controller;

import com.knowflow.common.ApiResponse;
import com.knowflow.service.SummaryService;
import com.knowflow.vo.SummaryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/summaries")
public class SummaryController {
    private final SummaryService service;

    public SummaryController(SummaryService service) {
        this.service = service;
    }

    @PostMapping("/document")
    public ApiResponse<SummaryVO> documentSummary(@RequestParam Long documentId) {
        return ApiResponse.ok(service.documentSummary(documentId));
    }

    @GetMapping("/document")
    public ApiResponse<SummaryVO> getDocumentSummary(@RequestParam Long documentId) {
        return ApiResponse.ok(service.getDocumentSummary(documentId));
    }

    @PostMapping("/knowledge-base")
    public ApiResponse<SummaryVO> knowledgeBaseSummary(@RequestParam Long knowledgeBaseId) {
        return ApiResponse.ok(service.knowledgeBaseSummary(knowledgeBaseId));
    }

    @GetMapping("/knowledge-base")
    public ApiResponse<SummaryVO> getKnowledgeBaseSummary(@RequestParam Long knowledgeBaseId) {
        return ApiResponse.ok(service.getKnowledgeBaseSummary(knowledgeBaseId));
    }
}
