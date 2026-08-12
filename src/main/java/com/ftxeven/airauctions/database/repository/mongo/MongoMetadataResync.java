package com.ftxeven.airauctions.database.repository.mongo;

import com.ftxeven.airauctions.database.repository.ListingMetadataResolver;
import com.ftxeven.airauctions.util.ItemSerializer;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import org.bson.Document;
import org.bson.types.Binary;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

final class MongoMetadataResync {

    private static final int BATCH_SIZE = 500;

    private MongoMetadataResync() {
    }

    static int run(MongoCollection<Document> collection, ListingMetadataResolver resolver) {
        List<WriteModel<Document>> batch = new ArrayList<>(BATCH_SIZE);
        int updated = 0;

        for (Document doc : collection.find()) {
            Binary itemBytes = doc.get("item", Binary.class);
            ListingMetadataResolver.Metadata metadata = resolver.resolve(ItemSerializer.deserialize(itemBytes.getData()));

            if (metadata.category().equals(doc.getString("category")) && metadata.searchName().equals(doc.getString("searchName"))) {
                continue;
            }

            batch.add(new UpdateOneModel<>(
                    eq("_id", doc.get("_id")),
                    Updates.combine(Updates.set("category", metadata.category()), Updates.set("searchName", metadata.searchName()))));
            updated++;

            if (batch.size() == BATCH_SIZE) {
                collection.bulkWrite(batch);
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            collection.bulkWrite(batch);
        }
        return updated;
    }
}