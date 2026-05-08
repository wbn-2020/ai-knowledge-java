package com.knowflow.modules.chat;

import com.knowflow.common.ApiResponse;
import com.knowflow.common.PageResponse;
import com.knowflow.modules.chat.dto.AskRequest;
import com.knowflow.modules.chat.dto.DocumentAskRequest;
import com.knowflow.modules.chat.dto.FeedbackRequest;
import com.knowflow.modules.chat.dto.MultiKnowledgeAskRequest;
import com.knowflow.modules.chat.dto.RenameSessionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/ask")
    public ApiResponse<AskVO> ask(@Valid @RequestBody AskRequest request) {
        return ApiResponse.ok(chatService.ask(request));
    }

    @PostMapping("/ask/document")
    public ApiResponse<AskVO> askDocument(@Valid @RequestBody DocumentAskRequest request) {
        return ApiResponse.ok(chatService.askDocument(request));
    }

    @PostMapping("/ask/multi")
    public ApiResponse<AskVO> askMulti(@Valid @RequestBody MultiKnowledgeAskRequest request) {
        return ApiResponse.ok(chatService.askMulti(request));
    }

    @GetMapping("/sessions")
    public ApiResponse<PageResponse<ChatSessionVO>> sessions(@RequestParam(defaultValue = "1") int pageNo,
                                                             @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(chatService.sessions(pageNo, pageSize));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<ChatMessageVO>> messages(@PathVariable Long sessionId) {
        return ApiResponse.ok(chatService.messages(sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(@PathVariable Long sessionId) {
        chatService.deleteSession(sessionId);
        return ApiResponse.ok();
    }

    @PutMapping("/sessions/{sessionId}/rename")
    public ApiResponse<ChatSessionVO> renameSession(@PathVariable Long sessionId, @Valid @RequestBody RenameSessionRequest request) {
        return ApiResponse.ok(chatService.renameSession(sessionId, request.title()));
    }

    @DeleteMapping("/sessions/{sessionId}/messages")
    public ApiResponse<Void> clearSession(@PathVariable Long sessionId) {
        chatService.clearSession(sessionId);
        return ApiResponse.ok();
    }

    @PostMapping("/sessions/{sessionId}/regenerate")
    public ApiResponse<AskVO> regenerate(@PathVariable Long sessionId) {
        return ApiResponse.ok(chatService.regenerate(sessionId));
    }

    @PostMapping("/feedback")
    public ApiResponse<Void> feedback(@Valid @RequestBody FeedbackRequest request) {
        chatService.feedback(request);
        return ApiResponse.ok();
    }

    @GetMapping("/sessions/{sessionId}/export/markdown")
    public ResponseEntity<String> exportMarkdown(@PathVariable Long sessionId) {
        return export(sessionId, "markdown", "md");
    }

    @GetMapping("/sessions/{sessionId}/export/pdf")
    public ResponseEntity<String> exportPdf(@PathVariable Long sessionId) {
        return export(sessionId, "pdf", "txt");
    }

    @GetMapping("/sessions/{sessionId}/export/word")
    public ResponseEntity<String> exportWord(@PathVariable Long sessionId) {
        return export(sessionId, "word", "txt");
    }

    private ResponseEntity<String> export(Long sessionId, String format, String extension) {
        String content = "markdown".equals(format) ? chatService.exportMarkdown(sessionId) : chatService.exportText(sessionId, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=chat-" + sessionId + "." + extension)
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }
}
