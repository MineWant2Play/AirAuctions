package com.ftxeven.airauctions.economy;

import org.bukkit.OfflinePlayer;

public interface EconomyProvider {

    String id();

    String key();

    String displayName();

    boolean allowDecimals();

    boolean supportsOffline();

    double balance(OfflinePlayer player);

    boolean has(OfflinePlayer player, double amount);

    boolean withdraw(OfflinePlayer player, double amount);

    boolean deposit(OfflinePlayer player, double amount);

    String format(String amount);
}