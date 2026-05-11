package com.knowflow.controller;

import com.knowflow.common.ApiResponse;
import com.knowflow.common.PageResponse;
import com.knowflow.dto.RenameDocumentRequest;
import com.knowflow.enums.DocumentParseStatus;
import com.knowflow.enums.EmbeddingStatus;
import com.knowflow.service.DocumentService;
import com.knowflow.service.SummaryService;
import com.knowflow.vo.DocumentChunkVO;
import com.knowflow.vo.DocumentSummaryVO;
import com.knowflow.vo.DocumentVO;
import com.knowflow.vo.KeywordVO;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;



@RestController
@RequestMapping("/documents")
public class DocumentController {
    private final DocumentService documentService;
    private final SummaryService summaryService;

    public DocumentController(DocumentService documentService, SummaryService summaryService) {
        this.documentService = documentService;
        this.summaryService = summaryService;
    }

    @PostMapping("/upload")
    public ApiResponse<DocumentVO> upload(@RequestParam Long knowledgeBaseId, @RequestParam MultipartFile file) throws IOException {
        return ApiResponse.ok(documentService.upload(knowledgeBaseId, file));
    }

    @GetMapping
    public ApiResponse<PageResponse<DocumentVO>> page(@RequestParam(required = false) Long knowledgeBaseId,
                                                      @RequestParam(defaultValue = "") String keyword,
                                                      @RequestParam(required = false) DocumentParseStatus parseStatus,
                                                      @RequestParam(required = false) EmbeddingStatus embeddingStatus,
                                                      @RequestParam(required = false) EmbeddingStatus vectorStatus,
                                                      @RequestParam(defaultValue = "1") int pageNo,
                                                      @RequestParam(defaultValue = "10") int pageSize) {
        EmbeddingStatus finalEmbeddingStatus = embeddingStatus != null ? embeddingStatus : vectorStatus;
        return ApiResponse.ok(documentService.page(knowledgeBaseId, keyword, parseStatus, finalEmbeddingStatus, pageNo, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(documentService.detail(id));
    }

    @GetMapping("/{id}/chunks")
    public ApiResponse<PageResponse<DocumentChunkVO>> chunks(@PathVariable Long id,
                                                             @RequestParam(defaultValue = "1") int pageNo,
                                                             @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(documentService.pageChunks(id, pageNo, pageSize));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/rename")
    public ApiResponse<DocumentVO> rename(@PathVariable Long id, @Valid @RequestBody RenameDocumentRequest request) {
        return ApiResponse.ok(documentService.rename(id, request.name()));
    }

    @GetMapping("/{id}/preview")
    public ApiResponse<String> preview(@PathVariable Long id) {
        return ApiResponse.ok(documentService.preview(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Path path = documentService.downloadPath(id);
        return ResponseEntity.ok(new FileSystemResource(path));
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<DocumentVO> retry(@PathVariable Long id) {
        return ApiResponse.ok(documentService.retry(id));
    }

    @GetMapping("/{id}/summary")
    public ApiResponse<DocumentSummaryVO> summary(@PathVariable Long id) {
        return ApiResponse.ok(summaryService.getDocumentSummaryV13(id));
    }

    @PostMapping("/{id}/summary/generate")
    public ApiResponse<DocumentSummaryVO> generateSummary(@PathVariable Long id) {
        return ApiResponse.ok(summaryService.generateDocumentSummary(id));
    }

    @PostMapping("/{id}/summary/regenerate")
    public ApiResponse<DocumentSummaryVO> regenerateSummary(@PathVariable Long id) {
        return ApiResponse.ok(summaryService.regenerateDocumentSummary(id));
    }

    @GetMapping("/{id}/keywords")
    public ApiResponse<java.util.List<KeywordVO>> keywords(@PathVariable Long id) {
        return ApiResponse.ok(summaryService.getDocumentKeywords(id));
    }

    @PostMapping("/{id}/keywords/extract")
    public ApiResponse<java.util.List<KeywordVO>> extractKeywords(@PathVariable Long id) {
        return ApiResponse.ok(summaryService.extractDocumentKeywords(id));
    }

    @PostMapping("/{id}/keywords/reextract")
    public ApiResponse<java.util.List<KeywordVO>> reextractKeywords(@PathVariable Long id) {
        return ApiResponse.ok(summaryService.reextractDocumentKeywords(id));
    }
}
