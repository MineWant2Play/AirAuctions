package com.ftxeven.airauctions.api.papi;

import com.ftxeven.airauctions.AirAuctions;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class AirAuctionsExpansion extends PlaceholderExpansion {

    private static final String GUI_PREFIX = "gui_";
    private static final String GLOBAL_LISTINGS_PREFIX = "global_listings_";
    private static final String LISTINGS_PREFIX = "listings_";
    private static final String SPENT_PREFIX = "spent_";
    private static final String EARNED_PREFIX = "earned_";
    private static final String VOLUME_PREFIX = "volume_";

    private final AirAuctions plugin;
    private final GuiPlaceholders gui;
    private final ListingPlaceholders listingStats;
    private final EconomyPlaceholders economyStats;

    public AirAuctionsExpansion(AirAuctions plugin) {
        this.plugin = plugin;
        this.gui = new GuiPlaceholders(plugin.guis(), plugin.configs(), plugin.services().players());
        this.listingStats = new ListingPlaceholders(plugin.services());
        this.economyStats = new EconomyPlaceholders(plugin.services(), plugin.configs());
    }

    @Override
    public @NotNull String getIdentifier() {
        return "airauctions";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public @NotNull String getRequiredPlugin() {
        return "AirAuctions";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return null;
        }
        String lower = params.toLowerCase(Locale.ROOT);

        if (lower.startsWith(GUI_PREFIX)) {
            return player.isOnline() ? gui.resolve(player.getPlayer(), lower.substring(GUI_PREFIX.length())) : "";
        }
        if (lower.equals("max_listings")) {
            return listingStats.maxListings(player.getUniqueId());
        }
        if (lower.equals("available_slots")) {
            return listingStats.availableSlots(player.getUniqueId());
        }
        if (lower.startsWith(GLOBAL_LISTINGS_PREFIX)) {
            return listingStats.global(lower.substring(GLOBAL_LISTINGS_PREFIX.length()));
        }
        if (lower.startsWith(LISTINGS_PREFIX)) {
            return listingStats.perPlayer(player.getUniqueId(), lower.substring(LISTINGS_PREFIX.length()));
        }
        if (lower.startsWith(SPENT_PREFIX)) {
            return economyStats.spent(player.getUniqueId(), params.substring(SPENT_PREFIX.length()).split("_"));
        }
        if (lower.startsWith(EARNED_PREFIX)) {
            return economyStats.earned(player.getUniqueId(), params.substring(EARNED_PREFIX.length()).split("_"));
        }
        if (lower.startsWith(VOLUME_PREFIX)) {
            return economyStats.volume(params.substring(VOLUME_PREFIX.length()).split("_"));
        }

        return null;
    }
}