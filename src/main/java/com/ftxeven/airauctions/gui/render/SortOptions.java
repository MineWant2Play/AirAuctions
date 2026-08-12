package com.ftxeven.airauctions.gui.render;

import com.ftxeven.airauctions.database.query.ListingSort;

import java.util.HashMap;
import java.util.Map;

public final class SortOptions {

    private static final Map<String, ListingSort> COMMON = Map.of(
            "highest_price", ListingSort.PRICE_HIGH,
            "lowest_price", ListingSort.PRICE_LOW,
            "alphabetical", ListingSort.ALPHABETICAL,
            "amount", ListingSort.AMOUNT
    );

    public static final Map<String, ListingSort> ACTIVE = withExtra(
            "newest_date", ListingSort.NEWEST,
            "oldest_date", ListingSort.OLDEST
    );

    // shared by expired and storage
    public static final Map<String, ListingSort> UNCLAIMED = withExtra(
            "purges_soonest", ListingSort.OLDEST,
            "purges_latest", ListingSort.NEWEST
    );

    public static final Map<String, ListingSort> HISTORY = withExtra(
            "newest_transaction", ListingSort.NEWEST,
            "oldest_transaction", ListingSort.OLDEST
    );

    private SortOptions() {
    }

    private static Map<String, ListingSort> withExtra(String keyA, ListingSort sortA, String keyB, ListingSort sortB) {
        Map<String, ListingSort> combined = new HashMap<>(COMMON);
        combined.put(keyA, sortA);
        combined.put(keyB, sortB);
        return Map.copyOf(combined);
    }
}