package com.ftxeven.airauctions.model;

import java.util.Set;

// which lifecycle bucket a listing query targets
public enum ListingScope {

    ACTIVE(Set.of(ListingStatus.ACTIVE), OwnerRole.SELLER),
    EXPIRED(Set.of(ListingStatus.EXPIRED, ListingStatus.CANCELLED), OwnerRole.SELLER),
    STORAGE(Set.of(ListingStatus.ENDED, ListingStatus.UNCOLLECTED), OwnerRole.CURRENT_BIDDER);

    private final Set<ListingStatus> statuses;
    private final OwnerRole ownerRole;

    ListingScope(Set<ListingStatus> statuses, OwnerRole ownerRole) {
        this.statuses = statuses;
        this.ownerRole = ownerRole;
    }

    public Set<ListingStatus> statuses() {
        return statuses;
    }

    // which side of the listing "owns" it for this scope
    public OwnerRole ownerRole() {
        return ownerRole;
    }

    // the scope a status currently belongs to - the inverse of statuses(). Every
    // ListingStatus falls under exactly one scope
    public static ListingScope forStatus(ListingStatus status) {
        return switch (status) {
            case ACTIVE -> ACTIVE;
            case EXPIRED, CANCELLED -> EXPIRED;
            case ENDED, UNCOLLECTED -> STORAGE;
        };
    }

    public enum OwnerRole { SELLER, CURRENT_BIDDER }
}