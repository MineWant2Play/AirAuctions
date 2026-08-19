package com.ftxeven.airauctions.database.cache;

import com.ftxeven.airauctions.database.cache.sync.CacheSync;
import com.ftxeven.airauctions.database.query.FacetCounts;
import com.ftxeven.airauctions.database.query.ListingQuery;
import com.ftxeven.airauctions.database.query.PageResult;
import com.ftxeven.airauctions.database.repository.ListingRepository;
import com.ftxeven.airauctions.model.Listing;
import com.ftxeven.airauctions.model.ListingScope;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

public final class ListingCache {

    private static final String CHANNEL = "listings";

    private static final Duration QUERY_TTL = Duration.ofSeconds(2);
    private static final int MAX_CACHED_QUERIES = 500;

    private final CacheSync sync;
    private final TtlCache<String, Listing> byId;
    private final TtlCache<ListingQuery, PageResult<Listing>> pages;
    private final TtlCache<ListingQuery, FacetCounts> facets;
    private final ListingRepository repository;

    public ListingCache(ListingRepository repository, CacheSync sync, Duration ttl) {
        this.repository = repository;
        this.sync = sync;
        this.byId = new TtlCache<>(ttl);
        this.pages = new TtlCache<>(QUERY_TTL, MAX_CACHED_QUERIES);
        this.facets = new TtlCache<>(QUERY_TTL, MAX_CACHED_QUERIES);
        sync.subscribe(CHANNEL, this::dropLocal);
    }

    // Single listing lookup

    public Optional<Listing> find(String id) {
        return byId.get(id).or(() -> {
            Optional<Listing> loaded = repository.find(id);
            loaded.ifPresent(this::warm);
            return loaded;
        });
    }

    public void warm(Listing listing) {
        byId.put(listing.info().id(), listing);
    }

    public void invalidate(String id) {
        dropLocal(id);
        sync.publish(CHANNEL, id);
        invalidateQueries();
    }

    public void invalidateAll() {
        byId.clear();
        invalidateQueries();
    }

    private void dropLocal(String id) {
        byId.remove(id);
    }

    // Query result cache (browsing pages / facet counts)¿

    public PageResult<Listing> queryPage(ListingQuery query, Supplier<PageResult<Listing>> loader) {
        return pages.resolve(query, loader);
    }

    public FacetCounts queryFacets(ListingQuery query, Supplier<FacetCounts> loader) {
        return facets.resolve(query, loader);
    }

    public void invalidateQueries() {
        pages.clear();
        facets.clear();
    }

    // Validity

    public static boolean isExpired(Listing.Info info, Instant now) {
        return !now.isBefore(info.expiresAt());
    }

    public static boolean isPurged(Listing.Info info, long purgeDelaySeconds, Instant now) {
        if (purgeDelaySeconds < 0 || info.endedAt() == null) {
            return false;
        }
        return !now.isBefore(info.endedAt().plusSeconds(purgeDelaySeconds));
    }

    public static boolean isValid(Listing.Info info, ListingScope scope, long purgeDelaySeconds, Instant now) {
        return switch (scope) {
            case ACTIVE -> !isExpired(info, now);
            case EXPIRED, STORAGE -> !isPurged(info, purgeDelaySeconds, now);
        };
    }
}