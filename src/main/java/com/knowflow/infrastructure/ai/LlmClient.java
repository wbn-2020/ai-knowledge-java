package com.knowflow.infrastructure.ai;

import com.knowflow.entity.AiModelConfig;

public interface LlmClient {
    String complete(String prompt);

    default String complete(String prompt, AiModelConfig modelConfig) {
        return complete(prompt);
    }
}
