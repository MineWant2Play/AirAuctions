package com.ftxeven.airauctions.service.listing;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.database.DatabaseManager;
import com.ftxeven.airauctions.database.query.FacetCounts;
import com.ftxeven.airauctions.database.query.HistoryQuery;
import com.ftxeven.airauctions.database.query.PageResult;
import com.ftxeven.airauctions.database.query.TransactionKind;
import com.ftxeven.airauctions.database.repository.ListingMetadataResolver;
import com.ftxeven.airauctions.model.HistoryEntry;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class HistoryService {

    private final DatabaseManager database;
    private final ConfigManager configs;

    public HistoryService(DatabaseManager database, ConfigManager configs) {
        this.database = database;
        this.configs = configs;
    }

    public PageResult<HistoryEntry> query(HistoryQuery query) {
        return database.history().query(query);
    }

    public FacetCounts facets(HistoryQuery query) {
        return database.history().facets(query);
    }

    public Optional<HistoryEntry> find(String listingId, Instant completedAt) {
        return database.history().find(listingId, completedAt);
    }

    public void record(HistoryEntry entry) {
        database.history().append(entry);

        int maxHistory = configs.main().listings().maxHistory();
        if (maxHistory <= 0) {
            return;
        }

        HistoryEntry.Info info = entry.info();
        database.history().trim(info.seller(), maxHistory);
        if (!info.buyer().equals(info.seller())) {
            database.history().trim(info.buyer(), maxHistory);
        }
    }

    public Map<String, Double> spent(UUID player, @Nullable Instant since) {
        return database.history().sumForPlayer(player, TransactionKind.SPENT, since);
    }

    public Map<String, Double> earned(UUID player, @Nullable Instant since) {
        return database.history().sumForPlayer(player, TransactionKind.EARNED, since);
    }

    public Map<String, Double> volume(@Nullable Instant since) {
        return database.history().sumGlobalVolume(since);
    }

    public int resyncMetadata(ListingMetadataResolver resolver) {
        return database.history().resyncMetadata(resolver);
    }

    public int deleteBySeller(Collection<UUID> sellers) {
        return database.history().deleteBySeller(sellers);
    }
}