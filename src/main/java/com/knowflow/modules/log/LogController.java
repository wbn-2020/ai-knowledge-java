package com.knowflow.modules.log;

import com.knowflow.common.ApiResponse;
import com.knowflow.common.PageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

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
}
