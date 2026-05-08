package com.knowflow.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum MessageRole {
    USER("USER"), ASSISTANT("ASSISTANT");

    @EnumValue
    private final String value;

    MessageRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
