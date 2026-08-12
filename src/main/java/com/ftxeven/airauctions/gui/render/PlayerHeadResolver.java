package com.ftxeven.airauctions.gui.render;

import com.ftxeven.airauctions.core.gui.render.MaterialResolver;
import com.ftxeven.airauctions.model.PlayerData;
import com.ftxeven.airauctions.service.player.PlayerService;

import java.util.Optional;
import java.util.UUID;

public final class PlayerHeadResolver implements MaterialResolver.HeadResolver {

    private final PlayerService players;

    public PlayerHeadResolver(PlayerService players) {
        this.players = players;
    }

    @Override
    public Optional<MaterialResolver.CachedHead> byUuid(UUID uuid) {
        return players.find(uuid).map(PlayerHeadResolver::toHead);
    }

    @Override
    public Optional<MaterialResolver.CachedHead> byName(String name) {
        return players.findByName(name).map(PlayerHeadResolver::toHead);
    }

    private static MaterialResolver.CachedHead toHead(PlayerData data) {
        PlayerData.Skin skin = data.skin();
        return new MaterialResolver.CachedHead(data.uuid(), data.name(), skin.value(), skin.signature());
    }
}