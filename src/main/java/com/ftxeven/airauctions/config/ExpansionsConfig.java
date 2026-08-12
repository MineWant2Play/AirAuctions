package com.ftxeven.airauctions.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ExpansionsConfig extends BaseConfig {

    private volatile Economy economy;
    private volatile Discord discord;

    public ExpansionsConfig(JavaPlugin plugin) {
        super(plugin, "expansions.yml");
    }

    @Override
    protected void read(ConfigurationSection yaml) {
        Economy newEconomy = readEconomy(yaml.getConfigurationSection("economy"));
        Discord newDiscord = readDiscord(yaml.getConfigurationSection("discord"));

        economy = newEconomy;
        discord = newDiscord;
    }

    public Economy economy() {
        return economy;
    }

    public Discord discord() {
        return discord;
    }

    // Section readers

    private Economy readEconomy(ConfigurationSection sec) {
        sec = orEmpty(sec);

        Map<String, Provider> providers = readProviders(sec.getConfigurationSection("providers"));
        String defaultCurrency = getString(sec, "default-currency", "vault");
        if (!providers.containsKey(defaultCurrency)) {
            plugin.getLogger().warning("default-currency '" + defaultCurrency + "' has no matching provider in " + fileName());
        }

        int buyAmountTrigger = getInt(sec, "buy-amount-trigger", 2);
        if (buyAmountTrigger < 2) {
            plugin.getLogger().warning("buy-amount-trigger must be >= 2 in " + fileName() + ", using 2");
            buyAmountTrigger = 2;
        }

        double minPrice = getDouble(sec, "min-price", 1);
        double maxPrice = getDouble(sec, "max-price", 10000000);
        if (maxPrice >= 0 && minPrice > maxPrice) {
            plugin.getLogger().warning("'min-price' (" + minPrice + ") is greater than 'max-price' (" + maxPrice + ") in " + fileName() + ", clamping min-price down to max-price");
            minPrice = maxPrice;
        }

        return new Economy(
                enumOr(sec, "number-format", NumberFormat.class, NumberFormat.SHORT),
                getStringList(sec, "number-format-suffixes", List.of("", "k", "M", "B", "T", "Q")),
                getBoolean(sec, "allow-shorthand-input", true),
                enumOr(sec, "decimal-handling", DecimalHandling.class, DecimalHandling.REJECT),
                minPrice,
                maxPrice,
                enumOr(sec, "buy-check", BuyCheck.class, BuyCheck.MINIMUM),
                buyAmountTrigger,
                getDouble(sec, "min-partial-price", 1),
                getBoolean(sec, "multi-currency", true),
                defaultCurrency,
                providers,
                readCharge(sec.getConfigurationSection("listing-fee"), 5),
                readCharge(sec.getConfigurationSection("sales-tax"), 10)
        );
    }

    private Map<String, Provider> readProviders(ConfigurationSection sec) {
        if (sec == null) {
            return Map.of();
        }
        Map<String, Provider> providers = new LinkedHashMap<>();
        for (String providerId : sec.getKeys(false)) {
            providers.put(providerId, readProvider(sec.getConfigurationSection(providerId), providerId));
        }
        return Collections.unmodifiableMap(providers);
    }

    // "all" is a reserved pseudo-provider (the "no filter" option in the economy cycle)
    // and legitimately has no type/settings/key, so it's built directly rather than warned about
    private Provider readProvider(ConfigurationSection sec, String providerId) {
        sec = orEmpty(sec);
        if (providerId.equals("all")) {
            return new Provider(true, null, providerId, sec.getString("display-name", providerId), "%amount%", false, null, null);
        }

        ProviderType type = providerType(sec.getString("type", ""), providerId);
        PlaceholderSettings placeholderSettings = type == ProviderType.PLACEHOLDER
                ? readPlaceholderSettings(sec.getConfigurationSection("settings"), providerId)
                : null;
        ExcellentEconomySettings excellentEconomySettings = type == ProviderType.EXCELLENTECONOMY
                ? readExcellentEconomySettings(sec.getConfigurationSection("settings"), providerId)
                : null;

        boolean allowDecimals = getBoolean(sec, "allow-decimals", false);
        if (type == ProviderType.PLAYERPOINTS && allowDecimals) {
            plugin.getLogger().warning("Provider '" + providerId + "' is type PLAYERPOINTS but has 'allow-decimals: true' in " + fileName() + ", PlayerPoints only supports whole numbers, forcing it to false");
            allowDecimals = false;
        }

        return new Provider(
                getBoolean(sec, "enabled", true),
                type,
                getString(sec, "key", providerId),
                getString(sec, "display-name", providerId),
                getString(sec, "format", "%amount%"),
                allowDecimals,
                placeholderSettings,
                excellentEconomySettings
        );
    }

    private ExcellentEconomySettings readExcellentEconomySettings(ConfigurationSection sec, String providerId) {
        sec = orEmpty(sec);
        String currency = getString(sec, "currency", "");
        if (currency.isBlank()) {
            plugin.getLogger().warning("Provider '" + providerId + "' is type EXCELLENTECONOMY but has no 'settings.currency' set in " + fileName() + ", it will not work");
            return null;
        }
        return new ExcellentEconomySettings(currency);
    }

    private ProviderType providerType(String raw, String providerId) {
        if (raw.isBlank()) {
            plugin.getLogger().warning("Provider '" + providerId + "' has no 'type' set in " + fileName() + ", it will be disabled");
            return null;
        }
        try {
            return ProviderType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid provider type '" + raw + "' for provider '" + providerId + "' in " + fileName() + ", it will be disabled");
            return null;
        }
    }

    private PlaceholderSettings readPlaceholderSettings(ConfigurationSection sec, String providerId) {
        if (sec == null) {
            plugin.getLogger().warning("Provider '" + providerId + "' is type PLACEHOLDER but has no 'settings' block in " + fileName() + ", it will not work");
            return null;
        }
        return new PlaceholderSettings(
                getString(sec, "balance-placeholder", ""),
                getString(sec, "give-command", ""),
                getString(sec, "take-command", "")
        );
    }

    // listing-fee and sales-tax share this exact shape, only the default amount differs
    private Charge readCharge(ConfigurationSection sec, double defaultAmount) {
        sec = orEmpty(sec);
        double min = getDouble(sec, "min", 1);
        double max = getDouble(sec, "max", 1000);
        if (max >= 0 && min > max) {
            plugin.getLogger().warning("'min' (" + min + ") is greater than 'max' (" + max + ") in " + fileName() + ", clamping min down to max");
            min = max;
        }

        return new Charge(
                getBoolean(sec, "enabled", true),
                enumOr(sec, "type", ChargeType.class, ChargeType.PERCENTAGE),
                getDouble(sec, "default", defaultAmount),
                min,
                max
        );
    }

    private Discord readDiscord(ConfigurationSection sec) {
        sec = orEmpty(sec);
        return new Discord(
                getBoolean(sec, "enabled", false),
                getString(sec, "webhook-url", ""),
                enumOr(sec, "time-format", TimeFormat.class, TimeFormat.RELATIVE),
                readEvents(sec.getConfigurationSection("events"))
        );
    }

    private Events readEvents(ConfigurationSection sec) {
        sec = orEmpty(sec);
        return new Events(
                readEvent(sec, "listing-deleted", false),
                readEvent(sec, "auction-new", true),
                readEvent(sec, "auction-sold", true),
                readEvent(sec, "auction-partial-sold", true),
                readEvent(sec, "auction-expired", false),
                readEvent(sec, "bid-new", true),
                readEvent(sec, "bid-placed", true),
                readEvent(sec, "bid-ended", true),
                readEvent(sec, "bid-expired", false)
        );
    }

    private DiscordEvent readEvent(ConfigurationSection eventsSection, String key, boolean defaultEnabled) {
        ConfigurationSection sec = eventsSection.getConfigurationSection(key);
        if (sec == null) {
            plugin.getLogger().warning("Missing discord event '" + key + "' in " + fileName() + ", it will be disabled");
            return DiscordEvent.disabled();
        }

        return new DiscordEvent(
                getBoolean(sec, "enabled", defaultEnabled),
                getString(sec, "webhook-url", ""),
                readEventTimeFormat(sec, key),
                getInt(sec, "color", 0),
                readAuthor(sec.getConfigurationSection("author")),
                getString(sec, "title", ""),
                getString(sec, "title-url", ""),
                getString(sec, "description", ""),
                getString(sec, "thumbnail-url", ""),
                getString(sec, "image-url", ""),
                readFields(sec),
                readFooter(sec.getConfigurationSection("footer")),
                getBoolean(sec, "timestamp", true)
        );
    }

    private @Nullable TimeFormat readEventTimeFormat(ConfigurationSection sec, String eventKey) {
        String raw = sec.getString("time-format", "");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return TimeFormat.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid time-format '" + raw + "' for discord event '" + eventKey + "' in " + fileName() + ", falling back to the global time-format");
            return null;
        }
    }

    private Author readAuthor(ConfigurationSection sec) {
        sec = orEmpty(sec);
        return new Author(
                getString(sec, "name", ""),
                getString(sec, "icon-url", ""),
                getString(sec, "url", "")
        );
    }

    private Footer readFooter(ConfigurationSection sec) {
        sec = orEmpty(sec);
        return new Footer(
                getString(sec, "text", ""),
                getString(sec, "icon-url", "")
        );
    }

    private List<EmbedField> readFields(ConfigurationSection sec) {
        List<Map<?, ?>> raw = sec.getMapList("fields");
        List<EmbedField> fields = new ArrayList<>(raw.size());
        for (Map<?, ?> entry : raw) {
            Object name = entry.get("name");
            Object value = entry.get("value");
            fields.add(new EmbedField(
                    name != null ? String.valueOf(name) : "",
                    value != null ? String.valueOf(value) : "",
                    Boolean.TRUE.equals(entry.get("inline"))
            ));
        }
        return fields;
    }

    // Section types

    public enum NumberFormat { SHORT, FORMATTED, RAW }

    public enum DecimalHandling { FLOOR, REJECT }

    public enum BuyCheck { MINIMUM, FULL }

    public enum ProviderType { VAULT, EXP, PLACEHOLDER, PLAYERPOINTS, EXCELLENTECONOMY }

    public enum ChargeType { PERCENTAGE, FIXED }

    public enum TimeFormat { RELATIVE, DATETIME, DATETIME_RELATIVE, TEXT }

    public record PlaceholderSettings(String balancePlaceholder, String giveCommand, String takeCommand) {}

    public record ExcellentEconomySettings(String currency) {}

    // type is null for pseudo-providers like "all"; settings is null except for PLACEHOLDER.
    // 'key' is the user-typable identifier (ECONOMY_OPTIONS / command parsing); 'id' (the
    // providers-map key this Provider is stored under) stays the internal/storage identifier.
    public record Provider(
            boolean enabled,
            ProviderType type,
            String key,
            String displayName,
            String format,
            boolean allowDecimals,
            PlaceholderSettings settings,
            ExcellentEconomySettings excellentEconomySettings
    ) {}

    public record Charge(boolean enabled, ChargeType type, double defaultValue, double min, double max) {}

    public record Economy(
            NumberFormat numberFormat,
            List<String> numberFormatSuffixes,
            boolean allowShorthandInput,
            DecimalHandling decimalHandling,
            double minPrice,
            double maxPrice,
            BuyCheck buyCheck,
            int buyAmountTrigger,
            double minPartialPrice,
            boolean multiCurrency,
            String defaultCurrency,
            Map<String, Provider> providers,
            Charge listingFee,
            Charge salesTax
    ) {
        public Economy {
            numberFormatSuffixes = List.copyOf(numberFormatSuffixes);
        }
    }

    public record Author(String name, String iconUrl, String url) {}

    public record EmbedField(String name, String value, boolean inline) {}

    public record Footer(String text, String iconUrl) {}

    public record DiscordEvent(
            boolean enabled,
            String webhookUrl,
            @Nullable TimeFormat timeFormat,
            int color,
            Author author,
            String title,
            String titleUrl,
            String description,
            String thumbnailUrl,
            String imageUrl,
            List<EmbedField> fields,
            Footer footer,
            boolean timestamp
    ) {
        public DiscordEvent {
            fields = List.copyOf(fields);
        }

        private static DiscordEvent disabled() {
            return new DiscordEvent(false, "", null, 0, new Author("", "", ""), "", "", "", "", "", List.of(), new Footer("", ""), false);
        }
    }

    public record Events(
            DiscordEvent listingDeleted,
            DiscordEvent auctionNew,
            DiscordEvent auctionSold,
            DiscordEvent auctionPartialSold,
            DiscordEvent auctionExpired,
            DiscordEvent bidNew,
            DiscordEvent bidPlaced,
            DiscordEvent bidEnded,
            DiscordEvent bidExpired
    ) {}

    public record Discord(boolean enabled, String webhookUrl, TimeFormat timeFormat, Events events) {}
}