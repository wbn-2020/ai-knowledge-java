package com.knowflow.modules.config;

import com.knowflow.common.ApiResponse;
import com.knowflow.modules.config.dto.AiModelConfigRequest;
import com.knowflow.modules.config.dto.PromptTemplateRequest;
import com.knowflow.modules.config.dto.SystemConfigRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/config")
public class ConfigController {
    private final ConfigService service;

    public ConfigController(ConfigService service) {
        this.service = service;
    }

    @GetMapping("/models")
    public ApiResponse<List<ConfigVO>> models() {
        return ApiResponse.ok(service.models());
    }

    @PostMapping("/models")
    public ApiResponse<ConfigVO> saveModel(@Valid @RequestBody AiModelConfigRequest request) {
        return ApiResponse.ok(service.saveModel(request));
    }

    @PutMapping("/models/{id}")
    public ApiResponse<ConfigVO> updateModel(@PathVariable Long id, @Valid @RequestBody AiModelConfigRequest request) {
        return ApiResponse.ok(service.updateModel(id, request));
    }

    @DeleteMapping("/models/{id}")
    public ApiResponse<Void> deleteModel(@PathVariable Long id) {
        service.deleteModel(id);
        return ApiResponse.ok();
    }

    @PostMapping("/models/{id}/test")
    public ApiResponse<String> testModel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> request) {
        return ApiResponse.ok(service.testModel(id, request == null ? null : request.get("prompt")));
    }

    @GetMapping("/prompts")
    public ApiResponse<List<ConfigVO>> prompts() {
        return ApiResponse.ok(service.prompts());
    }

    @GetMapping("/prompts/{id}")
    public ApiResponse<ConfigVO> promptDetail(@PathVariable Long id) {
        return ApiResponse.ok(service.promptDetail(id));
    }

    @PostMapping("/prompts")
    public ApiResponse<ConfigVO> savePrompt(@Valid @RequestBody PromptTemplateRequest request) {
        return ApiResponse.ok(service.savePrompt(request));
    }

    @PutMapping("/prompts/{id}")
    public ApiResponse<ConfigVO> updatePrompt(@PathVariable Long id, @Valid @RequestBody PromptTemplateRequest request) {
        return ApiResponse.ok(service.updatePrompt(id, request));
    }

    @DeleteMapping("/prompts/{id}")
    public ApiResponse<Void> deletePrompt(@PathVariable Long id) {
        service.deletePrompt(id);
        return ApiResponse.ok();
    }

    @PutMapping("/prompts/{id}/enabled")
    public ApiResponse<ConfigVO> setPromptEnabled(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        return ApiResponse.ok(service.setPromptEnabled(id, Boolean.TRUE.equals(request.get("enabled"))));
    }

    @GetMapping("/system")
    public ApiResponse<List<ConfigVO>> systemConfigs() {
        return ApiResponse.ok(service.systemConfigs());
    }

    @PostMapping("/system")
    public ApiResponse<ConfigVO> saveSystemConfig(@Valid @RequestBody SystemConfigRequest request) {
        return ApiResponse.ok(service.saveSystemConfig(request));
    }

    @DeleteMapping("/system/{id}")
    public ApiResponse<Void> deleteSystemConfig(@PathVariable Long id) {
        service.deleteSystemConfig(id);
        return ApiResponse.ok();
    }
}
