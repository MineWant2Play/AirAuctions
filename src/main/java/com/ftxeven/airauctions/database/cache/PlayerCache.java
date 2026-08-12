package com.ftxeven.airauctions.database.cache;

import com.ftxeven.airauctions.database.cache.sync.CacheSync;
import com.ftxeven.airauctions.database.repository.PlayerRepository;
import com.ftxeven.airauctions.model.PlayerData;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerCache {

    private static final String CHANNEL = "players";

    private final PlayerRepository repository;
    private final CacheSync sync;
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();
    private final long ttlNanos;

    public PlayerCache(PlayerRepository repository, CacheSync sync, Duration ttl) {
        this.repository = repository;
        this.sync = sync;
        this.ttlNanos = ttl.toNanos();
        sync.subscribe(CHANNEL, key -> dropLocal(UUID.fromString(key)));
    }

    public Optional<PlayerData> find(UUID uuid) {
        Entry cached = entries.get(uuid);
        if (cached != null) {
            if (!cached.isExpired()) {
                return Optional.of(cached.data());
            }
            entries.remove(uuid, cached);
        }

        Optional<PlayerData> loaded = repository.find(uuid);
        loaded.ifPresent(this::warm);
        return loaded;
    }

    public Map<UUID, PlayerData> findAll(Collection<UUID> uuids) {
        Map<UUID, PlayerData> result = new LinkedHashMap<>(uuids.size());
        List<UUID> missing = new ArrayList<>();

        for (UUID uuid : uuids) {
            Entry cached = entries.get(uuid);
            if (cached != null && !cached.isExpired()) {
                result.put(uuid, cached.data());
            } else {
                missing.add(uuid);
            }
        }

        if (!missing.isEmpty()) {
            Map<UUID, PlayerData> loaded = repository.findAll(missing);
            loaded.values().forEach(this::warm);
            result.putAll(loaded);
        }

        return result;
    }

    public Optional<PlayerData> findByName(String name) {
        String nameLower = name.toLowerCase(Locale.ROOT);
        for (PlayerData data : liveValues()) {
            if (data.name().toLowerCase(Locale.ROOT).equals(nameLower)) {
                return Optional.of(data);
            }
        }

        Optional<PlayerData> loaded = repository.findByName(name);
        loaded.ifPresent(this::warm);
        return loaded;
    }

    public void warm(PlayerData data) {
        entries.put(data.uuid(), new Entry(data, System.nanoTime() + ttlNanos));
    }

    public void invalidate(UUID uuid) {
        dropLocal(uuid);
        sync.publish(CHANNEL, uuid.toString());
    }

    public void invalidateAll() {
        entries.clear();
    }

    private void dropLocal(UUID uuid) {
        entries.remove(uuid);
    }

    private List<PlayerData> liveValues() {
        List<PlayerData> live = new ArrayList<>(entries.size());
        for (Map.Entry<UUID, Entry> entry : entries.entrySet()) {
            if (entry.getValue().isExpired()) {
                entries.remove(entry.getKey(), entry.getValue());
            } else {
                live.add(entry.getValue().data());
            }
        }
        return live;
    }

    private record Entry(PlayerData data, long expiresAtNanos) {
        boolean isExpired() {
            return System.nanoTime() >= expiresAtNanos;
        }
    }
}