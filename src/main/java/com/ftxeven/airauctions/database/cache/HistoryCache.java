package com.ftxeven.airauctions.database.cache;

import com.ftxeven.airauctions.database.query.FacetCounts;
import com.ftxeven.airauctions.database.query.HistoryQuery;
import com.ftxeven.airauctions.database.query.PageResult;
import com.ftxeven.airauctions.model.HistoryEntry;

import java.time.Duration;
import java.util.function.Supplier;

public final class HistoryCache {

    private static final Duration QUERY_TTL = Duration.ofSeconds(2);
    private static final int MAX_CACHED_QUERIES = 500;

    private final TtlCache<HistoryQuery, PageResult<HistoryEntry>> pages = new TtlCache<>(QUERY_TTL, MAX_CACHED_QUERIES);
    private final TtlCache<HistoryQuery, FacetCounts> facets = new TtlCache<>(QUERY_TTL, MAX_CACHED_QUERIES);

    public PageResult<HistoryEntry> queryPage(HistoryQuery query, Supplier<PageResult<HistoryEntry>> loader) {
        return pages.resolve(query, loader);
    }

    public FacetCounts queryFacets(HistoryQuery query, Supplier<FacetCounts> loader) {
        return facets.resolve(query, loader);
    }

    public void invalidateAll() {
        pages.clear();
        facets.clear();
    }
}