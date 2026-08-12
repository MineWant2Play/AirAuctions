package com.ftxeven.airauctions.service.listing;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.util.Messenger;
import com.ftxeven.airauctions.util.TimeFormatter;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;

public sealed interface ValidationResult {

    record Ok() implements ValidationResult {}

    record BlockedGamemode(GameMode mode) implements ValidationResult {}

    record BlockedWorld(String world) implements ValidationResult {}

    record DamagedItem() implements ValidationResult {}

    record Blacklisted() implements ValidationResult {}

    record AmountTooLow(int min) implements ValidationResult {}

    record OnCooldown(double remainingSeconds) implements ValidationResult {}

    record ListingLimitReached(int active, int limit) implements ValidationResult {}

    record ExpiredLimitReached(int count, int limit) implements ValidationResult {}

    record InvalidDuration(int min, int max) implements ValidationResult {}

    record QueryTooLong(int length, int limit) implements ValidationResult {}

    default boolean ok() {
        return this instanceof Ok;
    }

    default void send(Player player, ConfigManager configs, Messenger messenger) {
        switch (this) {
            case BlockedGamemode ignored ->
                    messenger.send(player, configs.lang().get("errors.limits.blocked-gamemode"));
            case BlockedWorld blockedWorld ->
                    messenger.send(player, configs.lang().get("errors.limits.blocked-world"), Map.of("world", blockedWorld.world()));
            case DamagedItem ignored ->
                    messenger.send(player, configs.lang().get("errors.item.damaged"));
            case Blacklisted ignored -> messenger.send(player, configs.lang().get(
                    configs.main().restrictions().blacklist().asWhitelist() ? "errors.item.not-whitelisted" : "errors.item.blacklisted"));
            case AmountTooLow amountTooLow ->
                    messenger.send(player, configs.lang().get("errors.economy.amount-too-low"), Map.of("min", String.valueOf(amountTooLow.min())));
            case OnCooldown onCooldown -> messenger.send(player, configs.lang().get("errors.limits.cooldown"), Map.of(
                    "timeout", TimeFormatter.duration(Duration.ofSeconds((long) Math.ceil(onCooldown.remainingSeconds())), configs.main().formatting(), configs.lang())));
            case ListingLimitReached limitReached -> messenger.send(player, configs.lang().get("errors.limits.limit-reached"), Map.of(
                    "count", String.valueOf(limitReached.active()), "limit", String.valueOf(limitReached.limit())));
            case ExpiredLimitReached expiredLimitReached -> messenger.send(player, configs.lang().get("errors.limits.expired-limit-reached"), Map.of(
                    "count", String.valueOf(expiredLimitReached.count()), "limit", String.valueOf(expiredLimitReached.limit())));
            case InvalidDuration ignored -> {
                // bid duration has its own resolution path
            }
            case QueryTooLong ignored -> {
                // query length is validated by SearchService
            }
            case Ok ignored -> {
                // unreachable
            }
        }
    }
}