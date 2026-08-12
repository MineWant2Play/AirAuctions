package com.ftxeven.airauctions.service.simulation;

import com.ftxeven.airauctions.service.player.PlayerService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerPool {

    static final String NAME_PREFIX = "AA_sim_";

    // default minecraft skins
    private static final List<String> TEXTURES = List.of(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDZhY2QwNmU4NDgzYjE3NmU4ZWEzOWZjMTJmZTEwNWViM2EyYTQ5NzBmNTEwMDA1N2U5ZDg0ZDRiNjBiZGZhNyJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzFmNDc3ZWIxYTdiZWVlNjMxYzJjYTY0ZDA2ZjhmNjhmYTkzYTMzODZkMDQ0NTJhYjI3ZjQzYWNkZjFiNjBjYiJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTNiZDE2MDc5Zjc2NGNkNTQxZTA3MmU4ODhmZTQzODg1ZTcxMWY5ODY1ODMyM2RiMGY5YTYwNDVkYTkxZWU3YSJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2NiM2JhNTJkZGQ1Y2M4MmMwYjA1MGMzZjkyMGY4N2RhMzZhZGQ4MDE2NTg0NmY0NzkwNzk2NjM4MDU0MzNkYiJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmVjZTcwMTdiMWJiMTM5MjZkMTE1ODg2NGIyODNiOGI5MzAyNzFmODBhOTA0ODJmMTc0Y2NhNmExN2U4ODIzNiJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmMxNjBmYmQxNmFkYmM0YmZmMjQwOWU3MDE4MGQ5MTEwMDJhZWJjZmE4MTFlYjZlYzNkMTA0MDc2MWFlYTZkZCJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGMwNWFiOWUwN2IzNTA1ZGMzZWMxMTM3MGMzYmRjZTU1NzBhZDJmYjJiNTYyZTliOWRkOWNmMjcxZjgxYWE0NCJ9fX0=",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTVjZGMzMjQzYjIxNTNhYjI4YTE1OTg2MWJlNjQzYTRmYzFlM2MxN2QyOTFjZGQzZTU3YTdmMzcwYWQ2NzZmMyJ9fX0="
    );

    private static final Map<UUID, SyntheticSkin> SKINS_BY_UUID = new ConcurrentHashMap<>();
    private static final Map<String, SyntheticSkin> SKINS_BY_NAME = new ConcurrentHashMap<>();

    private final PlayerService players;

    PlayerPool(PlayerService players) {
        this.players = players;
    }

    List<UUID> ensure(int size) {
        List<UUID> pool = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID uuid = idFor(i);
            pool.add(uuid);
            String name = NAME_PREFIX + i;
            if (players.find(uuid).isEmpty()) {
                players.registerSynthetic(uuid, name);
            }
            bakeSkin(uuid, name, i);
        }
        return pool;
    }

    public static Optional<SyntheticSkin> skinFor(UUID uuid) {
        return Optional.ofNullable(SKINS_BY_UUID.get(uuid));
    }

    public static Optional<SyntheticSkin> skinFor(String name) {
        return Optional.ofNullable(SKINS_BY_NAME.get(name));
    }

    public static boolean isSynthetic(UUID uuid) {
        return SKINS_BY_UUID.containsKey(uuid);
    }

    private static void bakeSkin(UUID uuid, String name, int index) {
        SyntheticSkin skin = SKINS_BY_UUID.computeIfAbsent(uuid, id ->
                new SyntheticSkin(uuid, name, TEXTURES.get(Math.floorMod(index, TEXTURES.size()))));
        SKINS_BY_NAME.putIfAbsent(name, skin);
    }

    private static UUID idFor(int index) {
        return UUID.nameUUIDFromBytes(("airauctions-sim:" + index).getBytes(StandardCharsets.UTF_8));
    }

    public record SyntheticSkin(UUID uuid, String name, String textureValue) {}
}