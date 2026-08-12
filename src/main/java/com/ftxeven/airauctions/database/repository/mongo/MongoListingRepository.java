package com.ftxeven.airauctions.database.repository.mongo;

import com.ftxeven.airauctions.database.id.ListingIdGenerator;
import com.ftxeven.airauctions.database.query.FacetCounts;
import com.ftxeven.airauctions.database.query.ListingQuery;
import com.ftxeven.airauctions.database.query.ListingSort;
import com.ftxeven.airauctions.database.query.PageResult;
import com.ftxeven.airauctions.database.repository.ListingMetadataResolver;
import com.ftxeven.airauctions.database.repository.ListingRepository;
import com.ftxeven.airauctions.model.BidEntry;
import com.ftxeven.airauctions.model.Listing;
import com.ftxeven.airauctions.model.ListingScope;
import com.ftxeven.airauctions.model.ListingStatus;
import com.ftxeven.airauctions.util.ItemSerializer;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;

import java.time.Instant;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;

public final class MongoListingRepository implements ListingRepository {

    private final MongoCollection<Document> listings;
    private final ListingIdGenerator listingIdGenerator;

    public MongoListingRepository(MongoDatabase database, ListingIdGenerator listingIdGenerator, String tablePrefix) {
        this.listings = database.getCollection(tablePrefix + "listings");
        this.listingIdGenerator = listingIdGenerator;
    }

    @Override
    public Optional<Listing> find(String id) {
        Document doc = listings.find(eq("_id", id)).first();
        return doc != null ? Optional.of(mapListing(doc)) : Optional.empty();
    }

    @Override
    public List<String> findRecentIds(int limit) {
        List<String> ids = new ArrayList<>();
        for (Document doc : listings.find()
                .projection(Projections.include("_id"))
                .sort(Sorts.descending("createdAt"))
                .limit(limit)) {
            ids.add(doc.getString("_id"));
        }
        return ids;
    }

    @Override
    public PageResult<Listing> query(ListingQuery query) {
        Bson filter = buildFilter(query);
        long total = MongoAggregates.countDocuments(listings, filter);
        if (total == 0) {
            return PageResult.empty(query.page());
        }

        int totalPages = Math.max(1, (int) Math.ceil(total / (double) query.pageSize()));
        int page = Math.min(query.page(), totalPages);

        List<Listing> items = new ArrayList<>();
        for (Document doc : listings.find(filter)
                .sort(buildSort(query.sort(), query.scope()))
                .skip((page - 1) * query.pageSize())
                .limit(query.pageSize())) {
            items.add(mapListing(doc));
        }

        return new PageResult<>(items, page, totalPages, total);
    }

    @Override
    public List<Listing> findAll(ListingQuery query) {
        Bson filter = buildFilter(query);
        List<Listing> items = new ArrayList<>();
        for (Document doc : listings.find(filter).sort(buildSort(query.sort(), query.scope()))) {
            items.add(mapListing(doc));
        }
        return items;
    }

    @Override
    public FacetCounts facets(ListingQuery query) {
        Map<String, Long> byCategory = MongoAggregates.countBy(listings, "category", buildFilter(query.withoutCategory()));
        Map<String, Long> byEconomy = MongoAggregates.countBy(listings, "economy", buildFilter(query.withoutEconomy()));
        Map<String, Long> byListingType = MongoAggregates.countBy(listings, "listingType", buildFilter(query.withoutListingType()));
        return FacetCounts.of(byCategory, byEconomy, byListingType);
    }

    @Override
    public int count(ListingQuery query) {
        return (int) MongoAggregates.countDocuments(listings, buildFilter(query));
    }

    @Override
    public Listing create(Listing listing) {
        String id = listingIdGenerator.next();
        Instant createdAt = Instant.now();
        Listing.Info info = listing.info();

        Document doc = new Document("_id", id)
                .append("listingType", listing.type().name())
                .append("status", info.status().name())
                .append("seller", info.seller().toString())
                .append("item", new Binary(ItemSerializer.serialize(info.item())))
                .append("amount", info.amount())
                .append("economy", info.economy())
                .append("fee", info.fee())
                .append("tax", info.tax())
                .append("taxRate", info.taxRate())
                .append("category", info.category())
                .append("searchName", info.searchName())
                .append("createdAt", Date.from(createdAt))
                .append("expiresAt", Date.from(info.expiresAt()))
                .append("endedAt", info.endedAt() != null ? Date.from(info.endedAt()) : null)
                .append("bidEntries", List.of());

        switch (listing) {
            case Listing.Auction auction -> doc.append("price", auction.price())
                    .append("remainingAmount", auction.remainingAmount())
                    .append("currentPrice", null)
                    .append("currentBidder", null)
                    .append("totalBidders", 0)
                    .append("remindersShown", 0)
                    .append("sortPrice", auction.price());
            case Listing.Bid bid -> doc.append("price", bid.startingPrice())
                    .append("remainingAmount", null)
                    .append("currentPrice", bid.currentPrice())
                    .append("currentBidder", bid.currentBidder() != null ? bid.currentBidder().toString() : null)
                    .append("totalBidders", bid.totalBidders())
                    .append("remindersShown", bid.remindersShown())
                    .append("sortPrice", bid.currentPrice());
        }

        listings.insertOne(doc);
        return Listing.withGeneratedId(listing, id, createdAt);
    }

    @Override
    public OptionalInt reduceAuctionAmount(String id, int purchasedAmount) {
        Bson filter = Filters.and(
                eq("_id", id),
                eq("listingType", "AUCTION"),
                eq("status", "ACTIVE"),
                Filters.gte("remainingAmount", purchasedAmount));
        Document result = listings.findOneAndUpdate(filter, Updates.inc("remainingAmount", -purchasedAmount),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
        return result != null ? OptionalInt.of(result.getInteger("remainingAmount")) : OptionalInt.empty();
    }

    @Override
    public OptionalInt placeBid(String id, UUID bidder, double offer, Instant newExpiresAt) {
        Bson priceFilter = Filters.and(eq("_id", id), eq("listingType", "BID"), eq("status", "ACTIVE"), Filters.lt("currentPrice", offer));
        Bson priceUpdate = Updates.combine(
                Updates.set("currentPrice", offer),
                Updates.set("sortPrice", offer),
                Updates.set("currentBidder", bidder.toString()),
                Updates.set("expiresAt", Date.from(newExpiresAt)));

        if (listings.updateOne(priceFilter, priceUpdate).getModifiedCount() == 0) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(upsertBidEntry(id, bidder, offer));
    }

    @Override
    public PageResult<BidEntry> bidEntries(String id, int page, int pageSize) {
        Document doc = listings.find(eq("_id", id)).projection(Projections.include("bidEntries")).first();
        List<BidEntry> all = doc != null ? mapBidEntries(doc) : List.of();

        long total = all.size();
        if (total == 0) {
            return PageResult.empty(page);
        }

        List<BidEntry> sorted = all.stream()
                .sorted(Comparator.comparingDouble(BidEntry::offer).reversed())
                .toList();

        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        int safePage = Math.clamp(page, 1, totalPages);
        int from = (safePage - 1) * pageSize;
        int to = Math.min(from + pageSize, sorted.size());

        return new PageResult<>(sorted.subList(from, to), safePage, totalPages, total);
    }

    @Override
    public Optional<BidEntry> bidEntry(String id, UUID bidder) {
        Document doc = listings.find(eq("_id", id)).projection(Projections.include("bidEntries")).first();
        if (doc == null) {
            return Optional.empty();
        }
        return mapBidEntries(doc).stream().filter(entry -> entry.bidder().equals(bidder)).findFirst();
    }

    @Override
    public void incrementBidReminders(String id) {
        listings.updateOne(eq("_id", id), Updates.inc("remindersShown", 1));
    }

    @Override
    public boolean updateStatus(String id, ListingStatus status, Instant endedAt) {
        Bson filter = Filters.and(eq("_id", id), eq("status", "ACTIVE"));
        Bson update = Updates.combine(
                Updates.set("status", status.name()),
                Updates.set("endedAt", endedAt != null ? Date.from(endedAt) : null));
        return listings.updateOne(filter, update).getModifiedCount() > 0;
    }

    @Override
    public void delete(String id) {
        listings.deleteOne(eq("_id", id));
    }

    @Override
    public List<Listing.Info> findDueToExpire(Instant now) {
        Bson filter = Filters.and(eq("status", "ACTIVE"), Filters.lte("expiresAt", Date.from(now)));
        List<Listing.Info> due = new ArrayList<>();
        for (Document doc : listings.find(filter)) {
            due.add(mapInfo(doc));
        }
        return due;
    }

    @Override
    public List<Listing.Info> findDueToPurge(ListingScope scope, Instant cutoff) {
        Bson filter = Filters.and(
                Filters.in("status", statusNames(scope)),
                Filters.lte("endedAt", Date.from(cutoff)));

        List<Listing.Info> due = new ArrayList<>();
        for (Document doc : listings.find(filter)) {
            due.add(mapInfo(doc));
        }
        return due;
    }

    @Override
    public int resyncMetadata(ListingMetadataResolver resolver) {
        return MongoMetadataResync.run(listings, resolver);
    }

    // Query building

    private Bson buildFilter(ListingQuery query) {
        String ownerField = query.scope().ownerRole() == ListingScope.OwnerRole.CURRENT_BIDDER ? "currentBidder" : "seller";
        BsonFilter filter = new BsonFilter()
                .in("status", statusNames(query.scope()))
                .eq(ownerField, query.owner() != null ? query.owner().toString() : null)
                .eq("listingType", query.listingType() != null ? query.listingType().name() : null)
                .eq("category", query.category())
                .eq("economy", query.economy())
                .like("searchName", query.search());
        applyValidityBound(filter, query);
        return filter.build();
    }

    private void applyValidityBound(BsonFilter filter, ListingQuery query) {
        Instant asOf = query.validAsOf();
        if (asOf == null) {
            return;
        }
        if (query.scope() == ListingScope.ACTIVE) {
            filter.gt("expiresAt", Date.from(asOf));
        } else if (query.purgeDelaySeconds() >= 0) {
            filter.gt("endedAt", Date.from(asOf.minusSeconds(query.purgeDelaySeconds())));
        }
    }

    private List<String> statusNames(ListingScope scope) {
        return scope.statuses().stream().map(Enum::name).toList();
    }

    private Bson buildSort(ListingSort sort, ListingScope scope) {
        String field = switch (sort) {
            case NEWEST, OLDEST -> scope == ListingScope.ACTIVE ? "createdAt" : "endedAt";
            case PRICE_HIGH, PRICE_LOW -> "sortPrice";
            case ALPHABETICAL -> "searchName";
            case AMOUNT -> "amount";
        };
        return sort.ascending() ? Sorts.ascending(field) : Sorts.descending(field);
    }

    // Bid placement

    private int upsertBidEntry(String id, UUID bidder, double offer) {
        String bidderId = bidder.toString();
        Bson matchFilter = Filters.and(eq("_id", id), Filters.elemMatch("bidEntries", eq("bidder", bidderId)));
        Bson matchUpdate = Updates.combine(
                Updates.set("bidEntries.$.offer", offer),
                Updates.inc("bidEntries.$.totalOffers", 1));

        Document updated = listings.findOneAndUpdate(matchFilter, matchUpdate,
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER).projection(Projections.include("totalBidders")));
        if (updated != null) {
            return updated.getInteger("totalBidders");
        }

        Document entry = new Document("bidder", bidderId).append("offer", offer).append("totalOffers", 1);
        Document inserted = listings.findOneAndUpdate(eq("_id", id),
                Updates.combine(Updates.push("bidEntries", entry), Updates.inc("totalBidders", 1)),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER).projection(Projections.include("totalBidders")));
        return inserted.getInteger("totalBidders");
    }

    // Row mapping

    private Listing mapListing(Document doc) {
        Listing.Info info = mapInfo(doc);
        double price = doc.getDouble("price");

        if ("AUCTION".equals(doc.getString("listingType"))) {
            return new Listing.Auction(info, price, doc.getInteger("remainingAmount"));
        }

        Double currentPrice = doc.getDouble("currentPrice");
        String bidderRaw = doc.getString("currentBidder");
        UUID currentBidder = bidderRaw != null ? UUID.fromString(bidderRaw) : null;
        return new Listing.Bid(info, price, currentPrice != null ? currentPrice : price, currentBidder,
                doc.getInteger("totalBidders"), doc.getInteger("remindersShown"));
    }

    private Listing.Info mapInfo(Document doc) {
        Binary item = doc.get("item", Binary.class);
        Date endedAt = doc.getDate("endedAt");

        return new Listing.Info(
                doc.getString("_id"),
                UUID.fromString(doc.getString("seller")),
                ItemSerializer.deserialize(item.getData()),
                doc.getInteger("amount"),
                doc.getString("economy"),
                doc.getDouble("fee"),
                doc.getDouble("tax"),
                doc.getDouble("taxRate"),
                doc.getString("category"),
                doc.getString("searchName"),
                doc.getDate("createdAt").toInstant(),
                doc.getDate("expiresAt").toInstant(),
                endedAt != null ? endedAt.toInstant() : null,
                ListingStatus.valueOf(doc.getString("status"))
        );
    }

    private List<BidEntry> mapBidEntries(Document doc) {
        List<Document> raw = doc.getList("bidEntries", Document.class, List.of());
        List<BidEntry> entries = new ArrayList<>(raw.size());
        for (Document entry : raw) {
            entries.add(new BidEntry(UUID.fromString(entry.getString("bidder")), entry.getDouble("offer"), entry.getInteger("totalOffers")));
        }
        return entries;
    }
}