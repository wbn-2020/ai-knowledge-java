package com.knowflow.controller;

import com.knowflow.common.ApiResponse;
import com.knowflow.common.PageResponse;
import com.knowflow.dto.RenameDocumentRequest;
import com.knowflow.enums.DocumentParseStatus;
import com.knowflow.enums.EmbeddingStatus;
import com.knowflow.service.DocumentService;
import com.knowflow.vo.DocumentVO;
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

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
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
                                                      @RequestParam(defaultValue = "1") int pageNo,
                                                      @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(documentService.page(knowledgeBaseId, keyword, parseStatus, embeddingStatus, pageNo, pageSize));
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
}
