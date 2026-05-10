package com.knowflow.controller;

import com.knowflow.common.ApiResponse;
import com.knowflow.common.PageResponse;
import com.knowflow.dto.LoginRequest;
import com.knowflow.vo.LoginVO;
import com.knowflow.dto.ResetPasswordRequest;
import com.knowflow.enums.DocumentParseStatus;
import com.knowflow.enums.KnowledgeBaseStatus;
import com.knowflow.enums.TaskStatus;
import com.knowflow.enums.UserStatus;
import com.knowflow.service.AdminService;
import com.knowflow.service.AuthService;
import com.knowflow.vo.AdminOverviewVO;
import com.knowflow.vo.DocumentChunkVO;
import com.knowflow.vo.DocumentTaskVO;
import com.knowflow.vo.DocumentVO;
import com.knowflow.vo.KnowledgeBaseVO;
import com.knowflow.vo.UserVO;
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
@RequestMapping("/admin")
public class AdminController {
    private final AuthService authService;
    private final AdminService adminService;

    public AdminController(AuthService authService, AdminService adminService) {
        this.authService = authService;
        this.adminService = adminService;
    }

    @PostMapping("/auth/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.adminLogin(request));
    }

    @GetMapping("/dashboard/overview")
    public ApiResponse<AdminOverviewVO> overview() {
        return ApiResponse.ok(adminService.overview());
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserVO>> users(@RequestParam(defaultValue = "") String keyword,
                                                   @RequestParam(required = false) UserStatus status,
                                                   @RequestParam(defaultValue = "1") int pageNo,
                                                   @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(adminService.users(keyword, status, pageNo, pageSize));
    }

    @PutMapping("/users/{userId}/status")
    public ApiResponse<UserVO> setUserStatus(@PathVariable Long userId, @RequestParam UserStatus status) {
        return ApiResponse.ok(adminService.setUserStatus(userId, status));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<UserVO> userDetail(@PathVariable Long userId) {
        return ApiResponse.ok(adminService.userDetail(userId));
    }

    @PutMapping("/users/{userId}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long userId, @Valid @RequestBody ResetPasswordRequest request) {
        adminService.resetPassword(userId, request.password());
        return ApiResponse.ok();
    }

    @GetMapping("/knowledge-bases")
    public ApiResponse<PageResponse<KnowledgeBaseVO>> knowledgeBases(@RequestParam(defaultValue = "") String keyword,
                                                                      @RequestParam(required = false) KnowledgeBaseStatus status,
                                                                      @RequestParam(defaultValue = "1") int pageNo,
                                                                      @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(adminService.knowledgeBases(keyword, status, pageNo, pageSize));
    }

    @GetMapping("/knowledge-bases/{id}")
    public ApiResponse<KnowledgeBaseVO> knowledgeBaseDetail(@PathVariable Long id) {
        return ApiResponse.ok(adminService.knowledgeBaseDetail(id));
    }

    @PutMapping("/knowledge-bases/{id}/status")
    public ApiResponse<KnowledgeBaseVO> setKnowledgeBaseStatus(@PathVariable Long id, @RequestParam KnowledgeBaseStatus status) {
        return ApiResponse.ok(adminService.setKnowledgeBaseStatus(id, status));
    }

    @DeleteMapping("/knowledge-bases/{id}")
    public ApiResponse<Void> deleteKnowledgeBase(@PathVariable Long id) {
        adminService.deleteKnowledgeBase(id);
        return ApiResponse.ok();
    }

    @GetMapping("/documents")
    public ApiResponse<PageResponse<DocumentVO>> documents(@RequestParam(defaultValue = "") String keyword,
                                                           @RequestParam(required = false) Long knowledgeBaseId,
                                                           @RequestParam(required = false) Long userId,
                                                           @RequestParam(required = false) String username,
                                                           @RequestParam(required = false) DocumentParseStatus parseStatus,
                                                           @RequestParam(required = false) String fileType,
                                                           @RequestParam(defaultValue = "1") int pageNo,
                                                           @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(adminService.documents(keyword, knowledgeBaseId, userId, username, parseStatus, fileType, pageNo, pageSize));
    }

    @GetMapping("/documents/{id}")
    public ApiResponse<DocumentVO> documentDetail(@PathVariable Long id) {
        return ApiResponse.ok(adminService.documentDetail(id));
    }

    @GetMapping("/documents/{id}/chunks")
    public ApiResponse<PageResponse<DocumentChunkVO>> documentChunks(@PathVariable Long id,
                                                                     @RequestParam(defaultValue = "1") int pageNo,
                                                                     @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(adminService.documentChunks(id, pageNo, pageSize));
    }

    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id) {
        adminService.deleteDocument(id);
        return ApiResponse.ok();
    }

    @PostMapping("/documents/{id}/retry")
    public ApiResponse<DocumentVO> retryDocument(@PathVariable Long id) {
        return ApiResponse.ok(adminService.retryDocument(id));
    }

    @GetMapping("/document-tasks")
    public ApiResponse<PageResponse<DocumentTaskVO>> tasks(@RequestParam(required = false) TaskStatus status,
                                                           @RequestParam(required = false) String taskType,
                                                           @RequestParam(required = false) Long documentId,
                                                           @RequestParam(defaultValue = "") String keyword,
                                                           @RequestParam(defaultValue = "1") int pageNo,
                                                           @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(adminService.tasks(status, taskType, documentId, keyword, pageNo, pageSize));
    }

    @PostMapping("/document-tasks/{id}/retry")
    public ApiResponse<DocumentTaskVO> retryTask(@PathVariable Long id) {
        return ApiResponse.ok(adminService.retryTask(id));
    }

    @GetMapping("/document-tasks/{id}")
    public ApiResponse<DocumentTaskVO> taskDetail(@PathVariable Long id) {
        return ApiResponse.ok(adminService.taskDetail(id));
    }
}
