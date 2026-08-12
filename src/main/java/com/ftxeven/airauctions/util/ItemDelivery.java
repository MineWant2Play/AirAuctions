package com.ftxeven.airauctions.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

public final class ItemDelivery {

    private ItemDelivery() {}

    public enum Result { DELIVERED, DROPPED, REJECTED }

    public static boolean fits(PlayerInventory inventory, ItemStack item, int amount) {
        int maxStackSize = item.getMaxStackSize();
        int capacity = 0;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null || stack.getType().isAir()) {
                capacity += maxStackSize;
            } else if (stack.isSimilar(item)) {
                capacity += maxStackSize - stack.getAmount();
            }
            if (capacity >= amount) {
                return true;
            }
        }
        return false;
    }

    public static boolean rejects(Player player, ItemStack item, int amount, boolean dropOnFullInventory) {
        return !dropOnFullInventory && !fits(player.getInventory(), item, amount);
    }

    public static Result give(Player player, ItemStack item, int amount, boolean dropOnFullInventory) {
        if (rejects(player, item, amount, dropOnFullInventory)) {
            return Result.REJECTED;
        }
        ItemStack stack = item.clone();
        stack.setAmount(amount);

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        if (leftover.isEmpty()) {
            return Result.DELIVERED;
        }
        for (ItemStack remaining : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), remaining);
        }
        return Result.DROPPED;
    }
}