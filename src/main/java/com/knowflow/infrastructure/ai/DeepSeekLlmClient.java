package com.knowflow.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowflow.common.BusinessException;
import com.knowflow.modules.config.AiModelConfig;
import com.knowflow.modules.config.AiModelConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DeepSeekLlmClient implements LlmClient {
    private static final String NO_EVIDENCE = "The current knowledge base has no sufficient evidence.";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String configuredApiKey;
    private final String configuredModel;
    private final String configuredBaseUrl;
    private final AiModelConfigRepository modelRepository;

    public DeepSeekLlmClient(@Value("${knowflow.deepseek.base-url}") String baseUrl,
                             @Value("${knowflow.deepseek.api-key}") String apiKey,
                             @Value("${knowflow.deepseek.model}") String model,
                             ObjectMapper objectMapper,
                             AiModelConfigRepository modelRepository) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.configuredBaseUrl = baseUrl;
        this.configuredApiKey = apiKey;
        this.configuredModel = model;
        this.modelRepository = modelRepository;
    }

    @Override
    public String complete(String prompt) {
        AiModelConfig config = defaultModelConfig();
        String apiKey = StringUtils.hasText(config == null ? null : config.getApiKey()) ? config.getApiKey() : configuredApiKey;
        String model = StringUtils.hasText(config == null ? null : config.getModelName()) ? config.getModelName() : configuredModel;
        String baseUrl = StringUtils.hasText(config == null ? null : config.getBaseUrl()) ? config.getBaseUrl() : configuredBaseUrl;
        if (!StringUtils.hasText(apiKey)) {
            throw BusinessException.badRequest("DeepSeek API key is not configured");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("stream", false);

        WebClient client = baseUrl.equals(configuredBaseUrl) ? webClient : WebClient.builder().baseUrl(baseUrl).build();
        String response = client.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(90));

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText();
                return StringUtils.hasText(content) ? content : NO_EVIDENCE;
            }
            return NO_EVIDENCE;
        } catch (Exception ex) {
            throw BusinessException.badRequest("Failed to parse DeepSeek response");
        }
    }

    private AiModelConfig defaultModelConfig() {
        return modelRepository.selectOne(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getEnabled, true)
                .eq(AiModelConfig::getDefaultModel, true)
                .orderByDesc(AiModelConfig::getUpdateTime)
                .last("limit 1"));
    }
}
