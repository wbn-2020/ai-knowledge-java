package com.knowflow.modules.knowledge;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowflow.common.BaseEntity;
import com.knowflow.common.enums.KnowledgeBaseStatus;

@TableName("knowledge_base")
public class KnowledgeBase extends BaseEntity {
    private Long userId;

    private String name;

    private String description;

    private String icon;

    private String category;

    private KnowledgeBaseStatus status = KnowledgeBaseStatus.NORMAL;

    private Integer documentCount = 0;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public KnowledgeBaseStatus getStatus() { return status; }
    public void setStatus(KnowledgeBaseStatus status) { this.status = status; }
    public Integer getDocumentCount() { return documentCount; }
    public void setDocumentCount(Integer documentCount) { this.documentCount = documentCount; }
}
