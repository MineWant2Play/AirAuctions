package com.ftxeven.airauctions.database.repository.sql;

import com.ftxeven.airauctions.database.repository.ListingMetadataResolver;
import com.ftxeven.airauctions.util.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

final class SqlMetadataResync {

    private static final int BATCH_SIZE = 500;

    private SqlMetadataResync() {
    }

    static int run(DataSource dataSource, String table, String idColumn, ListingMetadataResolver resolver) {
        String selectSql = "SELECT " + idColumn + ", item, category, search_name FROM " + table;
        String updateSql = "UPDATE " + table + " SET category = ?, search_name = ? WHERE " + idColumn + " = ?";

        int updated = 0;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement select = connection.prepareStatement(selectSql);
                 ResultSet result = select.executeQuery();
                 PreparedStatement update = connection.prepareStatement(updateSql)) {

                int batched = 0;
                while (result.next()) {
                    ItemStack item = ItemSerializer.deserialize(result.getBytes("item"));
                    ListingMetadataResolver.Metadata metadata = resolver.resolve(item);

                    if (metadata.category().equals(result.getString("category"))
                            && metadata.searchName().equals(result.getString("search_name"))) {
                        continue;
                    }

                    update.setString(1, metadata.category());
                    update.setString(2, metadata.searchName());
                    update.setObject(3, result.getObject(idColumn));
                    update.addBatch();
                    batched++;
                    updated++;

                    if (batched == BATCH_SIZE) {
                        update.executeBatch();
                        batched = 0;
                    }
                }
                if (batched > 0) {
                    update.executeBatch();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not resync metadata for " + table, e);
        }
        return updated;
    }
}