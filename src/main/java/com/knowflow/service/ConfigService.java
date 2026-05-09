package com.knowflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowflow.common.BusinessException;
import com.knowflow.dto.AiModelConfigRequest;
import com.knowflow.dto.PromptTemplateRequest;
import com.knowflow.dto.SystemConfigRequest;
import com.knowflow.entity.AiModelConfig;
import com.knowflow.entity.PromptTemplate;
import com.knowflow.entity.SystemConfig;
import com.knowflow.infrastructure.ai.LlmClient;
import com.knowflow.mapper.AiModelConfigRepository;
import com.knowflow.mapper.PromptTemplateRepository;
import com.knowflow.mapper.SystemConfigRepository;
import com.knowflow.security.SecurityUtils;
import com.knowflow.vo.ConfigVO;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;



@Service
public class ConfigService {
    private static final String FALLBACK_RAG_PROMPT = """
            You are KnowFlow AI. Answer only from the provided document chunks.
            If evidence is insufficient, say that the current knowledge base has no sufficient evidence.
            """;

    private final AiModelConfigRepository modelRepository;
    private final PromptTemplateRepository promptRepository;
    private final SystemConfigRepository systemRepository;
    private final LlmClient llmClient;
    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

    public ConfigService(AiModelConfigRepository modelRepository,
                         PromptTemplateRepository promptRepository,
                         SystemConfigRepository systemRepository,
                         LlmClient llmClient) {
        this.modelRepository = modelRepository;
        this.promptRepository = promptRepository;
        this.systemRepository = systemRepository;
        this.llmClient = llmClient;
    }

    public List<ConfigVO> models() {
        SecurityUtils.requireAdmin();
        return modelRepository.selectList(new LambdaQueryWrapper<AiModelConfig>()
                        .orderByDesc(AiModelConfig::getCreateTime))
                .stream()
                .map(ConfigVO::from)
                .toList();
    }

    @Transactional
    public ConfigVO saveModel(AiModelConfigRequest request) {
        SecurityUtils.requireAdmin();
        AiModelConfig config = new AiModelConfig();
        fillModel(config, request);
        if (Boolean.TRUE.equals(config.getDefaultModel())) {
            clearDefaultModels(null);
        }
        modelRepository.insert(config);
        log.debug("Saved model config: id={}, provider={}, modelName={}, hasApiKey={}",
                config.getId(), config.getProvider(), config.getModelName(), StringUtils.hasText(config.getApiKey()));
        return ConfigVO.from(config);
    }

    @Transactional
    public ConfigVO updateModel(Long id, AiModelConfigRequest request) {
        SecurityUtils.requireAdmin();
        AiModelConfig config = requireModel(id);
        fillModel(config, request);
        if (Boolean.TRUE.equals(config.getDefaultModel())) {
            clearDefaultModels(id);
        }
        modelRepository.updateById(config);
        log.debug("Updated model config: id={}, provider={}, modelName={}, hasApiKey={}",
                config.getId(), config.getProvider(), config.getModelName(), StringUtils.hasText(config.getApiKey()));
        return ConfigVO.from(config);
    }

    @Transactional
    public void deleteModel(Long id) {
        SecurityUtils.requireAdmin();
        requireModel(id);
        modelRepository.deleteById(id);
    }

    public String testModel(Long id, String prompt) {
        SecurityUtils.requireAdmin();
        AiModelConfig config = requireModel(id);
        log.debug("Testing model config: id={}, provider={}, modelName={}, baseUrl={}, hasApiKey={}",
                config.getId(), config.getProvider(), config.getModelName(), config.getBaseUrl(), StringUtils.hasText(config.getApiKey()));
        if (!StringUtils.hasText(config.getApiKey())) {
            throw BusinessException.badRequest("当前模型配置未设置 API Key");
        }
        String testPrompt = StringUtils.hasText(prompt) ? prompt : "Reply with: KnowFlow model test passed.";
        return llmClient.complete(testPrompt, config);
    }

    public List<ConfigVO> prompts() {
        SecurityUtils.requireAdmin();
        return promptRepository.selectList(new LambdaQueryWrapper<PromptTemplate>()
                        .orderByDesc(PromptTemplate::getCreateTime))
                .stream()
                .map(ConfigVO::from)
                .toList();
    }

    public ConfigVO promptDetail(Long id) {
        SecurityUtils.requireAdmin();
        return ConfigVO.from(requirePrompt(id));
    }

    @Transactional
    public ConfigVO savePrompt(PromptTemplateRequest request) {
        SecurityUtils.requireAdmin();
        PromptTemplate template = new PromptTemplate();
        fillPrompt(template, request);
        if (Boolean.TRUE.equals(template.getDefaultTemplate())) {
            clearDefaultPrompts(template.getScene(), null);
        }
        promptRepository.insert(template);
        return ConfigVO.from(template);
    }

    @Transactional
    public ConfigVO updatePrompt(Long id, PromptTemplateRequest request) {
        SecurityUtils.requireAdmin();
        PromptTemplate template = requirePrompt(id);
        fillPrompt(template, request);
        if (Boolean.TRUE.equals(template.getDefaultTemplate())) {
            clearDefaultPrompts(template.getScene(), id);
        }
        promptRepository.updateById(template);
        return ConfigVO.from(template);
    }

    @Transactional
    public ConfigVO setPromptEnabled(Long id, boolean enabled) {
        SecurityUtils.requireAdmin();
        PromptTemplate template = requirePrompt(id);
        template.setEnabled(enabled);
        promptRepository.updateById(template);
        return ConfigVO.from(template);
    }

    @Transactional
    public void deletePrompt(Long id) {
        SecurityUtils.requireAdmin();
        requirePrompt(id);
        promptRepository.deleteById(id);
    }

    public String defaultRagPrompt() {
        PromptTemplate template = promptRepository.selectOne(new LambdaQueryWrapper<PromptTemplate>()
                .eq(PromptTemplate::getScene, "RAG")
                .eq(PromptTemplate::getEnabled, true)
                .eq(PromptTemplate::getDefaultTemplate, true)
                .orderByDesc(PromptTemplate::getUpdateTime)
                .last("limit 1"));
        return template == null || !StringUtils.hasText(template.getContent()) ? FALLBACK_RAG_PROMPT : template.getContent();
    }

    public AiModelConfig defaultModelConfig() {
        return modelRepository.selectOne(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getEnabled, true)
                .eq(AiModelConfig::getDefaultModel, true)
                .orderByDesc(AiModelConfig::getUpdateTime)
                .last("limit 1"));
    }

    public AiModelConfig requireEnabledLlmConfig() {
        List<AiModelConfig> configs = modelRepository.selectList(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getEnabled, true)
                .orderByDesc(AiModelConfig::getDefaultModel)
                .orderByDesc(AiModelConfig::getUpdateTime)
                .last("limit 20"));
        AiModelConfig selected = configs.stream().filter(this::isLlm).findFirst().orElse(null);
        if (selected == null) {
            throw BusinessException.badRequest("当前未启用大语言模型配置");
        }
        if (!StringUtils.hasText(selected.getApiKey())) {
            throw BusinessException.badRequest("当前模型配置未设置 API Key");
        }
        if (!StringUtils.hasText(selected.getBaseUrl())) {
            throw BusinessException.badRequest("当前模型配置未设置接口地址");
        }
        if (!StringUtils.hasText(selected.getModelName())) {
            throw BusinessException.badRequest("当前模型配置未设置模型名称");
        }
        return selected;
    }

    public List<ConfigVO> systemConfigs() {
        SecurityUtils.requireAdmin();
        return systemRepository.selectList(new LambdaQueryWrapper<SystemConfig>()
                        .orderByDesc(SystemConfig::getCreateTime))
                .stream()
                .map(ConfigVO::from)
                .toList();
    }

    @Transactional
    public ConfigVO saveSystemConfig(SystemConfigRequest request) {
        SecurityUtils.requireAdmin();
        SystemConfig config = systemRepository.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, request.configKey())
                .last("limit 1"));
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

    @Transactional
    public void deleteSystemConfig(Long id) {
        SecurityUtils.requireAdmin();
        if (systemRepository.selectById(id) == null) {
            throw BusinessException.notFound("system config not found");
        }
        systemRepository.deleteById(id);
    }

    private AiModelConfig requireModel(Long id) {
        AiModelConfig config = modelRepository.selectById(id);
        if (config == null) {
            throw BusinessException.notFound("model config not found");
        }
        return config;
    }

    private PromptTemplate requirePrompt(Long id) {
        PromptTemplate template = promptRepository.selectById(id);
        if (template == null) {
            throw BusinessException.notFound("prompt template not found");
        }
        return template;
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
        config.setThinkingEnabled(true);
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

    private void clearDefaultModels(Long excludeId) {
        modelRepository.selectList(new LambdaQueryWrapper<AiModelConfig>().eq(AiModelConfig::getDefaultModel, true))
                .forEach(item -> {
                    if (excludeId == null || !excludeId.equals(item.getId())) {
                        item.setDefaultModel(false);
                        modelRepository.updateById(item);
                    }
                });
    }

    private void clearDefaultPrompts(String scene, Long excludeId) {
        promptRepository.selectList(new LambdaQueryWrapper<PromptTemplate>()
                        .eq(PromptTemplate::getScene, scene)
                        .eq(PromptTemplate::getDefaultTemplate, true))
                .forEach(item -> {
                    if (excludeId == null || !excludeId.equals(item.getId())) {
                        item.setDefaultTemplate(false);
                        promptRepository.updateById(item);
                    }
                });
    }

    private boolean isLlm(AiModelConfig config) {
        String explicitType = config.getModelType();
        if (StringUtils.hasText(explicitType)) {
            return "LLM".equalsIgnoreCase(explicitType);
        }
        String modelName = config.getModelName() == null ? "" : config.getModelName().toLowerCase(Locale.ROOT);
        return !modelName.contains("embedding");
    }
}
