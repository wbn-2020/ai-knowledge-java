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
        Long kbCount,
        Long docCount,
        Long qaCount,
        LocalDateTime lastLoginTime,
        LocalDateTime createTime
) {
    public static UserVO from(User user) {
        return new UserVO(
                user.getId(), user.getUsername(), user.getEmail(), user.getNickname(),
                user.getAvatar(), user.getBio(), user.getStatus(), user.getRole(),
                user.getRole() == UserRole.ADMIN, 0L, 0L, 0L, null, user.getCreateTime()
        );
    }

    public static UserVO enrich(UserVO base, Long kbCount, Long docCount, Long qaCount, LocalDateTime lastLoginTime) {
        return new UserVO(base.id(), base.username(), base.email(), base.nickname(), base.avatar(), base.bio(),
                base.status(), base.role(), base.isAdmin(), kbCount, docCount, qaCount, lastLoginTime, base.createTime());
    }
}
