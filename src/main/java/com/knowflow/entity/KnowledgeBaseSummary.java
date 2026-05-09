package com.knowflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowflow.common.BaseEntity;

@TableName("knowledge_base_summary")
public class KnowledgeBaseSummary extends BaseEntity {
    @TableField("user_id")
    private Long userId;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    private String summary;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
