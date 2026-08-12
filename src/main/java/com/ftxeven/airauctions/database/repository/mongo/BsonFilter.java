package com.ftxeven.airauctions.database.repository.mongo;

import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

final class BsonFilter {

    private final List<Bson> conditions = new ArrayList<>();

    BsonFilter eq(String field, Object value) {
        if (value != null) {
            conditions.add(Filters.eq(field, value));
        }
        return this;
    }

    BsonFilter eqEither(String fieldA, String fieldB, Object value) {
        if (value != null) {
            conditions.add(Filters.or(Filters.eq(fieldA, value), Filters.eq(fieldB, value)));
        }
        return this;
    }

    BsonFilter like(String field, String value) {
        if (value != null && !value.isBlank()) {
            conditions.add(Filters.regex(field, Pattern.quote(value), "i"));
        }
        return this;
    }

    BsonFilter in(String field, Collection<String> values) {
        if (values != null && !values.isEmpty()) {
            conditions.add(Filters.in(field, values));
        }
        return this;
    }

    BsonFilter gt(String field, Object value) {
        if (value != null) {
            conditions.add(Filters.gt(field, value));
        }
        return this;
    }

    Bson build() {
        return conditions.isEmpty() ? new Document() : Filters.and(conditions);
    }
}