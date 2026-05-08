package com.knowflow.modules.log;

import com.knowflow.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "login_log")
public class LoginLog extends BaseEntity {
    private Long userId;
    @Column(nullable = false, length = 128)
    private String account;
    @Column(nullable = false)
    private Boolean success;
    @Column(length = 255)
    private String message;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
