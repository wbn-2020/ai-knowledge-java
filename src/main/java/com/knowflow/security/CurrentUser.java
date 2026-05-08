package com.knowflow.security;

import com.knowflow.enums.UserRole;

public record CurrentUser(Long id, String username, UserRole role) {
}
