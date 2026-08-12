package com.ftxeven.airauctions.database.id;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.inc;

// Mongo has no native AUTO_INCREMENT, so it's emulated with a single counter document,
// ex: { _id: "listings", value: 1042 }
public final class MongoSequenceListingIdGenerator implements ListingIdGenerator {

    private final MongoCollection<Document> counters;
    private final String sequenceName;

    public MongoSequenceListingIdGenerator(MongoDatabase database, String collectionName, String sequenceName) {
        this.counters = database.getCollection(collectionName);
        this.sequenceName = sequenceName;
    }

    @Override
    public String next() {
        Document result = counters.findOneAndUpdate(
                eq("_id", sequenceName),
                inc("value", 1),
                new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
        return String.valueOf(result.getInteger("value"));
    }
}