package com.knowflow.modules.admin;

import com.knowflow.common.ApiResponse;
import com.knowflow.common.PageResponse;
import com.knowflow.common.enums.UserStatus;
import com.knowflow.modules.auth.AuthService;
import com.knowflow.modules.auth.dto.LoginRequest;
import com.knowflow.modules.auth.dto.LoginVO;
import com.knowflow.modules.document.DocumentTaskVO;
import com.knowflow.modules.document.DocumentVO;
import com.knowflow.modules.knowledge.KnowledgeBaseVO;
import com.knowflow.modules.user.UserVO;
import jakarta.validation.Valid;
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

    @GetMapping("/knowledge-bases")
    public ApiResponse<PageResponse<KnowledgeBaseVO>> knowledgeBases(@RequestParam(defaultValue = "") String keyword,
                                                                      @RequestParam(defaultValue = "1") int pageNo,
                                                                      @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(adminService.knowledgeBases(keyword, pageNo, pageSize));
    }

    @GetMapping("/knowledge-bases/{id}")
    public ApiResponse<KnowledgeBaseVO> knowledgeBaseDetail(@PathVariable Long id) {
        return ApiResponse.ok(adminService.knowledgeBaseDetail(id));
    }

    @GetMapping("/documents")
    public ApiResponse<PageResponse<DocumentVO>> documents(@RequestParam(defaultValue = "") String keyword,
                                                           @RequestParam(defaultValue = "1") int pageNo,
                                                           @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(adminService.documents(keyword, pageNo, pageSize));
    }

    @GetMapping("/documents/{id}")
    public ApiResponse<DocumentVO> documentDetail(@PathVariable Long id) {
        return ApiResponse.ok(adminService.documentDetail(id));
    }

    @GetMapping("/document-tasks")
    public ApiResponse<PageResponse<DocumentTaskVO>> tasks(@RequestParam(defaultValue = "1") int pageNo,
                                                           @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(adminService.tasks(pageNo, pageSize));
    }

    @GetMapping("/document-tasks/{id}")
    public ApiResponse<DocumentTaskVO> taskDetail(@PathVariable Long id) {
        return ApiResponse.ok(adminService.taskDetail(id));
    }
}
