package com.ftxeven.airauctions.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class ItemWithdrawal {

    private ItemWithdrawal() {
    }

    public static boolean has(Inventory inventory, ItemStack snapshot, int amount) {
        return available(inventory, snapshot) >= amount;
    }

    public static boolean take(Inventory inventory, ItemStack snapshot, int amount, int preferredSlot) {
        if (!has(inventory, snapshot, amount)) {
            return false;
        }

        int size = inventory.getContents().length;
        int remaining = drain(inventory, snapshot, amount, preferredSlot);
        for (int slot = 0; remaining > 0 && slot < size; slot++) {
            if (slot != preferredSlot) {
                remaining = drain(inventory, snapshot, remaining, slot);
            }
        }
        return true;
    }

    private static int drain(Inventory inventory, ItemStack snapshot, int remaining, int slot) {
        ItemStack stack = inventory.getItem(slot);
        if (stack == null || stack.getAmount() <= 0 || !stack.isSimilar(snapshot)) {
            return remaining;
        }

        int taken = Math.min(remaining, stack.getAmount());
        if (taken >= stack.getAmount()) {
            inventory.setItem(slot, null);
        } else {
            stack.setAmount(stack.getAmount() - taken);
            inventory.setItem(slot, stack);
        }
        return remaining - taken;
    }

    private static int available(Inventory inventory, ItemStack snapshot) {
        int total = 0;
        for (ItemStack stack : inventory.getContents()) {
            if (stack != null && stack.isSimilar(snapshot)) {
                total += stack.getAmount();
            }
        }
        return total;
    }
}