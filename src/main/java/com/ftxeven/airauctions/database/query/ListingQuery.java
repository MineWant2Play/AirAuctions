package com.ftxeven.airauctions.database.query;

import com.ftxeven.airauctions.model.ListingScope;
import com.ftxeven.airauctions.model.ListingType;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record ListingQuery(
        UUID owner,
        ListingScope scope,
        ListingType listingType,
        String category,
        String economy,
        String search,
        ListingSort sort,
        int page,
        int pageSize,
        @Nullable Instant validAsOf,
        int purgeDelaySeconds
) implements FilterableQuery<ListingQuery> {

    public ListingQuery {
        page = Math.max(1, page);
        pageSize = Math.max(1, pageSize);
    }

    public static Builder builder(ListingScope scope, int pageSize) {
        return new Builder(scope, pageSize);
    }

    public static ListingQuery owned(UUID owner, ListingScope scope) {
        return builder(scope, 1).owner(owner).build();
    }

    public ListingQuery withoutCategory() {
        return new ListingQuery(owner, scope, listingType, null, economy, search, sort, page, pageSize, validAsOf, purgeDelaySeconds);
    }

    public ListingQuery withoutListingType() {
        return new ListingQuery(owner, scope, null, category, economy, search, sort, page, pageSize, validAsOf, purgeDelaySeconds);
    }

    public ListingQuery withoutEconomy() {
        return new ListingQuery(owner, scope, listingType, category, null, search, sort, page, pageSize, validAsOf, purgeDelaySeconds);
    }

    public ListingQuery validAsOf(Instant now, int purgeDelaySeconds) {
        return new ListingQuery(owner, scope, listingType, category, economy, search, sort, page, pageSize, now, purgeDelaySeconds);
    }

    public static final class Builder {

        private final ListingScope scope;
        private final int pageSize;
        private UUID owner;
        private ListingType listingType;
        private String category;
        private String economy;
        private String search;
        private ListingSort sort = ListingSort.NEWEST;
        private int page = 1;

        private Builder(ListingScope scope, int pageSize) {
            this.scope = scope;
            this.pageSize = pageSize;
        }

        public Builder owner(UUID owner) {
            this.owner = owner;
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

        public ListingQuery build() {
            return new ListingQuery(owner, scope, listingType, category, economy, search, sort, page, pageSize, null, -1);
        }
    }
}