package com.parlandim.carteirama.common.web;

import com.parlandim.carteirama.common.error.ValidationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class Paging {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;

    private Paging() {
    }

    public static PageRequest of(Integer page, Integer limit, Sort sort) {
        int p = page == null ? 1 : page;
        int l = limit == null ? DEFAULT_LIMIT : limit;
        var errors = new ValidationException.Collector();
        if (p < 1) {
            errors.add("page", "out_of_range");
        }
        if (l < 1 || l > MAX_LIMIT) {
            errors.add("limit", "out_of_range");
        }
        errors.throwIfAny();
        return PageRequest.of(p - 1, l, sort);
    }
}
