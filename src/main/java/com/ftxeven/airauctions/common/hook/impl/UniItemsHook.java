package com.ftxeven.airauctions.common.hook.impl;

import com.ftxeven.airauctions.common.hook.ItemHook;
import io.github.projectunified.uniitem.all.AllItemProvider;
import io.github.projectunified.uniitem.api.ItemKey;
import io.github.projectunified.uniitem.api.ItemProvider;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class UniItemsHook implements ItemHook {

    private final ItemProvider provider;

    public UniItemsHook() {
        this(new AllItemProvider());
    }

    UniItemsHook(ItemProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    @Override
    public String name() {
        return "uniitems";
    }

    @Override
    public @Nullable String identify(ItemStack item) {
        ItemKey key = provider.key(item);
        return key != null ? key.toString() : null;
    }

    @Override
    public @Nullable ItemStack buildItem(String id) {
        return provider.item(ItemKey.fromString(id));
    }
}
