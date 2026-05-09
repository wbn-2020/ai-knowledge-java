package com.knowflow.vo;

import com.knowflow.entity.User;
import com.knowflow.enums.UserRole;
import com.knowflow.enums.UserStatus;
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
        Boolean isAdmin,
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
                user.getRole() == UserRole.ADMIN,
                user.getCreateTime()
        );
    }
}
