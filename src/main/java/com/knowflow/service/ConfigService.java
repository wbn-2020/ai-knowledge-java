package com.knowflow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowflow.common.BusinessException;
import com.knowflow.dto.AiModelConfigRequest;
import com.knowflow.dto.PromptConfigRequest;
import com.knowflow.dto.SystemSettingsSaveRequest;
import com.knowflow.entity.AiModelConfig;
import com.knowflow.entity.PromptTemplate;
import com.knowflow.entity.SystemConfig;
import com.knowflow.infrastructure.ai.LlmClient;
import com.knowflow.mapper.AiModelConfigRepository;
import com.knowflow.mapper.PromptTemplateRepository;
import com.knowflow.mapper.SystemConfigRepository;
import com.knowflow.security.SecurityUtils;
import com.knowflow.vo.ConfigVO;
import com.knowflow.vo.PromptConfigVO;
import com.knowflow.vo.SystemSettingsVO;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ConfigService {
    private static final String FALLBACK_RAG_PROMPT = """
            你是 KnowFlow AI 的知识库问答助手。请严格根据【知识库引用内容】回答用户问题。
            规则：
            1. 只能使用【知识库引用内容】中的信息回答。
            2. 如果引用内容不足以回答问题，请直接说明“当前知识库未找到相关依据”，不要编造。
            3. 不要引入引用内容之外的信息，不要使用外部常识扩展。
            4. 回答要清晰、准确、简洁。
            5. 如果引用内容之间存在冲突，请说明冲突点。
            """;

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);
    private final AiModelConfigRepository modelRepository;
    private final PromptTemplateRepository promptRepository;
    private final SystemConfigRepository systemRepository;
    private final LlmClient llmClient;
    private final OperationLogService operationLogService;

    public ConfigService(AiModelConfigRepository modelRepository,
                         PromptTemplateRepository promptRepository,
                         SystemConfigRepository systemRepository,
                         LlmClient llmClient,
                         OperationLogService operationLogService) {
        this.modelRepository = modelRepository;
        this.promptRepository = promptRepository;
        this.systemRepository = systemRepository;
        this.llmClient = llmClient;
        this.operationLogService = operationLogService;
    }

    public List<ConfigVO> models() {
        SecurityUtils.requireAdmin();
        return modelRepository.selectList(new LambdaQueryWrapper<AiModelConfig>()
                        .orderByDesc(AiModelConfig::getCreateTime))
                .stream().map(ConfigVO::from).toList();
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
        operationLogService.record("SAVE_MODEL_CONFIG", "AI_MODEL_CONFIG", config.getId(), config.getName());
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
        operationLogService.record("UPDATE_MODEL_CONFIG", "AI_MODEL_CONFIG", config.getId(), config.getName());
        return ConfigVO.from(config);
    }

    @Transactional
    public void deleteModel(Long id) {
        SecurityUtils.requireAdmin();
        AiModelConfig config = requireModel(id);
        modelRepository.deleteById(id);
        operationLogService.record("DELETE_MODEL_CONFIG", "AI_MODEL_CONFIG", id, config.getName());
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

    public List<PromptConfigVO> prompts() {
        SecurityUtils.requireAdmin();
        return promptRepository.selectList(new LambdaQueryWrapper<PromptTemplate>()
                        .orderByDesc(PromptTemplate::getCreateTime))
                .stream().map(PromptConfigVO::from).toList();
    }

    public PromptConfigVO promptDetail(Long id) {
        SecurityUtils.requireAdmin();
        return PromptConfigVO.from(requirePrompt(id));
    }

    @Transactional
    public PromptConfigVO savePrompt(PromptConfigRequest request) {
        SecurityUtils.requireAdmin();
        PromptTemplate template = new PromptTemplate();
        fillPrompt(template, request);
        promptRepository.insert(template);
        operationLogService.record("SAVE_PROMPT", "PROMPT_TEMPLATE", template.getId(), template.getName());
        return PromptConfigVO.from(template);
    }

    @Transactional
    public PromptConfigVO updatePrompt(Long id, PromptConfigRequest request) {
        SecurityUtils.requireAdmin();
        PromptTemplate template = requirePrompt(id);
        fillPrompt(template, request);
        promptRepository.updateById(template);
        operationLogService.record("UPDATE_PROMPT", "PROMPT_TEMPLATE", id, template.getName());
        return PromptConfigVO.from(template);
    }

    @Transactional
    public PromptConfigVO setPromptEnabled(Long id, boolean enabled) {
        SecurityUtils.requireAdmin();
        PromptTemplate template = requirePrompt(id);
        template.setEnabled(enabled);
        promptRepository.updateById(template);
        operationLogService.record("SET_PROMPT_ENABLED", "PROMPT_TEMPLATE", id, String.valueOf(enabled));
        return PromptConfigVO.from(template);
    }

    @Transactional
    public void deletePrompt(Long id) {
        SecurityUtils.requireAdmin();
        PromptTemplate template = requirePrompt(id);
        if (Boolean.TRUE.equals(template.getDefaultTemplate())) {
            throw BusinessException.badRequest("default prompt template cannot be deleted");
        }
        promptRepository.deleteById(id);
        operationLogService.record("DELETE_PROMPT", "PROMPT_TEMPLATE", id, template.getName());
    }

    public String defaultRagPrompt() {
        PromptTemplate template = promptRepository.selectOne(new LambdaQueryWrapper<PromptTemplate>()
                .in(PromptTemplate::getScene, "RAG", "QA")
                .eq(PromptTemplate::getEnabled, true)
                .orderByDesc(PromptTemplate::getUpdateTime)
                .last("limit 1"));
        if (template == null || !StringUtils.hasText(template.getContent())) {
            log.warn("No enabled RAG/QA prompt found, using fallback prompt");
            return FALLBACK_RAG_PROMPT;
        }
        return template.getContent();
    }

    public String promptBySceneOrDefault(String scene, String fallback) {
        if (!StringUtils.hasText(scene)) {
            return fallback;
        }
        PromptTemplate template = promptRepository.selectOne(new LambdaQueryWrapper<PromptTemplate>()
                .eq(PromptTemplate::getScene, scene)
                .eq(PromptTemplate::getEnabled, true)
                .orderByDesc(PromptTemplate::getUpdateTime)
                .last("limit 1"));
        if (template == null || !StringUtils.hasText(template.getContent())) {
            return fallback;
        }
        return template.getContent();
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

    public SystemSettingsVO systemSettings() {
        SecurityUtils.requireAdmin();
        int maxFileSize = intConfig("upload.maxFileSizeMb", 20);
        String allowedTypes = strConfig("upload.allowedTypes", "PDF,DOCX,TXT,MD");
        int chunkSize = intConfig("rag.chunkSize", 1000);
        int chunkOverlap = intConfig("rag.chunkOverlap", 100);
        int topK = intConfig("rag.topK", 5);
        double similarityThreshold = doubleConfig("rag.similarityThreshold",
                doubleConfig("rag.minScore", 0.55));
        int contextMaxLength = intConfig("rag.contextMaxLength", 4000);
        String platformName = strConfig("platform.name", "KnowFlow AI");
        String adminEmail = strConfig("platform.adminEmail", "");
        return new SystemSettingsVO(maxFileSize,
                Arrays.stream(allowedTypes.split(",")).map(String::trim).filter(StringUtils::hasText).toList(),
                chunkSize, chunkOverlap, topK, similarityThreshold, contextMaxLength, platformName, adminEmail);
    }

    @Transactional
    public SystemSettingsVO saveSystemSettings(SystemSettingsSaveRequest request) {
        SecurityUtils.requireAdmin();
        saveSystemConfigValue("upload.maxFileSizeMb", String.valueOf(request.maxFileSize()), "Max upload file size MB");
        saveSystemConfigValue("upload.allowedTypes", String.join(",", request.allowedTypes()), "Allowed upload file types");
        saveSystemConfigValue("rag.chunkSize", String.valueOf(request.chunkSize()), "RAG chunk size");
        saveSystemConfigValue("rag.chunkOverlap", String.valueOf(request.chunkOverlap()), "RAG chunk overlap");
        saveSystemConfigValue("rag.topK", String.valueOf(request.topK()), "RAG topK");
        saveSystemConfigValue("rag.similarityThreshold", String.valueOf(request.similarityThreshold()), "RAG similarity threshold");
        saveSystemConfigValue("rag.minScore", String.valueOf(request.similarityThreshold()), "RAG similarity threshold (legacy key)");
        int contextMaxLength = request.contextMaxLength() == null ? 4000 : request.contextMaxLength();
        saveSystemConfigValue("rag.contextMaxLength", String.valueOf(contextMaxLength), "RAG max context length");
        saveSystemConfigValue("platform.name", request.platformName(), "Platform name");
        saveSystemConfigValue("platform.adminEmail", request.adminEmail() == null ? "" : request.adminEmail(), "Platform admin email");
        operationLogService.record("SAVE_SYSTEM_CONFIG", "SYSTEM_CONFIG", null, "save structured settings");
        return systemSettings();
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

    private void fillPrompt(PromptTemplate template, PromptConfigRequest request) {
        String type = request.type().toUpperCase(Locale.ROOT);
        if (!Set.of("RAG", "QA", "SUMMARY", "KEYWORD", "TITLE", "DOCUMENT_SUMMARY", "KB_SUMMARY", "KEYWORD_EXTRACT").contains(type)) {
            throw BusinessException.badRequest("invalid prompt type");
        }
        String normalizedType = "QA".equals(type) ? "RAG" : type;
        template.setName(request.name());
        template.setCode(normalizedType + "_" + request.name().replaceAll("\\s+", "_").toUpperCase(Locale.ROOT));
        template.setContent(request.content());
        template.setScene(normalizedType);
        template.setEnabled(request.enabled() == null || request.enabled());
        template.setDescription(request.name());
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

    private void saveSystemConfigValue(String key, String value, String description) {
        SystemConfig config = systemRepository.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key)
                .last("limit 1"));
        if (config == null) {
            config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(description);
            systemRepository.insert(config);
            return;
        }
        config.setConfigValue(value);
        config.setDescription(description);
        systemRepository.updateById(config);
    }

    private int intConfig(String key, int fallback) {
        String value = strConfig(key, null);
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private double doubleConfig(String key, double fallback) {
        String value = strConfig(key, null);
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String strConfig(String key, String fallback) {
        SystemConfig config = systemRepository.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key)
                .last("limit 1"));
        return config == null ? fallback : config.getConfigValue();
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
