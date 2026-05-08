package com.knowflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum EmbeddingStatus {
    PENDING("PENDING"), PROCESSING("PROCESSING"), SUCCESS("SUCCESS"), FAILED("FAILED");

    @EnumValue
    private final String value;

    EmbeddingStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
