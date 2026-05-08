package com.knowflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowflow.entity.SystemConfig;
import com.knowflow.mapper.SystemConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


@Service
public class RuntimeConfigService {
    private final SystemConfigRepository systemConfigRepository;

    public RuntimeConfigService(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    public int intValue(String key, int fallback) {
        String value = value(key);
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public double doubleValue(String key, double fallback) {
        String value = value(key);
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public String value(String key) {
        SystemConfig config = systemConfigRepository.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key)
                .last("limit 1"));
        return config == null ? null : config.getConfigValue();
    }
}
