package com.ftxeven.airauctions.config;

import com.ftxeven.airauctions.model.MatchRules;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FilterConfig extends BaseConfig {

    private volatile Map<String, Category> categories;

    public FilterConfig(JavaPlugin plugin) {
        super(plugin, "data/filter.yml");
    }

    @Override
    protected void read(ConfigurationSection yaml) {
        categories = readCategories(yaml);
    }

    public Map<String, Category> categories() {
        return categories;
    }

    // Section readers

    private Map<String, Category> readCategories(ConfigurationSection sec) {
        if (sec == null) {
            return Map.of();
        }
        Map<String, Category> categories = new LinkedHashMap<>();
        for (String key : sec.getKeys(false)) {
            categories.put(key, readCategory(sec.getConfigurationSection(key), key));
        }
        return Collections.unmodifiableMap(categories);
    }

    private Category readCategory(ConfigurationSection sec, String key) {
        sec = orEmpty(sec);
        if (key.equals("all")) {
            return new Category(sec.getString("display-name", key), MatchRules.EMPTY);
        }
        return new Category(
                sec.getString("display-name", key),
                readMatchRules(sec.getConfigurationSection("match-rules"))
        );
    }

    // Section types

    public record Category(String displayName, MatchRules matchRules) {}
}