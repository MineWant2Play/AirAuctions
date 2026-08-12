package com.ftxeven.airauctions.service.listing;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.config.FilterConfig;
import com.ftxeven.airauctions.database.repository.ListingMetadataResolver;
import com.ftxeven.airauctions.service.listing.match.ItemMatcher;
import com.ftxeven.airauctions.util.ItemDisplay;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Map;

public final class ListingMetadataService implements ListingMetadataResolver {

    private final ConfigManager configs;
    private final ItemMatcher matcher;

    public ListingMetadataService(ConfigManager configs, ItemMatcher matcher) {
        this.configs = configs;
        this.matcher = matcher;
    }

    @Override
    public Metadata resolve(ItemStack item) {
        return new Metadata(category(item), searchName(item));
    }

    private String category(ItemStack item) {
        for (Map.Entry<String, FilterConfig.Category> entry : configs.filter().categories().entrySet()) {
            if (!entry.getKey().equals("all") && matcher.matches(item, entry.getValue().matchRules())) {
                return entry.getKey();
            }
        }
        return "all";
    }

    private String searchName(ItemStack item) {
        return ItemDisplay.plainName(item, configs.lang()).toLowerCase(Locale.ROOT);
    }
}