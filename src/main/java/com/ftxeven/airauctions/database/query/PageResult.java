package com.ftxeven.airauctions.database.query;

import java.util.List;

// one page of results plus pagination/header metadata (%current%, %total%, %page%, %pages%)
public record PageResult<T>(List<T> items, int page, int totalPages, long totalResults) {

    public PageResult {
        items = List.copyOf(items);
    }

    public static <T> PageResult<T> empty(int page) {
        return new PageResult<>(List.of(), Math.max(1, page), 1, 0);
    }
}