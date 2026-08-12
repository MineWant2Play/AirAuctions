package com.ftxeven.airauctions.economy.impl;

import com.ftxeven.airauctions.config.ExpansionsConfig.PlaceholderSettings;
import com.ftxeven.airauctions.economy.EconomyProvider;
import com.ftxeven.airauctions.util.Scheduler;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public final class PlaceholderProvider implements EconomyProvider {

    private final String id;
    private final String displayName;
    private final String template;
    private final boolean allowDecimals;
    private final PlaceholderSettings settings;

    public PlaceholderProvider(String id, String displayName, String template, boolean allowDecimals, PlaceholderSettings settings) {
        this.id = id;
        this.displayName = displayName;
        this.template = template;
        this.allowDecimals = allowDecimals;
        this.settings = settings;
    }

    @Override
    public String id() {
        return id;
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
        return true;
    }

    @Override
    public double balance(OfflinePlayer player) {
        String resolved = PlaceholderAPI.setPlaceholders(player, settings.balancePlaceholder());
        if (resolved == null) {
            return 0;
        }
        try {
            return Double.parseDouble(resolved.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return balance(player) >= amount;
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        return has(player, amount) && dispatch(settings.takeCommand(), player, amount);
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        return dispatch(settings.giveCommand(), player, amount);
    }

    @Override
    public String format(String amount) {
        return template.replace("%amount%", amount);
    }

    private boolean dispatch(String command, OfflinePlayer player, double amount) {
        String name = player.getName();
        if (name == null) {
            return false;
        }
        String parsed = command.replace("%player%", name).replace("%amount%", formatAmount(amount));
        Scheduler.runGlobal(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed));
        return true;
    }

    private String formatAmount(double amount) {
        return allowDecimals ? String.valueOf(amount) : String.valueOf((long) amount);
    }
}