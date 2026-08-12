package com.ftxeven.airauctions.database.cache;

import com.ftxeven.airauctions.database.cache.sync.CacheSync;
import com.ftxeven.airauctions.database.repository.ListingRepository;
import com.ftxeven.airauctions.model.Listing;
import com.ftxeven.airauctions.model.ListingScope;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ListingCache {

    private static final String CHANNEL = "listings";

    private final ListingRepository repository;
    private final CacheSync sync;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final long ttlNanos;

    public ListingCache(ListingRepository repository, CacheSync sync, Duration ttl) {
        this.repository = repository;
        this.sync = sync;
        this.ttlNanos = ttl.toNanos();
        sync.subscribe(CHANNEL, this::dropLocal);
    }

    public Optional<Listing> find(String id) {
        Entry cached = entries.get(id);
        if (cached != null) {
            if (!cached.isExpired()) {
                return Optional.of(cached.listing());
            }
            entries.remove(id, cached);
        }

        Optional<Listing> loaded = repository.find(id);
        loaded.ifPresent(this::warm);
        return loaded;
    }

    public void warm(Listing listing) {
        entries.put(listing.info().id(), new Entry(listing, System.nanoTime() + ttlNanos));
    }

    public void invalidate(String id) {
        dropLocal(id);
        sync.publish(CHANNEL, id);
    }

    public void invalidateAll() {
        entries.clear();
    }

    private void dropLocal(String id) {
        entries.remove(id);
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

    private record Entry(Listing listing, long expiresAtNanos) {
        boolean isExpired() {
            return System.nanoTime() >= expiresAtNanos;
        }
    }
}