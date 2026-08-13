package com.ftxeven.airauctions.database.repository;

import com.ftxeven.airauctions.model.PlayerData;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository {

    Optional<PlayerData> find(UUID uuid);

    Optional<PlayerData> findByName(String name);

    Map<UUID, PlayerData> findAll(Collection<UUID> uuids);

    List<UUID> findByNamePrefix(String prefix);

    void upsert(UUID uuid, String name, PlayerData.Skin skin);

    int adjustExtraSlots(UUID uuid, int delta);

    void addPendingEarnings(UUID uuid, String economy, double amount);

    void clearPendingEarnings(UUID uuid);

    void addPendingRefunds(UUID uuid, String economy, double amount);

    void clearPendingRefunds(UUID uuid);
}