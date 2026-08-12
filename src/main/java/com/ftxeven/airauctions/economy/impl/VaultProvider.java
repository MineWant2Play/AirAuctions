package com.ftxeven.airauctions.economy.impl;

import com.ftxeven.airauctions.economy.EconomyProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;

public final class VaultProvider implements EconomyProvider {

    private final String id;
    private final String displayName;
    private final String template;
    private final boolean allowDecimals;
    private final Economy vault;

    public VaultProvider(String id, String displayName, String template, boolean allowDecimals, Economy vault) {
        this.id = id;
        this.displayName = displayName;
        this.template = template;
        this.allowDecimals = allowDecimals;
        this.vault = vault;
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
        return vault.getBalance(player);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return vault.has(player, amount);
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        return vault.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        return vault.depositPlayer(player, amount).transactionSuccess();
    }

    @Override
    public String format(String amount) {
        return template.replace("%amount%", amount);
    }
}