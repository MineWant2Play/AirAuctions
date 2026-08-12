package com.ftxeven.airauctions.database.repository;

import com.ftxeven.airauctions.database.query.FacetCounts;
import com.ftxeven.airauctions.database.query.HistoryQuery;
import com.ftxeven.airauctions.database.query.PageResult;
import com.ftxeven.airauctions.database.query.TransactionKind;
import com.ftxeven.airauctions.model.HistoryEntry;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface HistoryRepository {

    PageResult<HistoryEntry> query(HistoryQuery query);

    FacetCounts facets(HistoryQuery query);

    // one auction purchase (even partial) or one concluded bid
    void append(HistoryEntry entry);

    Optional<HistoryEntry> find(String listingId, Instant completedAt);

    // drops the oldest rows for a player beyond maxEntries; non-positive = no-op
    void trim(UUID player, int maxEntries);

    Map<String, Double> sumForPlayer(UUID player, TransactionKind kind, @Nullable Instant since);

    Map<String, Double> sumGlobalVolume(@Nullable Instant since);

    // recomputes category/searchName for every stored row via the resolver
    int resyncMetadata(ListingMetadataResolver resolver);
}