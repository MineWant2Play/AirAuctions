package com.ftxeven.airauctions.gui.impl;

import com.ftxeven.airauctions.economy.EconomyProvider;
import com.ftxeven.airauctions.gui.render.ListingFlags;
import com.ftxeven.airauctions.model.ListingType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ListingDraft {

    private final ListingType type;
    private final ItemStack itemSnapshot;
    private final int slot;
    private final int amount;
    private final @Nullable Integer bidDurationSeconds; // null for AUCTION, always set for BID

    private double price;
    private String economyId;

    private ListingDraft(ListingType type, ItemStack itemSnapshot, int slot, int amount,
                         @Nullable Integer bidDurationSeconds, double price, String economyId) {
        this.type = type;
        this.itemSnapshot = itemSnapshot;
        this.slot = slot;
        this.amount = amount;
        this.bidDurationSeconds = bidDurationSeconds;
        this.price = price;
        this.economyId = economyId;
    }

    public static ListingDraft auction(ItemStack itemSnapshot, int slot, int amount, double price, String economyId) {
        return new ListingDraft(ListingType.AUCTION, itemSnapshot, slot, amount, null, price, economyId);
    }

    public static ListingDraft bid(ItemStack itemSnapshot, int slot, int amount, int durationSeconds, double price, String economyId) {
        return new ListingDraft(ListingType.BID, itemSnapshot, slot, amount, durationSeconds, price, economyId);
    }

    public ListingType type() { return type; }

    public ItemStack itemSnapshot() { return itemSnapshot; }

    public int slot() { return slot; }

    public int amount() { return amount; }

    public int bidDurationSeconds() {
        return bidDurationSeconds != null ? bidDurationSeconds : -1;
    }

    public double price() { return price; }

    public void price(double price) { this.price = price; }

    public String economyId() { return economyId; }

    public void economyId(String economyId) { this.economyId = economyId; }

    public ItemStack asStack() {
        ItemStack stack = itemSnapshot.clone();
        stack.setAmount(amount);
        return stack;
    }

    public ListingFlags.CreationDraft toFlagDraft(EconomyProvider provider) {
        return new ListingFlags.CreationDraft(itemSnapshot, amount, price, provider, bidDurationSeconds);
    }
}