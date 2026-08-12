package com.ftxeven.airauctions.database.repository.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MongoAggregates {

    private MongoAggregates() {
    }

    static long countDocuments(MongoCollection<Document> collection, Bson filter) {
        return collection.countDocuments(filter);
    }

    static Map<String, Long> countBy(MongoCollection<Document> collection, String groupField, Bson filter) {
        Map<String, Long> counts = new LinkedHashMap<>();
        List<Bson> pipeline = List.of(
                Aggregates.match(filter),
                Aggregates.group("$" + groupField, Accumulators.sum("count", 1)));

        for (Document doc : collection.aggregate(pipeline)) {
            Object key = doc.get("_id");
            if (key != null) {
                counts.put(key.toString(), ((Number) doc.get("count")).longValue());
            }
        }
        return counts;
    }
}