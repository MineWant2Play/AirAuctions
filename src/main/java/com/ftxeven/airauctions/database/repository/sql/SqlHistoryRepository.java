package com.ftxeven.airauctions.database.repository.sql;

import com.ftxeven.airauctions.database.query.*;
import com.ftxeven.airauctions.database.repository.HistoryRepository;
import com.ftxeven.airauctions.database.repository.ListingMetadataResolver;
import com.ftxeven.airauctions.model.HistoryEntry;
import com.ftxeven.airauctions.util.ItemSerializer;

import org.jetbrains.annotations.Nullable;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.*;

public final class SqlHistoryRepository implements HistoryRepository {

    private final DataSource dataSource;
    private final String table;

    public SqlHistoryRepository(DataSource dataSource, String tablePrefix) {
        this.dataSource = dataSource;
        this.table = tablePrefix + "history";
    }

    @Override
    public PageResult<HistoryEntry> query(HistoryQuery query) {
        SqlFilter filter = buildFilter(query);

        long total = SqlAggregates.countRows(dataSource, table, filter);
        if (total == 0) {
            return PageResult.empty(query.page());
        }

        int totalPages = Math.max(1, (int) Math.ceil(total / (double) query.pageSize()));
        int page = Math.min(query.page(), totalPages);

        String sql = "SELECT * FROM " + table + filter.whereClause()
                + " ORDER BY " + sortColumn(query.sort()) + " " + direction(query.sort())
                + " LIMIT ? OFFSET ?";

        List<HistoryEntry> items = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            filter.bind(statement);
            int paramCount = filter.params().size();
            statement.setInt(paramCount + 1, query.pageSize());
            statement.setInt(paramCount + 2, (page - 1) * query.pageSize());

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    items.add(mapEntry(result));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not query history", e);
        }

        return new PageResult<>(items, page, totalPages, total);
    }

    @Override
    public FacetCounts facets(HistoryQuery query) {
        Map<String, Long> byCategory = SqlAggregates.countBy(dataSource, table, "category", buildFilter(query.withoutCategory()));
        Map<String, Long> byEconomy = SqlAggregates.countBy(dataSource, table, "economy", buildFilter(query.withoutEconomy()));
        Map<String, Long> byListingType = SqlAggregates.countBy(dataSource, table, "listing_type", buildFilter(query.withoutListingType()));
        return FacetCounts.of(byCategory, byEconomy, byListingType);
    }

    @Override
    public void append(HistoryEntry entry) {
        HistoryEntry.Info info = entry.info();
        String sql = "INSERT INTO " + table
                + " (listing_id, listing_type, seller, buyer, item, amount, economy, fee, tax, "
                + "category, search_name, created_at, completed_at, price, starting_price, final_price, total_bidders) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, info.id());
            statement.setString(2, entry.type().name());
            statement.setString(3, info.seller().toString());
            statement.setString(4, info.buyer().toString());
            statement.setBytes(5, ItemSerializer.serialize(info.item()));
            statement.setInt(6, info.amount());
            statement.setString(7, info.economy());
            statement.setDouble(8, info.fee());
            statement.setDouble(9, info.tax());
            statement.setString(10, info.category());
            statement.setString(11, info.searchName());
            statement.setLong(12, info.createdAt().toEpochMilli());
            statement.setLong(13, info.completedAt().toEpochMilli());

            switch (entry) {
                case HistoryEntry.Auction auction -> {
                    statement.setDouble(14, auction.price());
                    statement.setNull(15, Types.DOUBLE);
                    statement.setNull(16, Types.DOUBLE);
                    statement.setNull(17, Types.INTEGER);
                }
                case HistoryEntry.Bid bid -> {
                    statement.setNull(14, Types.DOUBLE);
                    statement.setDouble(15, bid.startingPrice());
                    statement.setDouble(16, bid.finalPrice());
                    statement.setInt(17, bid.totalBidders());
                }
            }

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not append history entry for listing " + info.id(), e);
        }
    }

    @Override
    public Optional<HistoryEntry> find(String listingId, Instant completedAt) {
        String sql = "SELECT * FROM " + table + " WHERE listing_id = ? AND completed_at = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, listingId);
            statement.setLong(2, completedAt.toEpochMilli());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapEntry(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load history entry for listing " + listingId, e);
        }
    }

    @Override
    public void trim(UUID player, int maxEntries) {
        if (maxEntries <= 0) {
            return;
        }

        String sql = "DELETE FROM " + table
                + " WHERE entry_id IN ("
                + "  SELECT entry_id FROM ("
                + "    SELECT entry_id"
                + "    FROM " + table
                + "    WHERE seller = ? OR buyer = ?"
                + "    ORDER BY completed_at DESC"
                + "    LIMIT 1000000000 OFFSET ?"
                + "  ) AS overflow"
                + ")";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String uuid = player.toString();
            statement.setString(1, uuid);
            statement.setString(2, uuid);
            statement.setInt(3, maxEntries);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not trim history for player " + player, e);
        }
    }

    @Override
    public Map<String, Double> sumForPlayer(UUID player, TransactionKind kind, @Nullable Instant since) {
        String roleColumn = kind == TransactionKind.EARNED ? "seller" : "buyer";
        return sum(roleColumn + " = ?", List.of(player.toString()), kind == TransactionKind.EARNED, since);
    }

    @Override
    public Map<String, Double> sumGlobalVolume(@Nullable Instant since) {
        return sum(null, List.of(), false, since);
    }

    private Map<String, Double> sum(@Nullable String roleClause, List<Object> roleParams, boolean subtractTax, @Nullable Instant since) {
        String valueExpr = "COALESCE(price, final_price)" + (subtractTax ? " - tax" : "");

        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>(roleParams);
        if (roleClause != null) {
            conditions.add(roleClause);
        }
        if (since != null) {
            conditions.add("completed_at >= ?");
            params.add(since.toEpochMilli());
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        String sql = "SELECT economy, SUM(" + valueExpr + ") AS total FROM " + table + where + " GROUP BY economy";

        Map<String, Double> totals = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    totals.put(result.getString("economy"), result.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not sum history transactions", e);
        }
        return totals;
    }

    @Override
    public int resyncMetadata(ListingMetadataResolver resolver) {
        return SqlMetadataResync.run(dataSource, table, "entry_id", resolver);
    }

    private SqlFilter buildFilter(HistoryQuery query) {
        SqlFilter filter = new SqlFilter();
        if (query.player() != null) {
            String uuid = query.player().toString();
            switch (query.role()) {
                case SELLER -> filter.eq("seller", uuid);
                case BUYER -> filter.eq("buyer", uuid);
                case EITHER -> filter.eqEither("seller", "buyer", uuid);
            }
        }
        return filter
                .eq("listing_type", query.listingType() != null ? query.listingType().name() : null)
                .eq("category", query.category())
                .eq("economy", query.economy())
                .like("search_name", query.search());
    }

    // auctions only populate "price", bids only populate "final_price" - COALESCE picks
    // whichever one this row actually has
    private String sortColumn(ListingSort sort) {
        return switch (sort) {
            case NEWEST, OLDEST -> "completed_at";
            case PRICE_HIGH, PRICE_LOW -> "COALESCE(price, final_price)";
            case ALPHABETICAL -> "search_name";
            case AMOUNT -> "amount";
        };
    }

    private static String direction(ListingSort sort) {
        return sort.ascending() ? "ASC" : "DESC";
    }

    private HistoryEntry mapEntry(ResultSet rs) throws SQLException {
        HistoryEntry.Info info = mapInfo(rs);

        if ("AUCTION".equals(rs.getString("listing_type"))) {
            return new HistoryEntry.Auction(info, rs.getDouble("price"));
        }
        return new HistoryEntry.Bid(info, rs.getDouble("starting_price"), rs.getDouble("final_price"), rs.getInt("total_bidders"));
    }

    private HistoryEntry.Info mapInfo(ResultSet rs) throws SQLException {
        return new HistoryEntry.Info(
                rs.getString("listing_id"),
                UUID.fromString(rs.getString("seller")),
                UUID.fromString(rs.getString("buyer")),
                ItemSerializer.deserialize(rs.getBytes("item")),
                rs.getInt("amount"),
                rs.getString("economy"),
                rs.getDouble("fee"),
                rs.getDouble("tax"),
                rs.getString("category"),
                rs.getString("search_name"),
                Instant.ofEpochMilli(rs.getLong("created_at")),
                Instant.ofEpochMilli(rs.getLong("completed_at"))
        );
    }
}