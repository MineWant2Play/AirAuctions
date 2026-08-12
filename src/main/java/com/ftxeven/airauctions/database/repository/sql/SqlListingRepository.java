package com.ftxeven.airauctions.database.repository.sql;

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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public final class SqlListingRepository implements ListingRepository {

    private final DataSource dataSource;
    private final ListingIdGenerator listingIdGenerator;
    private final String listingsTable;
    private final String bidEntriesTable;

    public SqlListingRepository(DataSource dataSource, ListingIdGenerator listingIdGenerator, String tablePrefix) {
        this.dataSource = dataSource;
        this.listingIdGenerator = listingIdGenerator;
        this.listingsTable = tablePrefix + "listings";
        this.bidEntriesTable = tablePrefix + "bid_entries";
    }

    @Override
    public Optional<Listing> find(String id) {
        String sql = "SELECT * FROM " + listingsTable + " WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapListing(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load listing " + id, e);
        }
    }

    @Override
    public List<String> findRecentIds(int limit) {
        String sql = "SELECT id FROM " + listingsTable + " ORDER BY created_at DESC LIMIT ?";
        List<String> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    ids.add(result.getString("id"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load recent listing ids", e);
        }
        return ids;
    }

    @Override
    public PageResult<Listing> query(ListingQuery query) {
        SqlFilter filter = buildFilter(query);

        long total = SqlAggregates.countRows(dataSource, listingsTable, filter);
        if (total == 0) {
            return PageResult.empty(query.page());
        }

        int totalPages = Math.max(1, (int) Math.ceil(total / (double) query.pageSize()));
        int page = Math.min(query.page(), totalPages);

        String sql = "SELECT * FROM " + listingsTable + filter.whereClause()
                + " ORDER BY " + sortColumn(query.sort(), query.scope()) + " " + direction(query.sort())
                + " LIMIT ? OFFSET ?";

        List<Listing> items = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            filter.bind(statement);
            int paramCount = filter.params().size();
            statement.setInt(paramCount + 1, query.pageSize());
            statement.setInt(paramCount + 2, (page - 1) * query.pageSize());

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    items.add(mapListing(result));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not query listings", e);
        }

        return new PageResult<>(items, page, totalPages, total);
    }

    @Override
    public List<Listing> findAll(ListingQuery query) {
        SqlFilter filter = buildFilter(query);
        String sql = "SELECT * FROM " + listingsTable + filter.whereClause()
                + " ORDER BY " + sortColumn(query.sort(), query.scope()) + " " + direction(query.sort());

        List<Listing> items = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            filter.bind(statement);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    items.add(mapListing(result));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load all matching listings", e);
        }
        return items;
    }

    @Override
    public FacetCounts facets(ListingQuery query) {
        Map<String, Long> byCategory = SqlAggregates.countBy(dataSource, listingsTable, "category", buildFilter(query.withoutCategory()));
        Map<String, Long> byEconomy = SqlAggregates.countBy(dataSource, listingsTable, "economy", buildFilter(query.withoutEconomy()));
        Map<String, Long> byListingType = SqlAggregates.countBy(dataSource, listingsTable, "listing_type", buildFilter(query.withoutListingType()));
        return FacetCounts.of(byCategory, byEconomy, byListingType);
    }

    @Override
    public int count(ListingQuery query) {
        return (int) SqlAggregates.countRows(dataSource, listingsTable, buildFilter(query));
    }

    @Override
    public Listing create(Listing listing) {
        String id = listingIdGenerator.next();
        Instant createdAt = Instant.now();
        Listing.Info info = listing.info();

        String sql = "INSERT INTO " + listingsTable
                + " (id, listing_type, status, seller, item, amount, economy, fee, tax, tax_rate, "
                + "category, search_name, created_at, expires_at, ended_at, "
                + "price, remaining_amount, current_price, current_bidder, total_bidders, reminders_shown) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);
            statement.setString(2, listing.type().name());
            statement.setString(3, info.status().name());
            statement.setString(4, info.seller().toString());
            statement.setBytes(5, ItemSerializer.serialize(info.item()));
            statement.setInt(6, info.amount());
            statement.setString(7, info.economy());
            statement.setDouble(8, info.fee());
            statement.setDouble(9, info.tax());
            statement.setDouble(10, info.taxRate());
            statement.setString(11, info.category());
            statement.setString(12, info.searchName());
            statement.setLong(13, createdAt.toEpochMilli());
            statement.setLong(14, info.expiresAt().toEpochMilli());
            setNullableLong(statement, 15, info.endedAt());

            switch (listing) {
                case Listing.Auction auction -> {
                    statement.setDouble(16, auction.price());
                    statement.setInt(17, auction.remainingAmount());
                    statement.setNull(18, Types.DOUBLE);
                    statement.setNull(19, Types.VARCHAR);
                    statement.setInt(20, 0);
                }
                case Listing.Bid bid -> {
                    statement.setDouble(16, bid.startingPrice());
                    statement.setNull(17, Types.INTEGER);
                    statement.setDouble(18, bid.currentPrice());
                    if (bid.currentBidder() != null) {
                        statement.setString(19, bid.currentBidder().toString());
                    } else {
                        statement.setNull(19, Types.VARCHAR);
                    }
                    statement.setInt(20, bid.totalBidders());
                }
            }
            statement.setInt(21, 0); // reminders_shown always starts at 0

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create listing", e);
        }

        return Listing.withGeneratedId(listing, id, createdAt);
    }

    @Override
    public OptionalInt reduceAuctionAmount(String id, int purchasedAmount) {
        String updateSql = "UPDATE " + listingsTable
                + " SET remaining_amount = remaining_amount - ? "
                + "WHERE id = ? AND listing_type = 'AUCTION' AND status = 'ACTIVE' AND remaining_amount >= ?";
        String selectSql = "SELECT remaining_amount FROM " + listingsTable + " WHERE id = ?";

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                    update.setInt(1, purchasedAmount);
                    update.setString(2, id);
                    update.setInt(3, purchasedAmount);
                    if (update.executeUpdate() == 0) {
                        connection.rollback();
                        return OptionalInt.empty();
                    }
                }

                try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                    select.setString(1, id);
                    try (ResultSet result = select.executeQuery()) {
                        result.next();
                        int remaining = result.getInt(1);
                        connection.commit();
                        return OptionalInt.of(remaining);
                    }
                }
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not reduce auction amount for listing " + id, e);
        }
    }

    @Override
    public OptionalInt placeBid(String id, UUID bidder, double offer, Instant newExpiresAt) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!acceptBidPrice(connection, id, bidder, offer, newExpiresAt)) {
                    connection.rollback();
                    return OptionalInt.empty();
                }
                if (upsertBidEntry(connection, id, bidder, offer)) {
                    incrementTotalBidders(connection, id);
                }
                int totalBidders = readTotalBidders(connection, id);
                connection.commit();
                return OptionalInt.of(totalBidders);
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not place bid on listing " + id, e);
        }
    }

    @Override
    public PageResult<BidEntry> bidEntries(String id, int page, int pageSize) {
        long total = countBidEntries(id);
        if (total == 0) {
            return PageResult.empty(page);
        }

        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        int safePage = Math.clamp(page, 1, totalPages);

        String sql = "SELECT bidder, offer, total_offers FROM " + bidEntriesTable + " WHERE listing_id = ? ORDER BY offer DESC LIMIT ? OFFSET ?";
        List<BidEntry> entries = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setInt(2, pageSize);
            statement.setInt(3, (safePage - 1) * pageSize);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    entries.add(new BidEntry(
                            UUID.fromString(result.getString("bidder")),
                            result.getDouble("offer"),
                            result.getInt("total_offers")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load bid entries for listing " + id, e);
        }

        return new PageResult<>(entries, safePage, totalPages, total);
    }

    @Override
    public Optional<BidEntry> bidEntry(String id, UUID bidder) {
        String sql = "SELECT offer, total_offers FROM " + bidEntriesTable + " WHERE listing_id = ? AND bidder = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, bidder.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new BidEntry(bidder, result.getDouble("offer"), result.getInt("total_offers")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load bid entry for listing " + id, e);
        }
    }

    @Override
    public void incrementBidReminders(String id) {
        String sql = "UPDATE " + listingsTable + " SET reminders_shown = reminders_shown + 1 WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not increment bid reminders for listing " + id, e);
        }
    }

    @Override
    public boolean updateStatus(String id, ListingStatus status, Instant endedAt) {
        String sql = "UPDATE " + listingsTable + " SET status = ?, ended_at = ? WHERE id = ? AND status = 'ACTIVE'";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            setNullableLong(statement, 2, endedAt);
            statement.setString(3, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update status for listing " + id, e);
        }
    }

    @Override
    public void delete(String id) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement deleteEntries = connection.prepareStatement(
                        "DELETE FROM " + bidEntriesTable + " WHERE listing_id = ?")) {
                    deleteEntries.setString(1, id);
                    deleteEntries.executeUpdate();
                }
                try (PreparedStatement deleteListing = connection.prepareStatement(
                        "DELETE FROM " + listingsTable + " WHERE id = ?")) {
                    deleteListing.setString(1, id);
                    deleteListing.executeUpdate();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete listing " + id, e);
        }
    }

    @Override
    public List<Listing.Info> findDueToExpire(Instant now) {
        String sql = "SELECT * FROM " + listingsTable + " WHERE status = 'ACTIVE' AND expires_at <= ?";
        List<Listing.Info> due = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, now.toEpochMilli());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    due.add(mapInfo(result));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load listings due to expire", e);
        }
        return due;
    }

    @Override
    public List<Listing.Info> findDueToPurge(ListingScope scope, Instant cutoff) {
        List<String> statuses = statusNames(scope);
        String placeholders = statuses.stream().map(s -> "?").collect(Collectors.joining(", "));
        String sql = "SELECT * FROM " + listingsTable + " WHERE status IN (" + placeholders + ") AND ended_at <= ?";

        List<Listing.Info> due = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int i = 1;
            for (String status : statuses) {
                statement.setString(i++, status);
            }
            statement.setLong(i, cutoff.toEpochMilli());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    due.add(mapInfo(result));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load listings due to purge", e);
        }
        return due;
    }

    @Override
    public int resyncMetadata(ListingMetadataResolver resolver) {
        return SqlMetadataResync.run(dataSource, listingsTable, "id", resolver);
    }

    // Query building

    private SqlFilter buildFilter(ListingQuery query) {
        String ownerColumn = query.scope().ownerRole() == ListingScope.OwnerRole.CURRENT_BIDDER ? "current_bidder" : "seller";
        SqlFilter filter = new SqlFilter()
                .in("status", statusNames(query.scope()))
                .eq(ownerColumn, query.owner() != null ? query.owner().toString() : null)
                .eq("listing_type", query.listingType() != null ? query.listingType().name() : null)
                .eq("category", query.category())
                .eq("economy", query.economy())
                .like("search_name", query.search());
        applyValidityBound(filter, query);
        return filter;
    }

    private void applyValidityBound(SqlFilter filter, ListingQuery query) {
        Instant asOf = query.validAsOf();
        if (asOf == null) {
            return;
        }
        if (query.scope() == ListingScope.ACTIVE) {
            filter.gt("expires_at", asOf.toEpochMilli());
        } else if (query.purgeDelaySeconds() >= 0) {
            filter.gt("ended_at", asOf.minusSeconds(query.purgeDelaySeconds()).toEpochMilli());
        }
    }

    private List<String> statusNames(ListingScope scope) {
        return scope.statuses().stream().map(Enum::name).toList();
    }

    private String sortColumn(ListingSort sort, ListingScope scope) {
        return switch (sort) {
            case NEWEST, OLDEST -> scope == ListingScope.ACTIVE ? "created_at" : "ended_at";
            case PRICE_HIGH, PRICE_LOW -> "COALESCE(current_price, price)";
            case ALPHABETICAL -> "search_name";
            case AMOUNT -> "amount";
        };
    }

    private static String direction(ListingSort sort) {
        return sort.ascending() ? "ASC" : "DESC";
    }

    // Bid placement

    private boolean acceptBidPrice(Connection connection, String id, UUID bidder, double offer, Instant newExpiresAt) throws SQLException {
        String sql = "UPDATE " + listingsTable
                + " SET current_price = ?, current_bidder = ?, expires_at = ? "
                + "WHERE id = ? AND listing_type = 'BID' AND status = 'ACTIVE' AND current_price < ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, offer);
            statement.setString(2, bidder.toString());
            statement.setLong(3, newExpiresAt.toEpochMilli());
            statement.setString(4, id);
            statement.setDouble(5, offer);
            return statement.executeUpdate() > 0;
        }
    }

    private boolean upsertBidEntry(Connection connection, String id, UUID bidder, double offer) throws SQLException {
        String updateSql = "UPDATE " + bidEntriesTable + " SET offer = ?, total_offers = total_offers + 1 WHERE listing_id = ? AND bidder = ?";
        try (PreparedStatement update = connection.prepareStatement(updateSql)) {
            update.setDouble(1, offer);
            update.setString(2, id);
            update.setString(3, bidder.toString());
            if (update.executeUpdate() > 0) {
                return false;
            }
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO " + bidEntriesTable + " (listing_id, bidder, offer, total_offers) VALUES (?, ?, ?, 1)")) {
            insert.setString(1, id);
            insert.setString(2, bidder.toString());
            insert.setDouble(3, offer);
            insert.executeUpdate();
        }
        return true;
    }

    private void incrementTotalBidders(Connection connection, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE " + listingsTable + " SET total_bidders = total_bidders + 1 WHERE id = ?")) {
            statement.setString(1, id);
            statement.executeUpdate();
        }
    }

    private int readTotalBidders(Connection connection, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT total_bidders FROM " + listingsTable + " WHERE id = ?")) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private long countBidEntries(String id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM " + bidEntriesTable + " WHERE listing_id = ?")) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not count bid entries for listing " + id, e);
        }
    }

    // Row mapping

    private Listing mapListing(ResultSet rs) throws SQLException {
        Listing.Info info = mapInfo(rs);
        double price = rs.getDouble("price");

        if ("AUCTION".equals(rs.getString("listing_type"))) {
            return new Listing.Auction(info, price, rs.getInt("remaining_amount"));
        }

        Double currentPrice = getNullableDouble(rs, "current_price");
        String bidderRaw = rs.getString("current_bidder");
        UUID currentBidder = bidderRaw != null ? UUID.fromString(bidderRaw) : null;
        return new Listing.Bid(info, price, currentPrice != null ? currentPrice : price, currentBidder,
                rs.getInt("total_bidders"), rs.getInt("reminders_shown"));
    }

    private Listing.Info mapInfo(ResultSet rs) throws SQLException {
        return new Listing.Info(
                rs.getString("id"),
                UUID.fromString(rs.getString("seller")),
                ItemSerializer.deserialize(rs.getBytes("item")),
                rs.getInt("amount"),
                rs.getString("economy"),
                rs.getDouble("fee"),
                rs.getDouble("tax"),
                rs.getDouble("tax_rate"),
                rs.getString("category"),
                rs.getString("search_name"),
                Instant.ofEpochMilli(rs.getLong("created_at")),
                Instant.ofEpochMilli(rs.getLong("expires_at")),
                getNullableInstant(rs, "ended_at"),
                ListingStatus.valueOf(rs.getString("status"))
        );
    }

    private static void setNullableLong(PreparedStatement statement, int index, Instant value) throws SQLException {
        if (value != null) {
            statement.setLong(index, value.toEpochMilli());
        } else {
            statement.setNull(index, Types.BIGINT);
        }
    }

    private static Instant getNullableInstant(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : Instant.ofEpochMilli(value);
    }

    private static Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }
}