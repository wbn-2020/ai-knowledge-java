package com.knowflow.controller;

import com.knowflow.common.ApiResponse;
import com.knowflow.common.PageResponse;
import com.knowflow.service.ChatFeedbackService;
import com.knowflow.vo.AdminFeedbackVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminFeedbackController {
    private final ChatFeedbackService chatFeedbackService;

    public AdminFeedbackController(ChatFeedbackService chatFeedbackService) {
        this.chatFeedbackService = chatFeedbackService;
    }

    @GetMapping("/feedbacks")
    public ApiResponse<PageResponse<AdminFeedbackVO>> feedbacks(@RequestParam(required = false) String feedbackType,
                                                                @RequestParam(required = false) String reason,
                                                                @RequestParam(required = false) String username,
                                                                @RequestParam(required = false) String keyword,
                                                                @RequestParam(defaultValue = "1") int pageNo,
                                                                @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(chatFeedbackService.adminPage(feedbackType, reason, username, keyword, pageNo, pageSize));
    }
}
