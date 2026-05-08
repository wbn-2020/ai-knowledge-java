package com.knowflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum TaskStatus {
    PENDING("PENDING"), PROCESSING("PROCESSING"), SUCCESS("SUCCESS"), FAILED("FAILED");

    @EnumValue
    private final String value;

    TaskStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
