package com.knowflow.infrastructure.ai;

import org.springframework.stereotype.Component;

@Component
public class MockLlmClient implements LlmClient {
    @Override
    public String complete(String prompt) {
        int idx = prompt.indexOf("相关文档片段：");
        if (idx < 0) {
            return "当前知识库中没有找到足够依据。";
        }
        String context = prompt.substring(idx).replace("相关文档片段：", "").trim();
        if (context.length() > 500) {
            context = context.substring(0, 500);
        }
        return "根据当前知识库内容，相关依据如下：" + context;
    }
}
