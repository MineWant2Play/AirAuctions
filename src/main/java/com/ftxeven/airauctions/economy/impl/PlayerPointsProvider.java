package com.ftxeven.airauctions.economy.impl;

import com.ftxeven.airauctions.economy.EconomyProvider;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.OfflinePlayer;

public final class PlayerPointsProvider implements EconomyProvider {

    private final String id;
    private final String key;
    private final String displayName;
    private final String template;
    private final PlayerPointsAPI api;

    public PlayerPointsProvider(String id, String key, String displayName, String template, PlayerPointsAPI api) {
        this.id = id;
        this.key = key;
        this.displayName = displayName;
        this.template = template;
        this.api = api;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public boolean allowDecimals() {
        return false; // PlayerPoints only stores whole numbers
    }

    @Override
    public boolean supportsOffline() {
        return true;
    }

    @Override
    public double balance(OfflinePlayer player) {
        return api.look(player.getUniqueId());
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return balance(player) >= amount;
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        return api.take(player.getUniqueId(), (int) Math.round(amount));
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        return api.give(player.getUniqueId(), (int) Math.round(amount));
    }

    @Override
    public String format(String amount) {
        return template.replace("%amount%", amount);
    }
}