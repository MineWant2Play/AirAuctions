package com.ftxeven.airauctions.database.id;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class SqlSequenceListingIdGenerator implements ListingIdGenerator {

    private final DataSource dataSource;
    private final String tableName;
    private final String sequenceName;

    public SqlSequenceListingIdGenerator(DataSource dataSource, String tableName, String sequenceName) {
        this.dataSource = dataSource;
        this.tableName = tableName;
        this.sequenceName = sequenceName;
        ensureSequenceRow();
    }

    @Override
    public String next() {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long value = incrementAndFetch(connection);
                connection.commit();
                return String.valueOf(value);
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not generate next id for sequence '" + sequenceName + "'", e);
        }
    }

    private long incrementAndFetch(Connection connection) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE " + tableName + " SET value = value + 1 WHERE name = ?")) {
            update.setString(1, sequenceName);
            update.executeUpdate();
        }
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT value FROM " + tableName + " WHERE name = ?")) {
            select.setString(1, sequenceName);
            try (ResultSet result = select.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private void ensureSequenceRow() {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement create = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS %s (
                        name VARCHAR(64) PRIMARY KEY,
                        value BIGINT NOT NULL
                    )
                    """.formatted(tableName))) {
                create.execute();
            }

            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT 1 FROM " + tableName + " WHERE name = ?")) {
                select.setString(1, sequenceName);
                try (ResultSet result = select.executeQuery()) {
                    if (result.next()) {
                        return;
                    }
                }
            }

            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO " + tableName + " (name, value) VALUES (?, 0)")) {
                insert.setString(1, sequenceName);
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            // benign - another thread created the row between our check and insert
        }
    }
}