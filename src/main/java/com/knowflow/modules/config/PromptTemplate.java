package com.knowflow.modules.config;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowflow.common.BaseEntity;

@TableName("prompt_template")
public class PromptTemplate extends BaseEntity {
    private String code;
    private String name;
    private String content;
    private String scene;
    private Boolean enabled;
    private Boolean defaultTemplate;
    private String description;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getDefaultTemplate() { return defaultTemplate; }
    public void setDefaultTemplate(Boolean defaultTemplate) { this.defaultTemplate = defaultTemplate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
