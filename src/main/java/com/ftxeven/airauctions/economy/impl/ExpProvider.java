package com.ftxeven.airauctions.economy.impl;

import com.ftxeven.airauctions.economy.EconomyProvider;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class ExpProvider implements EconomyProvider {

    private final String id;
    private final String key;
    private final String displayName;
    private final String template;
    private final boolean allowDecimals;

    public ExpProvider(String id, String key, String displayName, String template, boolean allowDecimals) {
        this.id = id;
        this.key = key;
        this.displayName = displayName;
        this.template = template;
        this.allowDecimals = allowDecimals;
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
        return online != null ? totalExperience(online) : 0;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return balance(player) >= amount;
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        Player online = player.getPlayer();
        if (online == null) {
            return false;
        }
        int current = totalExperience(online);
        int cost = (int) Math.round(amount);
        if (current < cost) {
            return false;
        }
        setTotalExperience(online, current - cost);
        return true;
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        Player online = player.getPlayer();
        if (online == null) {
            return false;
        }
        online.giveExp((int) Math.round(amount));
        return true;
    }

    @Override
    public String format(String amount) {
        return template.replace("%amount%", amount);
    }

    // Experience helpers

    private int totalExperience(Player player) {
        return experienceForLevel(player.getLevel()) + Math.round(player.getExp() * experienceToNextLevel(player.getLevel()));
    }

    private void setTotalExperience(Player player, int total) {
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);
        if (total > 0) {
            player.giveExp(total);
        }
    }

    private static int experienceForLevel(int level) {
        if (level <= 15) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (int) Math.round(2.5 * level * level - 40.5 * level + 360);
        }
        return (int) Math.round(4.5 * level * level - 162.5 * level + 2220);
    }

    private static int experienceToNextLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        }
        if (level <= 30) {
            return 5 * level - 38;
        }
        return 9 * level - 158;
    }
}