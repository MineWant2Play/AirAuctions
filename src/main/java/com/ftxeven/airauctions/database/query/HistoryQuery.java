package com.ftxeven.airauctions.database.query;

import com.ftxeven.airauctions.model.ListingType;

import java.util.UUID;

public record HistoryQuery(
        UUID player,
        ListingType listingType,
        String category,
        String economy,
        String search,
        ListingSort sort,
        int page,
        int pageSize,
        Role role
) implements FilterableQuery<HistoryQuery> {

    public HistoryQuery {
        page = Math.max(1, page);
        pageSize = Math.max(1, pageSize);
        role = role != null ? role : Role.EITHER;
    }

    public static Builder builder(int pageSize) {
        return new Builder(pageSize);
    }

    public HistoryQuery withoutCategory() {
        return new HistoryQuery(player, listingType, null, economy, search, sort, page, pageSize, role);
    }

    public HistoryQuery withoutListingType() {
        return new HistoryQuery(player, null, category, economy, search, sort, page, pageSize, role);
    }

    public HistoryQuery withoutEconomy() {
        return new HistoryQuery(player, listingType, category, null, search, sort, page, pageSize, role);
    }

    public enum Role { EITHER, SELLER, BUYER }

    public static final class Builder {

        private final int pageSize;
        private UUID player;
        private ListingType listingType;
        private String category;
        private String economy;
        private String search;
        private ListingSort sort = ListingSort.NEWEST;
        private int page = 1;
        private Role role = Role.EITHER;

        private Builder(int pageSize) {
            this.pageSize = pageSize;
        }

        public Builder player(UUID player) {
            this.player = player;
            return this;
        }

        public Builder listingType(ListingType listingType) {
            this.listingType = listingType;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder economy(String economy) {
            this.economy = economy;
            return this;
        }

        public Builder search(String search) {
            this.search = search;
            return this;
        }

        public Builder sort(ListingSort sort) {
            this.sort = sort;
            return this;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder role(Role role) {
            this.role = role != null ? role : Role.EITHER;
            return this;
        }

        public HistoryQuery build() {
            return new HistoryQuery(player, listingType, category, economy, search, sort, page, pageSize, role);
        }
    }
}