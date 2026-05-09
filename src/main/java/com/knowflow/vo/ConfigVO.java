package com.knowflow.vo;

import com.knowflow.entity.AiModelConfig;
import com.knowflow.entity.PromptTemplate;
import com.knowflow.entity.SystemConfig;

public class ConfigVO {
    private Long id;
    private String name;
    private String code;
    private String value;
    private Boolean enabled;
    private Boolean defaultFlag;
    private String description;

    private String provider;
    private String modelType;
    private String modelName;
    private String baseUrl;
    private String apiKeyMasked;
    private Double temperature;

    public static ConfigVO from(AiModelConfig c) {
        ConfigVO vo = new ConfigVO();
        vo.id = c.getId();
        vo.name = c.getName();
        vo.code = c.getProvider() + ":" + c.getModelName();
        vo.value = mask(c.getApiKey());
        vo.enabled = c.getEnabled();
        vo.defaultFlag = c.getDefaultModel();
        vo.description = c.getDescription();
        vo.provider = c.getProvider();
        vo.modelType = inferModelType(c.getModelName());
        vo.modelName = c.getModelName();
        vo.baseUrl = c.getBaseUrl();
        vo.apiKeyMasked = mask(c.getApiKey());
        vo.temperature = c.getTemperature();
        return vo;
    }

    public static ConfigVO from(PromptTemplate t) {
        ConfigVO vo = new ConfigVO();
        vo.id = t.getId();
        vo.name = t.getName();
        vo.code = t.getCode();
        vo.value = t.getContent();
        vo.enabled = t.getEnabled();
        vo.defaultFlag = t.getDefaultTemplate();
        vo.description = t.getDescription();
        return vo;
    }

    public static ConfigVO from(SystemConfig c) {
        ConfigVO vo = new ConfigVO();
        vo.id = c.getId();
        vo.name = c.getConfigKey();
        vo.code = c.getConfigKey();
        vo.value = c.getConfigValue();
        vo.enabled = true;
        vo.defaultFlag = false;
        vo.description = c.getDescription();
        return vo;
    }

    private static String mask(String value) {
        if (value == null || value.length() < 8) {
            return "";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private static String inferModelType(String modelName) {
        if (modelName == null) {
            return "LLM";
        }
        String lower = modelName.toLowerCase();
        return lower.contains("embedding") ? "EMBEDDING" : "LLM";
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getValue() { return value; }
    public Boolean getEnabled() { return enabled; }
    public Boolean getDefaultFlag() { return defaultFlag; }
    public String getDescription() { return description; }
    public String getProvider() { return provider; }
    public String getModelType() { return modelType; }
    public String getModelName() { return modelName; }
    public String getBaseUrl() { return baseUrl; }
    public String getApiKeyMasked() { return apiKeyMasked; }
    public Double getTemperature() { return temperature; }
}
