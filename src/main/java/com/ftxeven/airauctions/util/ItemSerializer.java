package com.ftxeven.airauctions.util;

import org.bukkit.inventory.ItemStack;

public final class ItemSerializer {

    private ItemSerializer() {
    }

    public static byte[] serialize(ItemStack item) {
        return item.serializeAsBytes();
    }

    public static ItemStack deserialize(byte[] data) {
        return ItemStack.deserializeBytes(data);
    }
}