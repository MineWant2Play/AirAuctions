package com.ftxeven.airauctions.database.repository;

import org.bukkit.inventory.ItemStack;

@FunctionalInterface
public interface ListingMetadataResolver {

    // implemented by the service class and handed down to the repositories on reload,
    // so stored listings stay in sync with whatever categories/lang keys the reload just applied
    Metadata resolve(ItemStack item);

    record Metadata(String category, String searchName) {}
}