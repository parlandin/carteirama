package com.parlandim.carteirama.common.web;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> data, Meta meta) {
    public record Meta(int page, int limit, long total) {
    }

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                new Meta(page.getNumber() + 1, page.getSize(), page.getTotalElements()));
    }
}
