package com.knowflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowflow.common.BaseEntity;

@TableName("chat_message_feedback")
public class ChatMessageFeedback extends BaseEntity {
    @TableField("user_id")
    private Long userId;
    @TableField("session_id")
    private Long sessionId;
    @TableField("message_id")
    private Long messageId;
    @TableField("feedback_type")
    private String feedbackType;
    private String reason;
    private String remark;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public String getFeedbackType() { return feedbackType; }
    public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
