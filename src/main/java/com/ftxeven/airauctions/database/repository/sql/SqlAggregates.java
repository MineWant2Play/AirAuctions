package com.ftxeven.airauctions.database.repository.sql;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

final class SqlAggregates {

    private SqlAggregates() {
    }

    static long countRows(DataSource dataSource, String table, SqlFilter filter) {
        String sql = "SELECT COUNT(*) FROM " + table + filter.whereClause();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            filter.bind(statement);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not count rows in " + table, e);
        }
    }

    static Map<String, Long> countBy(DataSource dataSource, String table, String groupColumn, SqlFilter filter) {
        String sql = "SELECT " + groupColumn + ", COUNT(*) FROM " + table + filter.whereClause() + " GROUP BY " + groupColumn;
        Map<String, Long> counts = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            filter.bind(statement);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    counts.put(result.getString(1), result.getLong(2));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not compute facet counts for " + groupColumn, e);
        }
        return counts;
    }
}