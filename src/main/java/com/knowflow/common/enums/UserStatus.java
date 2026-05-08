package com.knowflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum UserStatus {
    ENABLED("ENABLED"), DISABLED("DISABLED");

    @EnumValue
    private final String value;

    UserStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
