package com.knowflow.modules.log;

import com.knowflow.common.ApiResponse;
import com.knowflow.common.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/logs")
public class LogController {
    private final LogService service;

    public LogController(LogService service) {
        this.service = service;
    }

    @GetMapping("/operations")
    public ApiResponse<PageResponse<LogVO>> operations(@RequestParam(defaultValue = "1") int pageNo,
                                                       @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(service.operationLogs(pageNo, pageSize));
    }

    @GetMapping("/logins")
    public ApiResponse<PageResponse<LogVO>> logins(@RequestParam(defaultValue = "1") int pageNo,
                                                   @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(service.loginLogs(pageNo, pageSize));
    }

    @GetMapping("/ai-calls")
    public ApiResponse<PageResponse<LogVO>> aiCalls(@RequestParam(defaultValue = "1") int pageNo,
                                                    @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(service.aiCallLogs(pageNo, pageSize));
    }
}
