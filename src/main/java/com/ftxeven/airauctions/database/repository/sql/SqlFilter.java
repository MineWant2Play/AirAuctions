package com.ftxeven.airauctions.database.repository.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

// accumulates "column = ?" style conditions for a dynamic WHERE clause
// Any null/blank value is silently skipped
final class SqlFilter {

    private final List<String> conditions = new ArrayList<>();
    private final List<Object> params = new ArrayList<>();

    SqlFilter eq(String column, Object value) {
        if (value != null) {
            conditions.add(column + " = ?");
            params.add(value);
        }
        return this;
    }

    SqlFilter eqEither(String columnA, String columnB, Object value) {
        if (value != null) {
            conditions.add("(" + columnA + " = ? OR " + columnB + " = ?)");
            params.add(value);
            params.add(value);
        }
        return this;
    }

    SqlFilter like(String column, String value) {
        if (value != null && !value.isBlank()) {
            conditions.add(column + " LIKE ? ESCAPE '\\'");
            params.add("%" + escapeLike(value) + "%");
        }
        return this;
    }

    SqlFilter in(String column, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return this;
        }
        String placeholders = values.stream().map(v -> "?").collect(Collectors.joining(", "));
        conditions.add(column + " IN (" + placeholders + ")");
        params.addAll(values);
        return this;
    }

    SqlFilter gt(String column, Object value) {
        if (value != null) {
            conditions.add(column + " > ?");
            params.add(value);
        }
        return this;
    }

    String whereClause() {
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    List<Object> params() {
        return params;
    }

    void bind(PreparedStatement statement) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}