package com.knowflow.common;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public record PageResponse<T>(List<T> list, long total, int pageNo, int pageSize) {
    public static <T> PageResponse<T> of(IPage<T> page) {
        return new PageResponse<>(page.getRecords(), page.getTotal(), Math.toIntExact(page.getCurrent()), Math.toIntExact(page.getSize()));
    }
}
