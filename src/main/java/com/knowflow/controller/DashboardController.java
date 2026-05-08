package com.knowflow.controller;

import com.knowflow.common.ApiResponse;
import com.knowflow.service.DashboardService;
import com.knowflow.vo.DashboardVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardVO> overview() {
        return ApiResponse.ok(service.overview());
    }
}
