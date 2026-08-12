package com.ftxeven.airauctions.service.listing;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.config.MainConfig;
import com.ftxeven.airauctions.model.ListingScope;
import com.ftxeven.airauctions.model.PlayerData;
import com.ftxeven.airauctions.permission.PermissionTiers;
import com.ftxeven.airauctions.permission.Permissions;
import com.ftxeven.airauctions.service.listing.match.ItemMatcher;
import com.ftxeven.airauctions.service.player.PlayerService;
import com.ftxeven.airauctions.util.Cooldowns;
import com.ftxeven.airauctions.util.Messenger;
import com.ftxeven.airauctions.util.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.UUID;

public final class ListingValidator {

    private final ConfigManager configs;
    private final PlayerService players;
    private final ItemMatcher matcher;
    private final ListingService listings;
    private final Cooldowns<UUID> cooldowns = new Cooldowns<>();

    public ListingValidator(ConfigManager configs, PlayerService players, ItemMatcher matcher, ListingService listings) {
        this.configs = configs;
        this.players = players;
        this.matcher = matcher;
        this.listings = listings;
    }

    public ValidationResult validate(Player seller, ItemStack item, int amount) {
        MainConfig.Restrictions restrictions = configs.main().restrictions();

        if (!seller.hasPermission(Permissions.Bypass.GAMEMODES) && restrictions.blockedGamemodes().contains(seller.getGameMode())) {
            return new ValidationResult.BlockedGamemode(seller.getGameMode());
        }

        String world = seller.getWorld().getName();
        if (!seller.hasPermission(Permissions.Bypass.WORLDS) && restrictions.blockedWorlds().contains(world)) {
            return new ValidationResult.BlockedWorld(world);
        }

        if (!restrictions.allowDamagedItems() && isDamaged(item) && !seller.hasPermission(Permissions.Bypass.DAMAGED_ITEMS)) {
            return new ValidationResult.DamagedItem();
        }

        if (!seller.hasPermission(Permissions.Bypass.BLACKLIST) && isBlacklisted(item, restrictions.blacklist())) {
            return new ValidationResult.Blacklisted();
        }

        int minAmount = configs.main().listings().minAmount();
        if (minAmount > 0 && amount < minAmount) {
            return new ValidationResult.AmountTooLow(minAmount);
        }

        int cooldown = configs.main().listings().cooldown();
        if (cooldown > 0 && !seller.hasPermission(Permissions.Bypass.COOLDOWN)) {
            double remaining = cooldowns.remainingSeconds(seller.getUniqueId(), cooldown);
            if (remaining > 0) {
                return new ValidationResult.OnCooldown(remaining);
            }
        }

        return new ValidationResult.Ok();
    }

    public ValidationResult validateListingLimit(UUID uuid, int limit) {
        if (limit < 0) {
            return new ValidationResult.Ok();
        }
        int active = listings.count(uuid, ListingScope.ACTIVE);
        return active >= limit ? new ValidationResult.ListingLimitReached(active, limit) : new ValidationResult.Ok();
    }

    public ValidationResult validateExpiredLimit(UUID uuid, int limit) {
        if (limit < 0) {
            return new ValidationResult.Ok();
        }
        int expired = listings.count(uuid, ListingScope.EXPIRED);
        return expired >= limit ? new ValidationResult.ExpiredLimitReached(expired, limit) : new ValidationResult.Ok();
    }

    public int maxActiveListings(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);

        double base = online != null
                ? PermissionTiers.resolveUnlimitedTier(online, Permissions.Bypass.LIMIT, configs.main().listings().maxActive())
                : configs.main().listings().maxActive();

        if (base < 0) {
            return -1;
        }

        int extra = players.find(uuid).map(PlayerData::extraSlots).orElse(0);
        return (int) base + extra;
    }

    public int availableSlots(UUID uuid) {
        int max = maxActiveListings(uuid);
        if (max < 0) {
            return -1;
        }
        return Math.max(0, max - listings.count(uuid, ListingScope.ACTIVE));
    }

    public void checkListingLimits(Player seller, Messenger messenger, Runnable onPassed) {
        UUID uuid = seller.getUniqueId();
        int activeLimit = maxActiveListings(uuid);
        int expiredLimit = configs.main().listings().maxExpired();
        if (activeLimit < 0 && expiredLimit < 0) {
            onPassed.run();
            return;
        }

        Scheduler.runAsync(() -> {
            ValidationResult result = activeLimit < 0 ? new ValidationResult.Ok() : validateListingLimit(uuid, activeLimit);
            if (result.ok()) {
                result = validateExpiredLimit(uuid, expiredLimit);
            }

            ValidationResult finalResult = result;
            Scheduler.runEntity(seller, () -> {
                if (finalResult.ok()) {
                    onPassed.run();
                } else {
                    finalResult.send(seller, configs, messenger);
                }
            });
        });
    }

    public void markListed(UUID uuid) {
        cooldowns.hit(uuid);
    }

    public void clearCooldown(UUID uuid) {
        cooldowns.clear(uuid);
    }

    private boolean isDamaged(ItemStack item) {
        return item.getItemMeta() instanceof Damageable damageable && damageable.getDamage() > 0;
    }

    private boolean isBlacklisted(ItemStack item, MainConfig.Blacklist blacklist) {
        return matcher.matches(item, blacklist.matchRules()) != blacklist.asWhitelist();
    }
}