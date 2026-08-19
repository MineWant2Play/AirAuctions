package com.ftxeven.airauctions.database.query;

import com.ftxeven.airauctions.model.ListingType;

import java.util.EnumMap;
import java.util.Map;

// backs the %count% placeholder in the category/type/economy filter cyclers
public record FacetCounts(
        Map<String, Long> byCategory,
        Map<ListingType, Long> byListingType,
        Map<String, Long> byEconomy
) {

    public static FacetCounts of(Map<String, Long> byCategory, Map<String, Long> byEconomy, Map<String, Long> rawListingTypeCounts) {
        Map<ListingType, Long> byListingType = new EnumMap<>(ListingType.class);
        rawListingTypeCounts.forEach((key, value) -> byListingType.put(ListingType.valueOf(key), value));
        return new FacetCounts(Map.copyOf(byCategory), Map.copyOf(byListingType), Map.copyOf(byEconomy));
    }
}