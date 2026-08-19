package com.ftxeven.airauctions.util;

import com.ftxeven.airauctions.economy.EconomyProvider;
import com.ftxeven.airauctions.service.economy.EconomyService;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

public final class PlaceholderMap {

    private final Map<String, String> map = new HashMap<>();

    private PlaceholderMap() {
    }

    public static PlaceholderMap create() {
        return new PlaceholderMap();
    }

    public static PlaceholderMap from(Map<String, String> seed) {
        PlaceholderMap builder = new PlaceholderMap();
        builder.map.putAll(seed);
        return builder;
    }

    public PlaceholderMap put(String key, String value) {
        map.put(key, value);
        return this;
    }

    public PlaceholderMap put(String key, int value) {
        return put(key, String.valueOf(value));
    }

    public PlaceholderMap money(EconomyService economy, String key, String economyId, double amount) {
        economy.formatInto(map, key, economyId, amount);
        return this;
    }

    public PlaceholderMap money(EconomyService economy, String key, String economyId, EconomyService.ChargeResult charge) {
        economy.formatInto(map, key, economyId, charge);
        return this;
    }

    public PlaceholderMap money(EconomyService economy, String key, String economyId, double amount, EconomyService.ChargeKind kind) {
        economy.formatInto(map, key, economyId, amount, kind);
        return this;
    }

    public PlaceholderMap money(EconomyService economy, String key, String economyId, OptionalDouble amount, String emptyLangKey) {
        economy.formatInto(map, key, economyId, amount, emptyLangKey);
        return this;
    }

    public PlaceholderMap economy(EconomyService economy, EconomyProvider provider) {
        economy.formatEconomy(map, provider);
        return this;
    }

    public PlaceholderMap economy(EconomyService economy, String economyId) {
        economy.formatEconomy(map, economyId);
        return this;
    }

    public Map<String, String> build() {
        return map;
    }
}