package com.ftxeven.airauctions.database.query;

public interface FilterableQuery<Q extends FilterableQuery<Q>> {
    Q withoutCategory();
    Q withoutListingType();
    Q withoutEconomy();
}