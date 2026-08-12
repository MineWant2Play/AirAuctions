package com.ftxeven.airauctions.config;

import com.ftxeven.airauctions.model.ListingScope;
import com.ftxeven.airauctions.model.MatchRules;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MainConfig extends BaseConfig {

    private volatile General general;
    private volatile Formatting formatting;
    private volatile Listings listings;
    private volatile Auctions auctions;
    private volatile Bids bids;
    private volatile Notifications notifications;
    private volatile Restrictions restrictions;

    public MainConfig(JavaPlugin plugin) {
        super(plugin, "config.yml");
    }

    @Override
    protected void read(ConfigurationSection yaml) {
        General newGeneral = readGeneral(yaml.getConfigurationSection("general"));
        Formatting newFormatting = readFormatting(yaml.getConfigurationSection("formatting"));
        Listings newListings = readListings(yaml.getConfigurationSection("listings"));
        Auctions newAuctions = readAuctions(yaml.getConfigurationSection("auctions"));
        Bids newBids = readBids(yaml.getConfigurationSection("bids"));
        Notifications newNotifications = readNotifications(yaml.getConfigurationSection("notifications"));
        Restrictions newRestrictions = readRestrictions(yaml.getConfigurationSection("restrictions"));

        general = newGeneral;
        formatting = newFormatting;
        listings = newListings;
        auctions = newAuctions;
        bids = newBids;
        notifications = newNotifications;
        restrictions = newRestrictions;
    }

    public General general() {
        return general;
    }

    public Formatting formatting() {
        return formatting;
    }

    public Listings listings() {
        return listings;
    }

    public Auctions auctions() {
        return auctions;
    }

    public Bids bids() {
        return bids;
    }

    public Notifications notifications() {
        return notifications;
    }

    public Restrictions restrictions() {
        return restrictions;
    }

    public int purgeDelaySeconds(ListingScope scope) {
        return switch (scope) {
            case ACTIVE -> -1;
            case EXPIRED -> listings.expiredPurgeDelay();
            case STORAGE -> bids.collectPurgeDelay();
        };
    }

    // Section readers

    private General readGeneral(ConfigurationSection sec) {
        sec = orEmpty(sec);
        return new General(
                getString(sec, "lang", "en_US"),
                getString(sec, "items-lang", "en_US"),
                getBoolean(sec, "notify-updates", true),
                getBoolean(sec, "console-feedback", true),
                getBoolean(sec, "strict-args", true)
        );
    }

    private Formatting readFormatting(ConfigurationSection sec) {
        sec = orEmpty(sec);
        DurationStyle duration = new DurationStyle(
                enumOr(sec, "duration.mode", DurationMode.class, DurationMode.CUSTOM),
                Math.max(1, getInt(sec, "duration.granularity", 2))
        );

        ZoneId zone = zone(getString(sec, "timezone", "system"));
        return new Formatting(
                duration,
                pattern(getString(sec, "time", "HH:mm"), "HH:mm").withZone(zone),
                pattern(getString(sec, "date", "dd/MM/yy"), "dd/MM/yy").withZone(zone),
                zone
        );
    }

    private Listings readListings(ConfigurationSection sec) {
        sec = orEmpty(sec);
        Sort sort = new Sort(
                readLabelMap(sec.getConfigurationSection("sort.active")),
                readLabelMap(sec.getConfigurationSection("sort.unclaimed")),
                readLabelMap(sec.getConfigurationSection("sort.history"))
        );

        int minAmount = getInt(sec, "min-amount", 1);
        if (minAmount <= 1) {
            minAmount = -1;
        }

        return new Listings(
                getInt(sec, "max-active", 3),
                getInt(sec, "max-history", 150),
                getInt(sec, "max-expired", 50),
                getInt(sec, "cooldown", 5),
                enumOr(sec, "amount-mode", AmountMode.class, AmountMode.OPTIONAL),
                minAmount,
                getBoolean(sec, "drop-on-full-inventory", false),
                getBoolean(sec, "instant-cancel", false),
                getInt(sec, "expired-purge-delay", -1),
                getInt(sec, "sweep-interval", 5),
                getBoolean(sec, "require-delete-confirmation", true),
                getInt(sec, "delete-confirmation-timeout", 60),
                sort,
                readLabelMap(sec.getConfigurationSection("filter.type"))
        );
    }

    private Auctions readAuctions(ConfigurationSection sec) {
        sec = orEmpty(sec);
        return new Auctions(
                getBoolean(sec, "allow-self-purchase", false),
                getInt(sec, "lifetime", 86400),
                readFlow(sec)
        );
    }

    private Bids readBids(ConfigurationSection sec) {
        sec = orEmpty(sec);

        int minIncrement = getInt(sec, "min-increment", 5);
        int maxIncrement = getInt(sec, "max-increment", 100000);
        if (maxIncrement >= 0 && minIncrement > maxIncrement) {
            plugin.getLogger().warning("'min-increment' (" + minIncrement + ") is greater than 'max-increment' (" + maxIncrement + ") in " + fileName() + ", clamping min-increment down to max-increment");
            minIncrement = maxIncrement;
        }

        int minDuration = getInt(sec, "min-duration", 60);
        int maxDuration = getInt(sec, "max-duration", 3600);
        if (maxDuration >= 0 && minDuration > maxDuration) {
            plugin.getLogger().warning("'min-duration' (" + minDuration + ") is greater than 'max-duration' (" + maxDuration + ") in " + fileName() + ", clamping min-duration down to max-duration");
            minDuration = maxDuration;
        }

        return new Bids(
                getBoolean(sec, "allow-self-bidding", false),
                minIncrement,
                maxIncrement,
                minDuration,
                maxDuration,
                getInt(sec, "default-duration", 300),
                readFlow(sec),
                getInt(sec, "snipe-extend", 20),
                getInt(sec, "snipe-window", 5),
                getBoolean(sec, "instant-collect", false),
                getInt(sec, "collect-purge-delay", 604800),
                getInt(sec, "max-uncollected", 10)
        );
    }

    // auctions and bids share this exact confirmation flow
    private ListingFlow readFlow(ConfigurationSection sec) {
        return new ListingFlow(
                getBoolean(sec, "require-confirmation", true),
                getInt(sec, "confirmation-timeout", 60),
                getBoolean(sec, "announce-to-seller", false)
        );
    }

    private Notifications readNotifications(ConfigurationSection sec) {
        sec = orEmpty(sec);
        return new Notifications(
                getBoolean(sec, "enabled", true),
                getInt(sec, "join-delay", 40),
                getInt(sec, "repeat-times", 3)
        );
    }

    private Restrictions readRestrictions(ConfigurationSection sec) {
        sec = orEmpty(sec);
        return new Restrictions(
                enumSet(sec, "blocked-gamemodes", GameMode.class),
                getStringList(sec, "blocked-worlds"),
                getBoolean(sec, "allow-damaged-items", true),
                getInt(sec, "max-search-length", 32),
                readBlacklist(sec.getConfigurationSection("blacklist"))
        );
    }

    private Blacklist readBlacklist(ConfigurationSection sec) {
        sec = orEmpty(sec);
        return new Blacklist(getBoolean(sec, "as-whitelist", false), readMatchRules(sec));
    }

    private DateTimeFormatter pattern(String raw, String fallback) {
        try {
            return DateTimeFormatter.ofPattern(raw);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid pattern '" + raw + "' in " + fileName() + ", using '" + fallback + "'");
            return DateTimeFormatter.ofPattern(fallback);
        }
    }

    private ZoneId zone(String raw) {
        if (raw == null || raw.equalsIgnoreCase("system")) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(raw);
        } catch (DateTimeException e) {
            plugin.getLogger().warning("Invalid timezone '" + raw + "' in " + fileName() + ", using system default");
            return ZoneId.systemDefault();
        }
    }

    // Section types

    public enum DurationMode { DETAILED, SEQUENTIAL, CUSTOM }

    public enum AmountMode { REQUIRED, OPTIONAL, DISABLED }

    public record General(String lang, String itemsLang, boolean notifyUpdates, boolean consoleFeedback, boolean strictArgs) {}

    public record DurationStyle(DurationMode mode, int granularity) {}

    public record Formatting(DurationStyle duration, DateTimeFormatter time, DateTimeFormatter date, ZoneId timezone) {}

    public record Sort(Map<String, String> active, Map<String, String> unclaimed, Map<String, String> history) {

        public Map<String, String> options(String dimension) {
            return switch (dimension) {
                case "active" -> active;
                case "unclaimed" -> unclaimed;
                case "history" -> history;
                default -> Map.of();
            };
        }
    }

    public record Listings(
            int maxActive,
            int maxHistory,
            int maxExpired,
            int cooldown,
            AmountMode amountMode,
            int minAmount,
            boolean dropOnFullInventory,
            boolean instantCancel,
            int expiredPurgeDelay,
            int sweepInterval,
            boolean requireDeleteConfirmation,
            int deleteConfirmationTimeout,
            Sort sort,
            Map<String, String> filterTypes
    ) {}

    public record ListingFlow(boolean requireConfirmation, int confirmationTimeout, boolean announceToSeller) {}

    public record Auctions(boolean allowSelfPurchase, int lifetime, ListingFlow flow) {}

    public record Bids(
            boolean allowSelfBidding,
            int minIncrement,
            int maxIncrement,
            int minDuration,
            int maxDuration,
            int defaultDuration,
            ListingFlow flow,
            int snipeExtend,
            int snipeWindow,
            boolean instantCollect,
            int collectPurgeDelay,
            int maxUncollected
    ) {}

    public record Notifications(boolean enabled, int joinDelay, int repeatTimes) {}

    public record Blacklist(boolean asWhitelist, MatchRules matchRules) {}

    public record Restrictions(
            Set<GameMode> blockedGamemodes,
            List<String> blockedWorlds,
            boolean allowDamagedItems,
            int maxSearchLength,
            Blacklist blacklist
    ) {
        public Restrictions {
            blockedGamemodes = Set.copyOf(blockedGamemodes);
            blockedWorlds = List.copyOf(blockedWorlds);
        }
    }
}