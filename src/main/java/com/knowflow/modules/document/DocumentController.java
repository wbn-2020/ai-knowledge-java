package com.knowflow.modules.document;

import com.knowflow.common.ApiResponse;
import com.knowflow.common.PageResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/documents")
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ApiResponse<DocumentVO> upload(@RequestParam Long knowledgeBaseId, @RequestParam MultipartFile file) throws IOException {
        return ApiResponse.ok(documentService.upload(knowledgeBaseId, file));
    }

    @GetMapping
    public ApiResponse<PageResponse<DocumentVO>> page(@RequestParam Long knowledgeBaseId,
                                                      @RequestParam(defaultValue = "") String keyword,
                                                      @RequestParam(defaultValue = "1") int pageNo,
                                                      @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(documentService.page(knowledgeBaseId, keyword, pageNo, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(documentService.detail(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<DocumentVO> retry(@PathVariable Long id) {
        return ApiResponse.ok(documentService.retry(id));
    }
}
