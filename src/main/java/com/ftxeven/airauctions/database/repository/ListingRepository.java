package com.ftxeven.airauctions.database.repository;

import com.ftxeven.airauctions.database.query.FacetCounts;
import com.ftxeven.airauctions.database.query.ListingQuery;
import com.ftxeven.airauctions.database.query.PageResult;
import com.ftxeven.airauctions.model.BidEntry;
import com.ftxeven.airauctions.model.Listing;
import com.ftxeven.airauctions.model.ListingScope;
import com.ftxeven.airauctions.model.ListingStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public interface ListingRepository {

    Optional<Listing> find(String id);

    PageResult<Listing> query(ListingQuery query);

    // every matching listing, ignoring page/pageSize
    List<Listing> findAll(ListingQuery query);

    // ids only
    List<String> findRecentIds(int limit);

    FacetCounts facets(ListingQuery query);

    // equivalent to findAll(ListingQuery.owned(owner, scope)).size(), without materializing the list
    int count(ListingQuery query);

    Listing create(Listing listing);

    OptionalInt reduceAuctionAmount(String id, int purchasedAmount);

    OptionalInt placeBid(String id, UUID bidder, double offer, Instant newExpiresAt);

    PageResult<BidEntry> bidEntries(String id, int page, int pageSize);

    Optional<BidEntry> bidEntry(String id, UUID bidder);

    // bumps how many times the won-bid reminder has been shown for this bid
    void incrementBidReminders(String id);

    boolean updateStatus(String id, ListingStatus status, Instant endedAt);

    void delete(String id);

    // ACTIVE listings whose expiresAt is already past
    List<Listing.Info> findDueToExpire(Instant now);

    // EXPIRED/ENDED listings whose endedAt is past cutoff
    List<Listing.Info> findDueToPurge(ListingScope scope, Instant cutoff);

    int resyncMetadata(ListingMetadataResolver resolver);
}