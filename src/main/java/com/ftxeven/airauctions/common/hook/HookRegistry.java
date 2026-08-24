package com.ftxeven.airauctions.common.hook;

import com.ftxeven.airauctions.common.gui.render.MaterialResolver;
import com.ftxeven.airauctions.common.hook.impl.UniItemsHook;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HookRegistry implements MaterialResolver.HookResolver {

    private final JavaPlugin plugin;
    private volatile Map<String, ItemHook> hooks = Map.of();

    public HookRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        Map<String, ItemHook> found = new LinkedHashMap<>();
        try {
            ItemHook hook = new UniItemsHook();
            found.put(hook.name(), hook);
        } catch (Throwable e) {
            plugin.getLogger().warning("Failed to initialize UniItems hooks: " + e.getMessage());
        }
        hooks = Collections.unmodifiableMap(found);
    }

    public Map<String, ItemHook> hooks() {
        return hooks;
    }

    public @Nullable ItemStack resolve(String id) {
        if (id.indexOf(':') < 1) {
            return null;
        }
        for (ItemHook hook : hooks.values()) {
            try {
                ItemStack item = hook.buildItem(id);
                if (item != null) {
                    return item;
                }
            } catch (Throwable e) {
                plugin.getLogger().warning("Hook '" + hook.name() + "' threw while building item '" + id + "': " + e.getMessage());
            }
        }
        return null;
    }

    public @Nullable String identify(ItemStack item) {
        for (ItemHook hook : hooks.values()) {
            try {
                String id = hook.identify(item);
                if (id != null) {
                    return id;
                }
            } catch (Throwable e) {
                plugin.getLogger().warning("Hook '" + hook.name() + "' threw while identifying an item, skipping: " + e.getMessage());
            }
        }
        return null;
    }

}
