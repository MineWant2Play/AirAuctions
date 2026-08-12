package com.ftxeven.airauctions.economy;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.config.ExpansionsConfig;
import com.ftxeven.airauctions.economy.impl.*;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI;
import su.nightexpress.excellenteconomy.api.currency.ExcellentCurrency;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class EconomyRegistry {

    private final JavaPlugin plugin;
    private final ConfigManager configs;
    private volatile Map<String, EconomyProvider> providers = Map.of();

    public EconomyRegistry(JavaPlugin plugin, ConfigManager configs) {
        this.plugin = plugin;
        this.configs = configs;
        reload();
    }

    public void reload() {
        Map<String, EconomyProvider> built = new LinkedHashMap<>();
        for (Map.Entry<String, ExpansionsConfig.Provider> entry : configs.expansions().economy().providers().entrySet()) {
            build(entry.getKey(), entry.getValue()).ifPresent(provider -> built.put(entry.getKey(), provider));
        }
        providers = Collections.unmodifiableMap(built);

        if (providers.isEmpty()) {
            plugin.getLogger().warning("No economy providers are active - selling and bidding will not work until at least one is enabled and hooks successfully");
            return;
        }

        String defaultCurrency = configs.expansions().economy().defaultCurrency();
        if (!providers.containsKey(defaultCurrency)) {
            plugin.getLogger().warning("default-currency '" + defaultCurrency + "' is not an active provider (disabled, or failed to hook)");
        }
    }

    public Map<String, EconomyProvider> providers() {
        return providers;
    }

    public Optional<EconomyProvider> get(String id) {
        return Optional.ofNullable(providers.get(id));
    }

    private Optional<EconomyProvider> build(String id, ExpansionsConfig.Provider providerConfig) {
        if (!providerConfig.enabled() || providerConfig.type() == null) {
            return Optional.empty();
        }

        try {
            return switch (providerConfig.type()) {
                case VAULT -> buildVault(id, providerConfig);
                case EXP -> Optional.of(new ExpProvider(id, providerConfig.displayName(), providerConfig.format(), providerConfig.allowDecimals()));
                case PLACEHOLDER -> buildPlaceholder(id, providerConfig);
                case PLAYERPOINTS -> buildPlayerPoints(id, providerConfig);
                case EXCELLENTECONOMY -> buildExcellentEconomy(id, providerConfig);
            };
        } catch (Throwable e) {
            plugin.getLogger().warning("Failed to hook economy provider '" + id + "': " + e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<EconomyProvider> buildVault(String id, ExpansionsConfig.Provider providerConfig) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
            plugin.getLogger().warning("Economy provider '" + id + "' is type VAULT but Vault is not installed, skipping");
            return Optional.empty();
        }

        RegisteredServiceProvider<Economy> registration = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            plugin.getLogger().warning("Economy provider '" + id + "' is type VAULT but no economy plugin is registered with Vault, skipping");
            return Optional.empty();
        }

        return Optional.of(new VaultProvider(id, providerConfig.displayName(), providerConfig.format(), providerConfig.allowDecimals(), registration.getProvider()));
    }

    private Optional<EconomyProvider> buildPlaceholder(String id, ExpansionsConfig.Provider providerConfig) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            plugin.getLogger().warning("Economy provider '" + id + "' is type PLACEHOLDER but PlaceholderAPI is not installed, skipping");
            return Optional.empty();
        }
        if (providerConfig.settings() == null) {
            return Optional.empty();
        }

        return Optional.of(new PlaceholderProvider(id, providerConfig.displayName(), providerConfig.format(), providerConfig.allowDecimals(), providerConfig.settings()));
    }

    private Optional<EconomyProvider> buildPlayerPoints(String id, ExpansionsConfig.Provider providerConfig) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("PlayerPoints")) {
            plugin.getLogger().warning("Economy provider '" + id + "' is type PLAYERPOINTS but PlayerPoints is not installed, skipping");
            return Optional.empty();
        }

        PlayerPointsAPI api = PlayerPoints.getInstance().getAPI();
        if (api == null) {
            plugin.getLogger().warning("Economy provider '" + id + "' is type PLAYERPOINTS but the PlayerPoints API is unavailable, skipping");
            return Optional.empty();
        }

        return Optional.of(new PlayerPointsProvider(id, providerConfig.displayName(), providerConfig.format(), api));
    }

    private Optional<EconomyProvider> buildExcellentEconomy(String id, ExpansionsConfig.Provider providerConfig) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("ExcellentEconomy")) {
            plugin.getLogger().warning("Economy provider '" + id + "' is type EXCELLENTECONOMY but ExcellentEconomy is not installed, skipping");
            return Optional.empty();
        }
        if (providerConfig.excellentEconomySettings() == null) {
            return Optional.empty(); // ExpansionsConfig already warned about the missing settings block
        }

        RegisteredServiceProvider<ExcellentEconomyAPI> registration = plugin.getServer().getServicesManager().getRegistration(ExcellentEconomyAPI.class);
        if (registration == null) {
            plugin.getLogger().warning("Economy provider '" + id + "' is type EXCELLENTECONOMY but its API service isn't registered, skipping");
            return Optional.empty();
        }

        ExcellentEconomyAPI api = registration.getProvider();
        String currencyId = providerConfig.excellentEconomySettings().currency();
        ExcellentCurrency currency = api.getCurrency(currencyId);
        if (currency == null) {
            plugin.getLogger().warning("Economy provider '" + id + "' references ExcellentEconomy currency '" + currencyId + "' which doesn't exist, skipping");
            return Optional.empty();
        }

        return Optional.of(new ExcellentEconomyProvider(id, providerConfig.displayName(), providerConfig.format(), providerConfig.allowDecimals(), api, currency));
    }
}