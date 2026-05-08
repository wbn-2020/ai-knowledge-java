package com.knowflow.modules.user;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowflow.common.BaseEntity;
import com.knowflow.common.enums.UserRole;
import com.knowflow.common.enums.UserStatus;

@TableName("sys_user")
public class User extends BaseEntity {
    private String username;

    private String email;

    private String password;

    private String nickname;

    private String avatar;

    private String bio;

    private UserStatus status = UserStatus.ENABLED;

    private UserRole role = UserRole.USER;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
}
