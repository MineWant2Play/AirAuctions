package com.ftxeven.airauctions.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class ConfigManager {

    private final JavaPlugin plugin;
    private final List<BaseConfig> configs = new ArrayList<>();

    private final MainConfig main;
    private final ExpansionsConfig expansions;
    private final StorageConfig storage;
    private final CommandsConfig commands;
    private final FilterConfig filter;
    private final AnimationsConfig animations;
    private final LangConfig lang;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        main = register(new MainConfig(plugin));
        expansions = register(new ExpansionsConfig(plugin));
        storage = register(new StorageConfig(plugin));
        commands = register(new CommandsConfig(plugin));
        filter = register(new FilterConfig(plugin));
        animations = register(new AnimationsConfig(plugin));
        lang = new LangConfig(plugin, animations);
    }

    public boolean load() {
        boolean ok = true;
        for (BaseConfig config : configs) {
            try {
                config.load();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load " + config.fileName() + ": " + e.getMessage());
                ok = false;
            }
        }

        // lang needs general.lang/items-lang out of config.yml, so it loads last
        if (main.general() != null) {
            try {
                lang.load(main.general().lang(), main.general().itemsLang());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load lang files: " + e.getMessage());
                ok = false;
            }
        } else {
            plugin.getLogger().severe("Skipping lang load, " + main.fileName() + " never loaded successfully");
            ok = false;
        }

        return ok;
    }

    public boolean reload() {
        return load();
    }

    public MainConfig main() { return main; }

    public ExpansionsConfig expansions() { return expansions; }

    public StorageConfig storage() { return storage; }

    public CommandsConfig commands() { return commands; }

    public FilterConfig filter() { return filter; }

    public AnimationsConfig animations() { return animations; }

    public LangConfig lang() { return lang; }

    private <T extends BaseConfig> T register(T config) {
        configs.add(config);
        return config;
    }
}