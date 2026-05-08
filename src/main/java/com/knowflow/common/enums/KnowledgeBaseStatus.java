package com.knowflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum KnowledgeBaseStatus {
    NORMAL("NORMAL"), PROCESSING("PROCESSING"), FAILED("FAILED"), DISABLED("DISABLED");

    @EnumValue
    private final String value;

    KnowledgeBaseStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
