package com.knowflow.vo;

public record AlertVO(
        String type,
        String level,
        String message,
        long total,
        long failed,
        double failureRate
) {
}
