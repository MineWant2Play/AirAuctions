package com.ftxeven.airauctions.core.gui.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

// Recursively merges an 'override' section onto a 'base' one
public final class SectionMerge {

    private SectionMerge() {
    }

    public static @Nullable ConfigurationSection merge(@Nullable ConfigurationSection base, @Nullable ConfigurationSection override) {
        if (override == null) {
            return base;
        }
        if (base == null) {
            return override;
        }

        YamlConfiguration merged = new YamlConfiguration();
        copyInto(merged, base);
        overlayInto(merged, override);
        return merged;
    }

    // Replaces the section at 'key' in 'target' wholesale with a copy of 'source'
    public static void replace(ConfigurationSection target, String key, ConfigurationSection source) {
        target.set(key, null);
        copyInto(target.createSection(key), source);
    }

    private static void copyInto(ConfigurationSection target, ConfigurationSection source) {
        for (String key : source.getKeys(false)) {
            ConfigurationSection nested = source.getConfigurationSection(key);
            if (nested != null) {
                copyInto(target.createSection(key), nested);
            } else {
                target.set(key, source.get(key));
            }
        }
    }

    private static void overlayInto(ConfigurationSection target, ConfigurationSection override) {
        for (String key : override.getKeys(false)) {
            ConfigurationSection overrideNested = override.getConfigurationSection(key);
            ConfigurationSection targetNested = target.getConfigurationSection(key);
            if (overrideNested != null && targetNested != null) {
                overlayInto(targetNested, overrideNested);
            } else if (overrideNested != null) {
                copyInto(target.createSection(key), overrideNested);
            } else {
                target.set(key, override.get(key));
            }
        }
    }
}