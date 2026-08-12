package com.ftxeven.airauctions.database.query;

public enum ListingSort {
    NEWEST(false),
    OLDEST(true),
    PRICE_HIGH(false),
    PRICE_LOW(true),
    ALPHABETICAL(true),
    AMOUNT(false);

    private final boolean ascending;

    ListingSort(boolean ascending) {
        this.ascending = ascending;
    }

    public boolean ascending() {
        return ascending;
    }
}