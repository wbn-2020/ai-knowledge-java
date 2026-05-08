package com.knowflow.modules.summary;

import com.knowflow.common.ApiResponse;
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

    @PostMapping("/knowledge-base")
    public ApiResponse<SummaryVO> knowledgeBaseSummary(@RequestParam Long knowledgeBaseId) {
        return ApiResponse.ok(service.knowledgeBaseSummary(knowledgeBaseId));
    }
}
