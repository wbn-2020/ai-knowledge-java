package com.knowflow.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowflow.common.BusinessException;
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
    private final String apiKey;
    private final String model;

    public DeepSeekLlmClient(@Value("${knowflow.deepseek.base-url}") String baseUrl,
                             @Value("${knowflow.deepseek.api-key}") String apiKey,
                             @Value("${knowflow.deepseek.model}") String model,
                             ObjectMapper objectMapper) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String complete(String prompt) {
        if (!StringUtils.hasText(apiKey)) {
            throw BusinessException.badRequest("DeepSeek API key is not configured");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("stream", false);

        String response = webClient.post()
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
}
