package com.knowflow.modules.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowflow.common.BusinessException;
import com.knowflow.modules.config.dto.AiModelConfigRequest;
import com.knowflow.modules.config.dto.PromptTemplateRequest;
import com.knowflow.modules.config.dto.SystemConfigRequest;
import com.knowflow.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ConfigService {
    private final AiModelConfigRepository modelRepository;
    private final PromptTemplateRepository promptRepository;
    private final SystemConfigRepository systemRepository;

    public ConfigService(AiModelConfigRepository modelRepository, PromptTemplateRepository promptRepository, SystemConfigRepository systemRepository) {
        this.modelRepository = modelRepository;
        this.promptRepository = promptRepository;
        this.systemRepository = systemRepository;
    }

    public List<ConfigVO> models() {
        SecurityUtils.requireAdmin();
        return modelRepository.selectList(new LambdaQueryWrapper<AiModelConfig>().orderByDesc(AiModelConfig::getCreateTime)).stream().map(ConfigVO::from).toList();
    }

    @Transactional
    public ConfigVO saveModel(AiModelConfigRequest request) {
        SecurityUtils.requireAdmin();
        if (Boolean.TRUE.equals(request.defaultModel())) {
            modelRepository.selectList(new LambdaQueryWrapper<AiModelConfig>().eq(AiModelConfig::getDefaultModel, true))
                    .forEach(item -> {
                        item.setDefaultModel(false);
                        modelRepository.updateById(item);
                    });
        }
        AiModelConfig config = new AiModelConfig();
        fillModel(config, request);
        modelRepository.insert(config);
        return ConfigVO.from(config);
    }

    @Transactional
    public ConfigVO updateModel(Long id, AiModelConfigRequest request) {
        SecurityUtils.requireAdmin();
        AiModelConfig config = requireModel(id);
        fillModel(config, request);
        modelRepository.updateById(config);
        return ConfigVO.from(config);
    }

    public List<ConfigVO> prompts() {
        SecurityUtils.requireAdmin();
        return promptRepository.selectList(new LambdaQueryWrapper<PromptTemplate>().orderByDesc(PromptTemplate::getCreateTime)).stream().map(ConfigVO::from).toList();
    }

    @Transactional
    public ConfigVO savePrompt(PromptTemplateRequest request) {
        SecurityUtils.requireAdmin();
        PromptTemplate template = new PromptTemplate();
        fillPrompt(template, request);
        promptRepository.insert(template);
        return ConfigVO.from(template);
    }

    @Transactional
    public ConfigVO updatePrompt(Long id, PromptTemplateRequest request) {
        SecurityUtils.requireAdmin();
        PromptTemplate template = promptRepository.selectById(id);
        if (template == null) {
            throw BusinessException.notFound("Prompt 模板不存在");
        }
        fillPrompt(template, request);
        promptRepository.updateById(template);
        return ConfigVO.from(template);
    }

    @Transactional
    public void deletePrompt(Long id) {
        SecurityUtils.requireAdmin();
        promptRepository.deleteById(id);
    }

    public List<ConfigVO> systemConfigs() {
        SecurityUtils.requireAdmin();
        return systemRepository.selectList(new LambdaQueryWrapper<SystemConfig>().orderByDesc(SystemConfig::getCreateTime)).stream().map(ConfigVO::from).toList();
    }

    @Transactional
    public ConfigVO saveSystemConfig(SystemConfigRequest request) {
        SecurityUtils.requireAdmin();
        SystemConfig config = systemRepository.selectOne(new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, request.configKey()).last("limit 1"));
        if (config == null) {
            config = new SystemConfig();
            config.setConfigKey(request.configKey());
        }
        config.setConfigValue(request.configValue());
        config.setDescription(request.description());
        if (config.getId() == null) {
            systemRepository.insert(config);
        } else {
            systemRepository.updateById(config);
        }
        return ConfigVO.from(config);
    }

    private AiModelConfig requireModel(Long id) {
        AiModelConfig config = modelRepository.selectById(id);
        if (config == null) {
            throw BusinessException.notFound("模型配置不存在");
        }
        return config;
    }

    private void fillModel(AiModelConfig config, AiModelConfigRequest request) {
        config.setName(request.name());
        config.setProvider(request.provider());
        config.setBaseUrl(request.baseUrl());
        if (StringUtils.hasText(request.apiKey())) {
            config.setApiKey(request.apiKey());
        }
        config.setModelName(request.modelName());
        config.setEnabled(request.enabled() == null || request.enabled());
        config.setDefaultModel(Boolean.TRUE.equals(request.defaultModel()));
        config.setMaxTokens(request.maxTokens());
        config.setTemperature(request.temperature());
        config.setDescription(request.description());
    }

    private void fillPrompt(PromptTemplate template, PromptTemplateRequest request) {
        template.setCode(request.code());
        template.setName(request.name());
        template.setContent(request.content());
        template.setScene(request.scene());
        template.setEnabled(request.enabled() == null || request.enabled());
        template.setDefaultTemplate(Boolean.TRUE.equals(request.defaultTemplate()));
        template.setDescription(request.description());
    }
}
