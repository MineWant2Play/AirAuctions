package com.ftxeven.airauctions.gui.render;

import com.ftxeven.airauctions.config.ConfigManager;

import java.util.LinkedHashMap;

public final class FilterOptions {

    private FilterOptions() {
    }

    public static LinkedHashMap<String, String> category(ConfigManager configs) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        configs.filter().categories().forEach((key, category) -> options.put(key, category.displayName()));
        return options;
    }

    public static LinkedHashMap<String, String> type(ConfigManager configs) {
        return new LinkedHashMap<>(configs.main().listings().filterTypes());
    }

    public static LinkedHashMap<String, String> economy(ConfigManager configs, boolean includeAll) {
        LinkedHashMap<String, String> options = new LinkedHashMap<>();
        configs.expansions().economy().providers().forEach((key, provider) -> {
            if (provider.enabled() && (includeAll || !key.equals("all"))) {
                options.put(key, provider.displayName());
            }
        });
        return options;
    }
}