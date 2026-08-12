package com.ftxeven.airauctions.economy.impl;

import com.ftxeven.airauctions.economy.EconomyProvider;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI;
import su.nightexpress.excellenteconomy.api.currency.ExcellentCurrency;

public final class ExcellentEconomyProvider implements EconomyProvider {

    private final String id;
    private final String key;
    private final String displayName;
    private final String template;
    private final boolean allowDecimals;
    private final ExcellentEconomyAPI api;
    private final ExcellentCurrency currency;

    public ExcellentEconomyProvider(String id, String key, String displayName, String template, boolean allowDecimals,
                                    ExcellentEconomyAPI api, ExcellentCurrency currency) {
        this.id = id;
        this.key = key;
        this.displayName = displayName;
        this.template = template;
        this.allowDecimals = allowDecimals;
        this.api = api;
        this.currency = currency;
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
        return allowDecimals;
    }

    @Override
    public boolean supportsOffline() {
        return false;
    }

    @Override
    public double balance(OfflinePlayer player) {
        Player online = player.getPlayer();
        return online != null ? api.getBalance(online, currency) : 0;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return balance(player) >= amount;
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        Player online = player.getPlayer();
        return online != null && has(player, amount) && api.withdraw(online, currency, amount);
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        Player online = player.getPlayer();
        return online != null && api.deposit(online, currency, amount);
    }

    @Override
    public String format(String amount) {
        return template.replace("%amount%", amount);
    }
}