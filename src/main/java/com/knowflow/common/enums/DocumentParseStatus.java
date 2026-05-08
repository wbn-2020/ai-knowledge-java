package com.knowflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum DocumentParseStatus {
    PENDING("PENDING"), PARSING("PARSING"), SUCCESS("SUCCESS"), FAILED("FAILED");

    @EnumValue
    private final String value;

    DocumentParseStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
