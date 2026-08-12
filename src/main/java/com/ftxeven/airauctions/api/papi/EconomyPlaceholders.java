package com.ftxeven.airauctions.api.papi;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.service.ServiceManager;
import com.ftxeven.airauctions.util.MiniText;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

final class EconomyPlaceholders {

    private final ServiceManager services;
    private final ConfigManager configs;

    EconomyPlaceholders(ServiceManager services, ConfigManager configs) {
        this.services = services;
        this.configs = configs;
    }

    @Nullable String spent(UUID player, String[] tokens) {
        return perPlayer(player, tokens, false);
    }

    @Nullable String earned(UUID player, String[] tokens) {
        return perPlayer(player, tokens, true);
    }

    @Nullable String volume(String[] tokens) {
        Parsed parsed = parse(tokens);
        if (parsed == null) {
            return null;
        }
        Map<String, Double> totals = services.history().volume(parsed.since());
        return format(parsed.economyId(), totals.getOrDefault(parsed.economyId(), 0.0), parsed.plain());
    }

    private @Nullable String perPlayer(UUID player, String[] tokens, boolean earned) {
        Parsed parsed = parse(tokens);
        if (parsed == null) {
            return null;
        }
        Map<String, Double> totals = earned
                ? services.history().earned(player, parsed.since())
                : services.history().spent(player, parsed.since());
        return format(parsed.economyId(), totals.getOrDefault(parsed.economyId(), 0.0), parsed.plain());
    }

    private @Nullable Parsed parse(String[] tokens) {
        if (tokens.length == 0) {
            return null;
        }
        Period period = Period.parse(tokens[0]);
        if (period == null) {
            return null;
        }

        String[] rest = Arrays.copyOfRange(tokens, 1, tokens.length);
        boolean plain = rest.length > 0 && rest[rest.length - 1].equalsIgnoreCase("plain");
        String[] economyTokens = plain ? Arrays.copyOfRange(rest, 0, rest.length - 1) : rest;
        String economyId = economyTokens.length == 0
                ? configs.expansions().economy().defaultCurrency()
                : String.join("_", economyTokens);

        return new Parsed(period.since(configs.main().formatting()), economyId, plain);
    }

    private String format(String economyId, double amount, boolean plain) {
        String formatted = services.economy().format(economyId, amount);
        return plain ? MiniText.plain(formatted) : formatted;
    }

    private record Parsed(@Nullable Instant since, String economyId, boolean plain) {}
}