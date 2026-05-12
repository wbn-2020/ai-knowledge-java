package com.knowflow.controller;

import com.knowflow.common.ApiResponse;
import com.knowflow.dto.ChatFeedbackRequest;
import com.knowflow.service.ChatFeedbackService;
import com.knowflow.vo.ChatFeedbackVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat/messages")
public class ChatFeedbackController {
    private final ChatFeedbackService chatFeedbackService;

    public ChatFeedbackController(ChatFeedbackService chatFeedbackService) {
        this.chatFeedbackService = chatFeedbackService;
    }

    @PostMapping("/{messageId}/feedback")
    public ApiResponse<ChatFeedbackVO> submit(@PathVariable Long messageId, @Valid @RequestBody ChatFeedbackRequest request) {
        return ApiResponse.ok(chatFeedbackService.submit(messageId, request));
    }

    @GetMapping("/{messageId}/feedback")
    public ApiResponse<ChatFeedbackVO> get(@PathVariable Long messageId) {
        return ApiResponse.ok(chatFeedbackService.getMyFeedback(messageId));
    }

    @DeleteMapping("/{messageId}/feedback")
    public ApiResponse<Void> delete(@PathVariable Long messageId) {
        chatFeedbackService.deleteMyFeedback(messageId);
        return ApiResponse.ok();
    }
}
