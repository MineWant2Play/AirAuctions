package com.ftxeven.airauctions.common.hook.impl;

import com.ftxeven.airauctions.common.hook.ItemHook;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ItemsAdderHook implements ItemHook {

    @Override
    public String prefix() { return "itemsadder"; }

    @Override
    public @Nullable String rawId(ItemStack item) {
        var stack = CustomStack.byItemStack(item);
        return stack != null ? stack.getNamespacedID() : null;
    }

    @Override
    public @Nullable ItemStack buildItem(String id) {
        var stack = CustomStack.getInstance(id);
        return stack != null ? stack.getItemStack() : null;
    }
}