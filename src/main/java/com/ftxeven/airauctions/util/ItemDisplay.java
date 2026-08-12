package com.ftxeven.airauctions.util;

import com.ftxeven.airauctions.config.LangConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemDisplay {

    private ItemDisplay() {
    }

    public static String name(ItemStack item, LangConfig lang) {
        Component displayName = customDisplayName(item);
        return displayName != null ? MiniText.mini().serialize(displayName) : lang.item(item.getType().getKey().getKey());
    }

    public static String plainName(ItemStack item, LangConfig lang) {
        Component displayName = customDisplayName(item);
        return displayName != null ? MiniText.plain(displayName) : lang.item(item.getType().getKey().getKey());
    }

    private static Component customDisplayName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() ? meta.displayName() : null;
    }
}