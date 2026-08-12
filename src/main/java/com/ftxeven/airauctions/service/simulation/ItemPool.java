package com.ftxeven.airauctions.service.simulation;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.config.FilterConfig;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class ItemPool {

    private static final List<Material> FALLBACK = List.of(
            Material.DIAMOND, Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE, Material.NETHERITE_INGOT,
            Material.IRON_INGOT, Material.GOLD_INGOT, Material.EMERALD, Material.ENCHANTED_BOOK,
            Material.ELYTRA, Material.TOTEM_OF_UNDYING, Material.NETHER_STAR, Material.OAK_LOG,
            Material.COBBLESTONE, Material.STONE_BRICKS, Material.BREAD, Material.GOLDEN_APPLE,
            Material.ENDER_PEARL, Material.BLAZE_ROD, Material.EXPERIENCE_BOTTLE, Material.SADDLE
    );

    private final List<Material> pool;

    ItemPool(ConfigManager configs) {
        this.pool = fromCategories(configs);
    }

    private static List<Material> fromCategories(ConfigManager configs) {
        List<Material> found = new ArrayList<>();
        for (FilterConfig.Category category : configs.filter().categories().values()) {
            for (String raw : category.matchRules().materials()) {
                Material material = Material.matchMaterial(raw);
                if (material != null && material.isItem()) {
                    found.add(material);
                }
            }
        }
        return found.isEmpty() ? FALLBACK : found;
    }

    ItemStack random(Random random) {
        Material material = pool.get(random.nextInt(pool.size()));
        ItemStack item = new ItemStack(material);

        if (material.getMaxDurability() > 0 && random.nextInt(10) == 0) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof Damageable damageable) {
                damageable.setDamage(random.nextInt(material.getMaxDurability()));
                item.setItemMeta(meta);
            }
        }
        return item;
    }
}