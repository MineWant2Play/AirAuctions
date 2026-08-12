package com.ftxeven.airauctions.database.repository.mongo;

import com.ftxeven.airauctions.database.repository.PlayerRepository;
import com.ftxeven.airauctions.model.PlayerData;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.Document;

import java.util.*;

import static com.mongodb.client.model.Filters.eq;

public final class MongoPlayerRepository implements PlayerRepository {

    private static final Collation CASE_INSENSITIVE = Collation.builder()
            .locale("en")
            .collationStrength(CollationStrength.SECONDARY)
            .build();

    private final MongoCollection<Document> players;

    public MongoPlayerRepository(MongoDatabase database, String tablePrefix) {
        this.players = database.getCollection(tablePrefix + "players");
    }

    @Override
    public Optional<PlayerData> find(UUID uuid) {
        Document doc = players.find(eq("_id", uuid.toString())).first();
        return doc != null ? Optional.of(mapPlayer(doc)) : Optional.empty();
    }

    @Override
    public Optional<PlayerData> findByName(String name) {
        Document doc = players.find(eq("name", name)).collation(CASE_INSENSITIVE).first();
        return doc != null ? Optional.of(mapPlayer(doc)) : Optional.empty();
    }

    @Override
    public Map<UUID, PlayerData> findAll(Collection<UUID> uuids) {
        if (uuids.isEmpty()) {
            return Map.of();
        }
        List<String> ids = uuids.stream().map(UUID::toString).toList();

        Map<UUID, PlayerData> result = new LinkedHashMap<>();
        for (Document doc : players.find(Filters.in("_id", ids))) {
            PlayerData data = mapPlayer(doc);
            result.put(data.uuid(), data);
        }
        return result;
    }

    @Override
    public void upsert(UUID uuid, String name, PlayerData.Skin skin) {
        Document setFields = new Document("name", name);
        Document setOnInsertFields = new Document()
                .append("extraSlots", 0)
                .append("pendingEarnings", new Document())
                .append("pendingRefunds", new Document());

        if (skin.isPresent()) {
            setFields.append("skin", skin.value()).append("skinSignature", skin.signature());
        } else {
            setOnInsertFields.append("skin", "").append("skinSignature", "");
        }

        Document update = new Document("$set", setFields).append("$setOnInsert", setOnInsertFields);
        players.updateOne(eq("_id", uuid.toString()), update, new UpdateOptions().upsert(true));
    }

    @Override
    public int adjustExtraSlots(UUID uuid, int delta) {
        Document result = players.findOneAndUpdate(
                eq("_id", uuid.toString()),
                Updates.inc("extraSlots", delta),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
        return result.getInteger("extraSlots");
    }

    @Override
    public void addPendingEarnings(UUID uuid, String economy, double amount) {
        addPending(uuid, "pendingEarnings", economy, amount);
    }

    @Override
    public void clearPendingEarnings(UUID uuid) {
        clearPending(uuid, "pendingEarnings");
    }

    @Override
    public void addPendingRefunds(UUID uuid, String economy, double amount) {
        addPending(uuid, "pendingRefunds", economy, amount);
    }

    @Override
    public void clearPendingRefunds(UUID uuid) {
        clearPending(uuid, "pendingRefunds");
    }

    private void addPending(UUID uuid, String rootField, String economy, double amount) {
        String field = rootField + "." + economy.replace(".", "_");
        players.updateOne(eq("_id", uuid.toString()), Updates.inc(field, amount));
    }

    private void clearPending(UUID uuid, String rootField) {
        players.updateOne(eq("_id", uuid.toString()), Updates.set(rootField, new Document()));
    }

    private PlayerData mapPlayer(Document doc) {
        PlayerData.Skin skin = new PlayerData.Skin(
                doc.getString("skin") != null ? doc.getString("skin") : "",
                doc.getString("skinSignature") != null ? doc.getString("skinSignature") : "");

        return new PlayerData(
                UUID.fromString(doc.getString("_id")),
                doc.getString("name"),
                skin,
                doc.getInteger("extraSlots"),
                mapCurrencyMap(doc, "pendingEarnings"),
                mapCurrencyMap(doc, "pendingRefunds")
        );
    }

    private Map<String, Double> mapCurrencyMap(Document doc, String field) {
        Document raw = doc.get(field, new Document());
        Map<String, Double> values = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            values.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
        }
        return values;
    }
}