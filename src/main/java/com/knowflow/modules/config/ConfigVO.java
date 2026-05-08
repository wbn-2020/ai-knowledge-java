package com.knowflow.modules.config;

public record ConfigVO(Long id, String name, String code, String value, Boolean enabled, Boolean defaultFlag, String description) {
    public static ConfigVO from(AiModelConfig c) {
        return new ConfigVO(c.getId(), c.getName(), c.getProvider() + ":" + c.getModelName(), mask(c.getApiKey()), c.getEnabled(), c.getDefaultModel(), c.getDescription());
    }

    public static ConfigVO from(PromptTemplate t) {
        return new ConfigVO(t.getId(), t.getName(), t.getCode(), t.getContent(), t.getEnabled(), t.getDefaultTemplate(), t.getDescription());
    }

    public static ConfigVO from(SystemConfig c) {
        return new ConfigVO(c.getId(), c.getConfigKey(), c.getConfigKey(), c.getConfigValue(), true, false, c.getDescription());
    }

    private static String mask(String value) {
        if (value == null || value.length() < 8) {
            return "";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
