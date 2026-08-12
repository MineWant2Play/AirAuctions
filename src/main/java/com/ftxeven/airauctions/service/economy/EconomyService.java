package com.ftxeven.airauctions.service.economy;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.config.ExpansionsConfig;
import com.ftxeven.airauctions.economy.EconomyProvider;
import com.ftxeven.airauctions.economy.EconomyRegistry;
import com.ftxeven.airauctions.permission.Permissions;
import com.ftxeven.airauctions.service.Eligibility;
import com.ftxeven.airauctions.util.MiniText;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

public final class EconomyService {

    private final EconomyRegistry economy;
    private final ConfigManager configs;
    private final PriceValidator prices;
    private final ChargeCalculator charges;

    public EconomyService(EconomyRegistry economy, ConfigManager configs) {
        this.economy = economy;
        this.configs = configs;
        this.prices = new PriceValidator(configs);
        this.charges = new ChargeCalculator(configs);
    }

    // Provider resolution

    public Optional<EconomyProvider> get(String economyId) {
        return economy.get(economyId);
    }

    public Optional<EconomyProvider> defaultProvider() {
        return economy.get(configs.expansions().economy().defaultCurrency());
    }

    // matches a provider by its user-typable 'key' (e.g. "Money") - used for the [economy]
    // command argument, distinct from the internal storage 'id' (e.g. "vault")
    public Optional<EconomyProvider> findByKey(String key) {
        String needle = key.trim();
        return economy.providers().values().stream()
                .filter(provider -> provider.key().equalsIgnoreCase(needle))
                .findFirst();
    }

    public List<String> economyKeys() {
        return economy.providers().values().stream().map(EconomyProvider::key).toList();
    }

    public boolean multiCurrency() {
        return configs.expansions().economy().multiCurrency();
    }

    public String displayName(String economyId) {
        return economy.get(economyId).map(EconomyProvider::displayName).orElse(economyId);
    }

    // Formatting

    public String format(String economyId, double amount) {
        Optional<EconomyProvider> provider = economy.get(economyId);
        ExpansionsConfig.Economy config = configs.expansions().economy();
        boolean allowDecimals = provider.map(EconomyProvider::allowDecimals).orElse(true);
        String number = NumberConverter.format(amount, config.numberFormat(), config.numberFormatSuffixes(), allowDecimals);
        return provider.map(p -> p.format(number)).orElse(number);
    }

    // Writes %key%/%key_plain% for a plain amount that's always present
    public void formatInto(Map<String, String> placeholders, String key, String economyId, double amount) {
        put(placeholders, key, format(economyId, amount));
    }

    // Writes %key%/%key_plain% for a fee/tax charge
    public void formatInto(Map<String, String> placeholders, String key, String economyId, ChargeResult charge) {
        formatInto(placeholders, key, economyId, charge.amount(), charge.kind());
    }

    // Writes %key%/%key_plain% for a fee/tax amount recomputed outside a ChargeResult
    public void formatInto(Map<String, String> placeholders, String key, String economyId, double amount, ChargeKind kind) {
        OptionalDouble present = amount <= 0 ? OptionalDouble.empty() : OptionalDouble.of(amount);
        formatInto(placeholders, key, economyId, present, kind.emptyPlaceholderKey());
    }

    // Writes %key%/%key_plain% for an amount that might not exist yet
    public void formatInto(Map<String, String> placeholders, String key, String economyId, OptionalDouble amount, String emptyLangKey) {
        put(placeholders, key, amount.isPresent() ? format(economyId, amount.getAsDouble()) : emptyText(emptyLangKey));
    }

    // Writes %economy% (+ %economy_plain%) and %economy_id% for a resolved provider
    public void formatEconomy(Map<String, String> placeholders, EconomyProvider provider) {
        formatEconomy(placeholders, provider.id(), provider.displayName());
    }

    // Same, resolved by economy id
    // falls back to the raw id as its own display text if that provider is no longer active
    public void formatEconomy(Map<String, String> placeholders, String economyId) {
        formatEconomy(placeholders, economyId, displayName(economyId));
    }

    private void formatEconomy(Map<String, String> placeholders, String economyId, String displayName) {
        placeholders.put("economy_id", economyId);
        put(placeholders, "economy", displayName);
    }

    private void put(Map<String, String> placeholders, String key, String formatted) {
        placeholders.put(key, formatted);
        placeholders.put(key + "_plain", MiniText.plain(formatted));
    }

    private String emptyText(String langKey) {
        return configs.lang().get(langKey).getFirst();
    }

    // Price validation

    public OptionalDouble parsePrice(String raw, EconomyProvider provider) {
        return prices.parse(raw, provider);
    }

    public double minPrice() {
        return prices.min();
    }

    public double maxPrice() {
        return prices.max();
    }

    public double minPartialPrice() {
        return prices.minPartial();
    }

    public boolean meetsMinPrice(double price) {
        return prices.meetsMin(price);
    }

    public boolean meetsMaxPrice(double price) {
        return prices.meetsMax(price);
    }

    public boolean isPriceInRange(double price) {
        return prices.isInRange(price);
    }

    public Eligibility eligibleForPrice(double price, EconomyProvider provider) {
        if (isPriceInRange(price)) {
            return Eligibility.eligible();
        }

        boolean belowMin = !meetsMinPrice(price);
        Map<String, String> placeholders = new HashMap<>();
        formatInto(placeholders, belowMin ? "min" : "max", provider.id(), belowMin ? minPrice() : maxPrice());
        return Eligibility.denied(belowMin ? "errors.economy.price-too-low" : "errors.economy.price-too-high", placeholders);
    }

    // Charges

    public ChargeResult fee(Permissible seller, EconomyProvider provider, double price) {
        return charges.fee(seller, provider, price);
    }

    public ChargeResult tax(Permissible seller, EconomyProvider provider, double price) {
        return charges.tax(seller, provider, price);
    }

    public boolean charge(Player seller, EconomyProvider provider, ChargeResult charge) {
        return charge.waived() || provider.withdraw(seller, charge.amount());
    }

    public Eligibility eligibleForFee(Player seller, EconomyProvider provider, ChargeResult fee) {
        if (fee.waived() || provider.has(seller, fee.amount())) {
            return Eligibility.eligible();
        }

        Map<String, String> placeholders = new HashMap<>();
        formatInto(placeholders, "fee", provider.id(), fee);
        return Eligibility.denied("errors.economy.insufficient-funds-fee", placeholders);
    }

    public double finalizeTax(EconomyProvider provider, double taxRate, double storedTax, double finalPrice) {
        return charges.finalizeTax(provider, taxRate, storedTax, finalPrice);
    }

    // Internal types

    public enum ChargeKind {
        FEE(Permissions.Bypass.FEE, "placeholders.empty.fee"),
        TAX(Permissions.Bypass.TAX, "placeholders.empty.tax");

        private final String bypassPermission;
        private final String emptyPlaceholderKey;

        ChargeKind(String bypassPermission, String emptyPlaceholderKey) {
            this.bypassPermission = bypassPermission;
            this.emptyPlaceholderKey = emptyPlaceholderKey;
        }

        public String bypassPermission() { return bypassPermission; }

        public String emptyPlaceholderKey() { return emptyPlaceholderKey; }
    }

    public record ChargeResult(ChargeKind kind, double amount, double rate, boolean waived) {
        public static ChargeResult waived(ChargeKind kind) {
            return new ChargeResult(kind, 0, -1, true);
        }
    }
}