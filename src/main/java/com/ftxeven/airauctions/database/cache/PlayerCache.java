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

public final class PlayerCache {

    private static final String CHANNEL = "players";

    private final PlayerRepository repository;
    private final CacheSync sync;
    private final TtlCache<UUID, PlayerData> cache;

    public PlayerCache(PlayerRepository repository, CacheSync sync, Duration ttl) {
        this.repository = repository;
        this.sync = sync;
        this.cache = new TtlCache<>(ttl);
        sync.subscribe(CHANNEL, key -> dropLocal(UUID.fromString(key)));
    }

    public Optional<PlayerData> find(UUID uuid) {
        return cache.get(uuid).or(() -> {
            Optional<PlayerData> loaded = repository.find(uuid);
            loaded.ifPresent(this::warm);
            return loaded;
        });
    }

    public Map<UUID, PlayerData> findAll(Collection<UUID> uuids) {
        Map<UUID, PlayerData> result = new LinkedHashMap<>(uuids.size());
        List<UUID> missing = new ArrayList<>();

        for (UUID uuid : uuids) {
            cache.get(uuid).ifPresentOrElse(data -> result.put(uuid, data), () -> missing.add(uuid));
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
        for (PlayerData data : cache.liveValues()) {
            if (data.name().toLowerCase(Locale.ROOT).equals(nameLower)) {
                return Optional.of(data);
            }
        }

        Optional<PlayerData> loaded = repository.findByName(name);
        loaded.ifPresent(this::warm);
        return loaded;
    }

    public void warm(PlayerData data) {
        cache.put(data.uuid(), data);
    }

    public void invalidate(UUID uuid) {
        dropLocal(uuid);
        sync.publish(CHANNEL, uuid.toString());
    }

    public void invalidateAll() {
        cache.clear();
    }

    private void dropLocal(UUID uuid) {
        cache.remove(uuid);
    }
}