package com.ftxeven.airauctions.database.repository.sql;

import com.ftxeven.airauctions.database.repository.PlayerRepository;
import com.ftxeven.airauctions.model.PlayerData;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public final class SqlPlayerRepository implements PlayerRepository {

    private final DataSource dataSource;
    private final String playersTable;
    private final String earningsTable;
    private final String refundsTable;

    public SqlPlayerRepository(DataSource dataSource, String tablePrefix) {
        this.dataSource = dataSource;
        this.playersTable = tablePrefix + "players";
        this.earningsTable = tablePrefix + "player_earnings";
        this.refundsTable = tablePrefix + "player_refunds";
    }

    @Override
    public Optional<PlayerData> find(UUID uuid) {
        String sql = "SELECT * FROM " + playersTable + " WHERE uuid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapPlayer(connection, result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load player " + uuid, e);
        }
    }

    @Override
    public Optional<PlayerData> findByName(String name) {
        String sql = "SELECT * FROM " + playersTable + " WHERE name = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapPlayer(connection, result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load player by name " + name, e);
        }
    }

    @Override
    public Map<UUID, PlayerData> findAll(Collection<UUID> uuids) {
        if (uuids.isEmpty()) {
            return Map.of();
        }

        String placeholders = uuids.stream().map(u -> "?").collect(Collectors.joining(", "));
        String earningsSql = "SELECT uuid, economy, amount FROM " + earningsTable + " WHERE uuid IN (" + placeholders + ")";
        String refundsSql = "SELECT uuid, economy, amount FROM " + refundsTable + " WHERE uuid IN (" + placeholders + ")";
        String playersSql = "SELECT * FROM " + playersTable + " WHERE uuid IN (" + placeholders + ")";

        Map<UUID, PlayerData> result = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            Map<UUID, Map<String, Double>> earningsByPlayer = new LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement(earningsSql)) {
                bindUuids(statement, uuids);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        earningsByPlayer.computeIfAbsent(uuid, k -> new LinkedHashMap<>())
                                .put(rs.getString("economy"), rs.getDouble("amount"));
                    }
                }
            }

            Map<UUID, Map<String, Double>> refundsByPlayer = new LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement(refundsSql)) {
                bindUuids(statement, uuids);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        refundsByPlayer.computeIfAbsent(uuid, k -> new LinkedHashMap<>())
                                .put(rs.getString("economy"), rs.getDouble("amount"));
                    }
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(playersSql)) {
                bindUuids(statement, uuids);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        result.put(uuid, new PlayerData(
                                uuid,
                                rs.getString("name"),
                                new PlayerData.Skin(rs.getString("skin"), rs.getString("skin_signature")),
                                rs.getInt("extra_slots"),
                                earningsByPlayer.getOrDefault(uuid, Map.of()),
                                refundsByPlayer.getOrDefault(uuid, Map.of())));
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not batch-load players", e);
        }
        return result;
    }

    private void bindUuids(PreparedStatement statement, Collection<UUID> uuids) throws SQLException {
        int i = 1;
        for (UUID uuid : uuids) {
            statement.setString(i++, uuid.toString());
        }
    }

    @Override
    public void upsert(UUID uuid, String name, PlayerData.Skin skin) {
        try (Connection connection = dataSource.getConnection()) {
            String updateSql = skin.isPresent()
                    ? "UPDATE " + playersTable + " SET name = ?, skin = ?, skin_signature = ? WHERE uuid = ?"
                    : "UPDATE " + playersTable + " SET name = ? WHERE uuid = ?";

            try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                update.setString(1, name);
                if (skin.isPresent()) {
                    update.setString(2, skin.value());
                    update.setString(3, skin.signature());
                    update.setString(4, uuid.toString());
                } else {
                    update.setString(2, uuid.toString());
                }
                if (update.executeUpdate() > 0) {
                    return;
                }
            }

            String insertSql = "INSERT INTO " + playersTable
                    + " (uuid, name, skin, skin_signature, extra_slots) VALUES (?, ?, ?, ?, 0)";
            try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                insert.setString(1, uuid.toString());
                insert.setString(2, name);
                insert.setString(3, skin.isPresent() ? skin.value() : "");
                insert.setString(4, skin.isPresent() ? skin.signature() : "");
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not upsert player " + uuid, e);
        }
    }

    @Override
    public int adjustExtraSlots(UUID uuid, int delta) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE " + playersTable + " SET extra_slots = extra_slots + ? WHERE uuid = ?")) {
                update.setInt(1, delta);
                update.setString(2, uuid.toString());
                update.executeUpdate();
            }
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT extra_slots FROM " + playersTable + " WHERE uuid = ?")) {
                select.setString(1, uuid.toString());
                try (ResultSet result = select.executeQuery()) {
                    result.next();
                    return result.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not adjust extra slots for player " + uuid, e);
        }
    }

    @Override
    public void addPendingEarnings(UUID uuid, String economy, double amount) {
        addPending(earningsTable, uuid, economy, amount);
    }

    @Override
    public void clearPendingEarnings(UUID uuid) {
        clearPending(earningsTable, uuid);
    }

    @Override
    public void addPendingRefunds(UUID uuid, String economy, double amount) {
        addPending(refundsTable, uuid, economy, amount);
    }

    @Override
    public void clearPendingRefunds(UUID uuid) {
        clearPending(refundsTable, uuid);
    }

    private void addPending(String table, UUID uuid, String economy, double amount) {
        String updateSql = "UPDATE " + table + " SET amount = amount + ? WHERE uuid = ? AND economy = ?";
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                update.setDouble(1, amount);
                update.setString(2, uuid.toString());
                update.setString(3, economy);
                if (update.executeUpdate() > 0) {
                    return;
                }
            }

            String insertSql = "INSERT INTO " + table + " (uuid, economy, amount) VALUES (?, ?, ?)";
            try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                insert.setString(1, uuid.toString());
                insert.setString(2, economy);
                insert.setDouble(3, amount);
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not add pending amount for player " + uuid + " in " + table, e);
        }
    }

    private void clearPending(String table, UUID uuid) {
        String sql = "DELETE FROM " + table + " WHERE uuid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not clear pending amounts for player " + uuid + " in " + table, e);
        }
    }

    private PlayerData mapPlayer(Connection connection, ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        return new PlayerData(
                uuid,
                rs.getString("name"),
                new PlayerData.Skin(rs.getString("skin"), rs.getString("skin_signature")),
                rs.getInt("extra_slots"),
                loadPending(connection, earningsTable, uuid),
                loadPending(connection, refundsTable, uuid)
        );
    }

    private Map<String, Double> loadPending(Connection connection, String table, UUID uuid) throws SQLException {
        Map<String, Double> pending = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT economy, amount FROM " + table + " WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    pending.put(result.getString("economy"), result.getDouble("amount"));
                }
            }
        }
        return pending;
    }
}