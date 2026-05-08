package com.knowflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum UserRole {
    USER("USER"), ADMIN("ADMIN");

    @EnumValue
    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
