package com.knowflow.modules.log;

import com.knowflow.common.ApiResponse;
import com.knowflow.common.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/logs")
public class LogController {
    private final LogService service;

    public LogController(LogService service) {
        this.service = service;
    }

    @GetMapping("/operations")
    public ApiResponse<PageResponse<LogVO>> operations(@RequestParam(required = false) String action,
                                                       @RequestParam(required = false) Long userId,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                                       @RequestParam(defaultValue = "1") int pageNo,
                                                       @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(service.operationLogs(action, userId, startTime, endTime, pageNo, pageSize));
    }

    @GetMapping("/logins")
    public ApiResponse<PageResponse<LogVO>> logins(@RequestParam(required = false) String account,
                                                   @RequestParam(required = false) Boolean success,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                                   @RequestParam(defaultValue = "1") int pageNo,
                                                   @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(service.loginLogs(account, success, startTime, endTime, pageNo, pageSize));
    }

    @GetMapping("/ai-calls")
    public ApiResponse<PageResponse<LogVO>> aiCalls(@RequestParam(required = false) String modelName,
                                                    @RequestParam(required = false) String callType,
                                                    @RequestParam(required = false) Boolean success,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                                    @RequestParam(defaultValue = "1") int pageNo,
                                                    @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(service.aiCallLogs(modelName, callType, success, startTime, endTime, pageNo, pageSize));
    }

    @GetMapping("/operations/export")
    public ResponseEntity<String> exportOperations(@RequestParam(required = false) String action,
                                                   @RequestParam(required = false) Long userId,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return csv("operation-logs.csv", service.exportOperations(action, userId, startTime, endTime));
    }

    @GetMapping("/logins/export")
    public ResponseEntity<String> exportLogins(@RequestParam(required = false) String account,
                                               @RequestParam(required = false) Boolean success,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return csv("login-logs.csv", service.exportLogins(account, success, startTime, endTime));
    }

    @GetMapping("/ai-calls/export")
    public ResponseEntity<String> exportAiCalls(@RequestParam(required = false) String modelName,
                                                @RequestParam(required = false) String callType,
                                                @RequestParam(required = false) Boolean success,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return csv("ai-call-logs.csv", service.exportAiCalls(modelName, callType, success, startTime, endTime));
    }

    @GetMapping("/alerts")
    public ApiResponse<List<AlertVO>> alerts() {
        return ApiResponse.ok(service.alerts());
    }

    private ResponseEntity<String> csv(String filename, String content) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }
}
