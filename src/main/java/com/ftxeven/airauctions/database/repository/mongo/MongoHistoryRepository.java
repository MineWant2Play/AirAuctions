package com.ftxeven.airauctions.database.repository.mongo;

import com.ftxeven.airauctions.database.query.*;
import com.ftxeven.airauctions.database.repository.HistoryRepository;
import com.ftxeven.airauctions.database.repository.ListingMetadataResolver;
import com.ftxeven.airauctions.model.HistoryEntry;
import com.ftxeven.airauctions.util.ItemSerializer;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;

public final class MongoHistoryRepository implements HistoryRepository {

    private final MongoCollection<Document> history;

    public MongoHistoryRepository(MongoDatabase database, String tablePrefix) {
        this.history = database.getCollection(tablePrefix + "history");
    }

    @Override
    public PageResult<HistoryEntry> query(HistoryQuery query) {
        Bson filter = buildFilter(query);
        long total = MongoAggregates.countDocuments(history, filter);
        if (total == 0) {
            return PageResult.empty(query.page());
        }

        int totalPages = Math.max(1, (int) Math.ceil(total / (double) query.pageSize()));
        int page = Math.min(query.page(), totalPages);

        List<HistoryEntry> items = new ArrayList<>();
        for (Document doc : history.find(filter)
                .sort(buildSort(query.sort()))
                .skip((page - 1) * query.pageSize())
                .limit(query.pageSize())) {
            items.add(mapEntry(doc));
        }

        return new PageResult<>(items, page, totalPages, total);
    }

    @Override
    public FacetCounts facets(HistoryQuery query) {
        Map<String, Long> byCategory = MongoAggregates.countBy(history, "category", buildFilter(query.withoutCategory()));
        Map<String, Long> byEconomy = MongoAggregates.countBy(history, "economy", buildFilter(query.withoutEconomy()));
        Map<String, Long> byListingType = MongoAggregates.countBy(history, "listingType", buildFilter(query.withoutListingType()));
        return FacetCounts.of(byCategory, byEconomy, byListingType);
    }

    @Override
    public void append(HistoryEntry entry) {
        HistoryEntry.Info info = entry.info();

        double sortPrice = switch (entry) {
            case HistoryEntry.Auction auction -> auction.price();
            case HistoryEntry.Bid bid -> bid.finalPrice();
        };

        Document doc = new Document()
                .append("listingId", info.id())
                .append("listingType", entry.type().name())
                .append("seller", info.seller().toString())
                .append("buyer", info.buyer().toString())
                .append("item", new Binary(ItemSerializer.serialize(info.item())))
                .append("amount", info.amount())
                .append("economy", info.economy())
                .append("fee", info.fee())
                .append("tax", info.tax())
                .append("category", info.category())
                .append("searchName", info.searchName())
                .append("createdAt", Date.from(info.createdAt()))
                .append("completedAt", Date.from(info.completedAt()))
                .append("sortPrice", sortPrice);

        switch (entry) {
            case HistoryEntry.Auction auction -> doc.append("price", auction.price())
                    .append("startingPrice", null)
                    .append("finalPrice", null)
                    .append("totalBidders", null);
            case HistoryEntry.Bid bid -> doc.append("price", null)
                    .append("startingPrice", bid.startingPrice())
                    .append("finalPrice", bid.finalPrice())
                    .append("totalBidders", bid.totalBidders());
        }

        history.insertOne(doc);
    }

    @Override
    public Optional<HistoryEntry> find(String listingId, Instant completedAt) {
        Document doc = history.find(Filters.and(eq("listingId", listingId), eq("completedAt", Date.from(completedAt)))).first();
        return doc != null ? Optional.of(mapEntry(doc)) : Optional.empty();
    }

    @Override
    public void trim(UUID player, int maxEntries) {
        if (maxEntries <= 0) {
            return;
        }

        Bson playerFilter = Filters.or(eq("seller", player.toString()), eq("buyer", player.toString()));
        List<ObjectId> overflowIds = new ArrayList<>();

        try (MongoCursor<Document> cursor = history.find(playerFilter)
                .sort(Sorts.descending("completedAt"))
                .skip(maxEntries)
                .projection(Projections.include("_id"))
                .iterator()) {
            while (cursor.hasNext()) {
                overflowIds.add(cursor.next().getObjectId("_id"));
            }
        }

        if (!overflowIds.isEmpty()) {
            history.deleteMany(Filters.in("_id", overflowIds));
        }
    }

    @Override
    public Map<String, Double> sumForPlayer(UUID player, TransactionKind kind, @Nullable Instant since) {
        String roleField = kind == TransactionKind.EARNED ? "seller" : "buyer";
        return sum(eq(roleField, player.toString()), kind == TransactionKind.EARNED, since);
    }

    @Override
    public Map<String, Double> sumGlobalVolume(@Nullable Instant since) {
        return sum(null, false, since);
    }

    private Map<String, Double> sum(@Nullable Bson roleFilter, boolean subtractTax, @Nullable Instant since) {
        List<Bson> matches = new ArrayList<>();
        if (roleFilter != null) {
            matches.add(roleFilter);
        }
        if (since != null) {
            matches.add(Filters.gte("completedAt", Date.from(since)));
        }

        List<Bson> pipeline = new ArrayList<>();
        if (!matches.isEmpty()) {
            pipeline.add(Aggregates.match(Filters.and(matches)));
        }
        Object valueExpr = subtractTax ? new Document("$subtract", List.of("$sortPrice", "$tax")) : "$sortPrice";
        pipeline.add(Aggregates.group("$economy", Accumulators.sum("total", valueExpr)));

        Map<String, Double> totals = new LinkedHashMap<>();
        for (Document doc : history.aggregate(pipeline)) {
            totals.put(doc.getString("_id"), doc.get("total", Number.class).doubleValue());
        }
        return totals;
    }

    @Override
    public int resyncMetadata(ListingMetadataResolver resolver) {
        return MongoMetadataResync.run(history, resolver);
    }

    private Bson buildFilter(HistoryQuery query) {
        BsonFilter filter = new BsonFilter();
        if (query.player() != null) {
            String uuid = query.player().toString();
            switch (query.role()) {
                case SELLER -> filter.eq("seller", uuid);
                case BUYER -> filter.eq("buyer", uuid);
                case EITHER -> filter.eqEither("seller", "buyer", uuid);
            }
        }
        return filter
                .eq("listingType", query.listingType() != null ? query.listingType().name() : null)
                .eq("category", query.category())
                .eq("economy", query.economy())
                .like("searchName", query.search())
                .build();
    }

    private Bson buildSort(ListingSort sort) {
        String field = switch (sort) {
            case NEWEST, OLDEST -> "completedAt";
            case PRICE_HIGH, PRICE_LOW -> "sortPrice";
            case ALPHABETICAL -> "searchName";
            case AMOUNT -> "amount";
        };
        return sort.ascending() ? Sorts.ascending(field) : Sorts.descending(field);
    }

    private HistoryEntry mapEntry(Document doc) {
        HistoryEntry.Info info = mapInfo(doc);

        if ("AUCTION".equals(doc.getString("listingType"))) {
            return new HistoryEntry.Auction(info, doc.getDouble("price"));
        }
        return new HistoryEntry.Bid(info, doc.getDouble("startingPrice"), doc.getDouble("finalPrice"), doc.getInteger("totalBidders"));
    }

    private HistoryEntry.Info mapInfo(Document doc) {
        Binary item = doc.get("item", Binary.class);
        return new HistoryEntry.Info(
                doc.getString("listingId"),
                UUID.fromString(doc.getString("seller")),
                UUID.fromString(doc.getString("buyer")),
                ItemSerializer.deserialize(item.getData()),
                doc.getInteger("amount"),
                doc.getString("economy"),
                doc.getDouble("fee"),
                doc.getDouble("tax"),
                doc.getString("category"),
                doc.getString("searchName"),
                doc.getDate("createdAt").toInstant(),
                doc.getDate("completedAt").toInstant()
        );
    }
}