package com.ftxeven.airauctions.common.hook;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface ItemHook {

    String name();

    @Nullable String identify(ItemStack item);

    @Nullable ItemStack buildItem(String id);
}
