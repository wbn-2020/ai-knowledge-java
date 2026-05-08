package com.knowflow.modules.log;

public record AlertVO(
        String type,
        String level,
        String message,
        long total,
        long failed,
        double failureRate
) {
}
