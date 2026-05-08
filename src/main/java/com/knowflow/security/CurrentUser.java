package com.knowflow.security;

import com.knowflow.common.enums.UserRole;

public record CurrentUser(Long id, String username, UserRole role) {
}
