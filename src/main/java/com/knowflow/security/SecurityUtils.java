package com.knowflow.security;

import com.knowflow.common.BusinessException;
import com.knowflow.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().id();
    }

    public static CurrentUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw BusinessException.unauthorized("请先登录");
        }
        return currentUser;
    }

    public static void requireAdmin() {
        if (getCurrentUser().role() != UserRole.ADMIN) {
            throw BusinessException.forbidden("无权限访问后台管理功能");
        }
    }
}
