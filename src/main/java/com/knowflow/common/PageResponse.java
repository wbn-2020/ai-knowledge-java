package com.knowflow.common;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(List<T> list, long total, int pageNo, int pageSize) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(), page.getNumber() + 1, page.getSize());
    }
}
