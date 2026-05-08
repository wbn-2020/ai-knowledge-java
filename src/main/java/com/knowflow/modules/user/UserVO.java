package com.knowflow.modules.user;

import com.knowflow.common.enums.UserRole;
import com.knowflow.common.enums.UserStatus;

import java.time.LocalDateTime;

public record UserVO(
        Long id,
        String username,
        String email,
        String nickname,
        String avatar,
        String bio,
        UserStatus status,
        UserRole role,
        LocalDateTime createTime
) {
    public static UserVO from(User user) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatar(),
                user.getBio(),
                user.getStatus(),
                user.getRole(),
                user.getCreateTime()
        );
    }
}
