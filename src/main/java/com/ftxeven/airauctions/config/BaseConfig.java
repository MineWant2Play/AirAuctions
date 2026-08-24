package com.ftxeven.airauctions.config;

import com.ftxeven.airauctions.model.MatchRules;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public abstract class BaseConfig {

    protected final JavaPlugin plugin;
    private final String fileName;

    protected BaseConfig(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
    }

    public final void load() {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException e) {
            throw new RuntimeException("I/O error: " + e.getMessage(), e);
        } catch (InvalidConfigurationException e) {
            throw new RuntimeException("Malformed YAML: " + e.getMessage(), e);
        }

        read(yaml);
    }

    public String fileName() {
        return fileName;
    }

    protected abstract void read(ConfigurationSection yaml);

    // Parsing helpers

    protected static ConfigurationSection orEmpty(ConfigurationSection section) {
        return section != null ? section : new YamlConfiguration();
    }

    protected static Map<String, String> readLabelMap(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            map.put(key, section.getString(key, key));
        }
        return Collections.unmodifiableMap(map);
    }

    protected MatchRules readMatchRules(ConfigurationSection section) {
        section = orEmpty(section);
        return new MatchRules(
                optionalStringList(section, "materials"),
                optionalStringList(section, "names"),
                optionalStringList(section, "lores"),
                optionalStringList(section, "enchantments"),
                optionalStringList(section, "nbt-keys"),
                optionalStringList(section, "custom-model-data"),
                optionalStringList(section, "render-models"),
                optionalStringList(section, "plugin-items")
        );
    }

    protected String getString(ConfigurationSection section, String path, String fallback) {
        warnIfMissing(section, path, fallback);
        return section.getString(path, fallback);
    }

    protected int getInt(ConfigurationSection section, String path, int fallback) {
        if (warnIfMissing(section, path, fallback)) {
            return fallback;
        }
        Object raw = section.get(path);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        plugin.getLogger().warning("Invalid value '" + raw + "' at '" + path + "' in " + fileName + ", using " + fallback);
        return fallback;
    }

    protected double getDouble(ConfigurationSection section, String path, double fallback) {
        if (warnIfMissing(section, path, fallback)) {
            return fallback;
        }
        Object raw = section.get(path);
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        plugin.getLogger().warning("Invalid value '" + raw + "' at '" + path + "' in " + fileName + ", using " + fallback);
        return fallback;
    }

    protected boolean getBoolean(ConfigurationSection section, String path, boolean fallback) {
        if (warnIfMissing(section, path, fallback)) {
            return fallback;
        }
        Object raw = section.get(path);
        if (raw instanceof Boolean bool) {
            return bool;
        }
        plugin.getLogger().warning("Invalid value '" + raw + "' at '" + path + "' in " + fileName + ", using " + fallback);
        return fallback;
    }

    protected List<String> getStringList(ConfigurationSection section, String path) {
        return getStringList(section, path, List.of());
    }

    protected List<String> getStringList(ConfigurationSection section, String path, List<String> fallback) {
        if (warnIfMissing(section, path, fallback)) {
            return fallback;
        }
        Object raw = section.get(path);
        if (raw instanceof List<?>) {
            return section.getStringList(path);
        }
        plugin.getLogger().warning("Invalid value '" + raw + "' at '" + path + "' in " + fileName + ", using " + fallback);
        return fallback;
    }

    protected List<String> optionalStringList(ConfigurationSection section, String path) {
        if (!section.isSet(path)) {
            return List.of();
        }
        Object raw = section.get(path);
        if (raw instanceof List<?>) {
            return section.getStringList(path);
        }
        plugin.getLogger().warning("Invalid value '" + raw + "' at '" + path + "' in " + fileName + ", skipping");
        return List.of();
    }

    protected <T extends Enum<T>> T enumOr(ConfigurationSection section, String path, Class<T> type, T fallback) {
        if (warnIfMissing(section, path, fallback)) {
            return fallback;
        }
        String raw = section.getString(path, "");
        if (raw.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid value '" + raw + "' at '" + path + "' in " + fileName + ", using " + fallback);
            return fallback;
        }
    }

    protected <T extends Enum<T>> Set<T> enumSet(ConfigurationSection section, String path, Class<T> type) {
        Set<T> values = EnumSet.noneOf(type);
        for (String raw : getStringList(section, path)) {
            try {
                values.add(Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid value '" + raw + "' at '" + path + "' in " + fileName + ", skipping");
            }
        }
        return values;
    }

    private boolean warnIfMissing(ConfigurationSection section, String path, Object fallback) {
        if (section.isSet(path)) {
            return false;
        }
        plugin.getLogger().warning("Missing '" + path + "' in " + fileName + ", using default: " + fallback);
        return true;
    }
}