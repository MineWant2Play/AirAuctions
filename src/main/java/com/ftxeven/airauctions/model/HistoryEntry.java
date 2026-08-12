package com.ftxeven.airauctions.model;

import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.UUID;

public sealed interface HistoryEntry {

    Info info();

    default ListingType type() {
        return switch (this) {
            case Auction a -> ListingType.AUCTION;
            case Bid b -> ListingType.BID;
        };
    }

    default ItemStack displayItem() {
        ItemStack item = info().item().clone();
        item.setAmount(Math.max(1, info().amount()));
        return item;
    }

    record Info(
            String id,
            UUID seller,
            UUID buyer,
            ItemStack item,
            int amount,
            String economy,
            double fee,
            double tax,
            String category,
            String searchName,
            Instant createdAt,
            Instant completedAt
    ) {
        public Info {
            item = item.clone();
        }
    }

    record Auction(
            Info info,
            double price
    ) implements HistoryEntry {}

    /** written once, only when a bid ends with a winner */
    record Bid(
            Info info,
            double startingPrice,
            double finalPrice,
            int totalBidders
    ) implements HistoryEntry {}
}