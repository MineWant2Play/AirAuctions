package com.ftxeven.airauctions.database;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.config.StorageConfig;
import com.ftxeven.airauctions.database.id.HexListingIdGenerator;
import com.ftxeven.airauctions.database.id.ListingIdGenerator;
import com.ftxeven.airauctions.database.id.MongoSequenceListingIdGenerator;
import com.ftxeven.airauctions.database.id.SqlSequenceListingIdGenerator;
import com.ftxeven.airauctions.database.migration.MigrationLoader;
import com.ftxeven.airauctions.database.migration.MigrationRunner;
import com.ftxeven.airauctions.database.migration.MongoMigrationRunner;
import com.ftxeven.airauctions.database.migration.SqlMigrationRunner;
import com.ftxeven.airauctions.database.repository.HistoryRepository;
import com.ftxeven.airauctions.database.repository.ListingRepository;
import com.ftxeven.airauctions.database.repository.PlayerRepository;
import com.ftxeven.airauctions.database.repository.mongo.MongoHistoryRepository;
import com.ftxeven.airauctions.database.repository.mongo.MongoListingRepository;
import com.ftxeven.airauctions.database.repository.mongo.MongoPlayerRepository;
import com.ftxeven.airauctions.database.repository.sql.SqlHistoryRepository;
import com.ftxeven.airauctions.database.repository.sql.SqlListingRepository;
import com.ftxeven.airauctions.database.repository.sql.SqlPlayerRepository;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class DatabaseManager {

    private final JavaPlugin plugin;
    private final ConfigManager configs;

    private HikariDataSource dataSource;
    private MongoClient mongoClient;

    private ListingRepository listings;
    private HistoryRepository history;
    private PlayerRepository players;

    public DatabaseManager(JavaPlugin plugin, ConfigManager configs) {
        this.plugin = plugin;
        this.configs = configs;
    }

    public boolean connect() {
        try {
            StorageConfig.Database db = configs.storage().database();
            String prefix = db.tablePrefix();
            ListingIdGenerator listingIdGenerator;

            if (db.driver() == StorageConfig.Driver.MONGODB) {
                MongoDatabase database = connectMongo(db.mongodb());
                runMigrations(new MongoMigrationRunner(plugin, database, prefix), StorageConfig.Driver.MONGODB);

                listingIdGenerator = db.listingIdFormat() == StorageConfig.ListingIdFormat.UUID
                        ? new HexListingIdGenerator(db.uuidLength())
                        : new MongoSequenceListingIdGenerator(database, prefix + "counters", "listings");

                listings = new MongoListingRepository(database, listingIdGenerator, prefix);
                history = new MongoHistoryRepository(database, prefix);
                players = new MongoPlayerRepository(database, prefix);
            } else {
                connectSql(db);
                runMigrations(new SqlMigrationRunner(plugin, dataSource, prefix), db.driver());

                listingIdGenerator = db.listingIdFormat() == StorageConfig.ListingIdFormat.UUID
                        ? new HexListingIdGenerator(db.uuidLength())
                        : new SqlSequenceListingIdGenerator(dataSource, prefix + "id_sequences", "listings");

                listings = new SqlListingRepository(dataSource, listingIdGenerator, prefix);
                history = new SqlHistoryRepository(dataSource, prefix);
                players = new SqlPlayerRepository(dataSource, prefix);
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to the database: " + e.getMessage());
            close();
            return false;
        }
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    public ListingRepository listings() {
        return listings;
    }

    public HistoryRepository history() {
        return history;
    }

    public PlayerRepository players() {
        return players;
    }

    private void runMigrations(MigrationRunner runner, StorageConfig.Driver driver) throws Exception {
        List<MigrationRunner.Migration> migrations = new MigrationLoader(plugin).load(MigrationLoader.folderFor(driver));
        runner.migrate(migrations);
    }

    // SQL wiring

    private void connectSql(StorageConfig.Database db) {
        HikariConfig hikari = new HikariConfig();

        if (db.driver() == StorageConfig.Driver.SQLITE) {
            File file = new File(plugin.getDataFolder(), db.sqlite().file());
            file.getParentFile().mkdirs();
            hikari.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            hikari.setMaximumPoolSize(1); // sqlite only really supports one writer at a time
            hikari.setConnectionInitSql("PRAGMA foreign_keys = ON");
        } else {
            StorageConfig.Mysql mysql = db.mysql();
            hikari.setJdbcUrl("jdbc:mysql://" + mysql.host() + ":" + mysql.port() + "/" + mysql.database()
                    + "?useSSL=" + mysql.ssl() + "&autoReconnect=" + mysql.autoReconnect());
            hikari.setUsername(mysql.user());
            hikari.setPassword(mysql.password());
            hikari.setMaximumPoolSize(mysql.pool().size());
            hikari.setPoolName(mysql.pool().name());
            hikari.setConnectionTimeout(mysql.pool().connectionTimeout());
            hikari.setIdleTimeout(mysql.pool().idleTimeout());
            hikari.setMaxLifetime(mysql.pool().maxLifetime());
        }

        dataSource = new HikariDataSource(hikari);
    }

    // MongoDB wiring

    private MongoDatabase connectMongo(StorageConfig.Mongodb mongo) {
        MongoClientSettings.Builder settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongo.uri().isBlank() ? plainConnectionString(mongo) : mongo.uri()));

        settings.applyToConnectionPoolSettings(builder -> builder
                .minSize(mongo.pool().minSize())
                .maxSize(mongo.pool().maxSize())
                .maxConnectionIdleTime(mongo.pool().maxIdleTime(), TimeUnit.MILLISECONDS)
                .maxConnectionLifeTime(mongo.pool().maxLifeTime(), TimeUnit.MILLISECONDS));
        settings.applyToClusterSettings(builder -> builder
                .serverSelectionTimeout(mongo.pool().serverSelectionTimeout(), TimeUnit.MILLISECONDS));
        settings.applyToSocketSettings(builder -> builder
                .connectTimeout(mongo.pool().connectTimeout(), TimeUnit.MILLISECONDS));

        mongoClient = MongoClients.create(settings.build());
        return mongoClient.getDatabase(mongo.database());
    }

    private String plainConnectionString(StorageConfig.Mongodb mongo) {
        String credentials = mongo.user().isBlank() ? "" : mongo.user() + ":" + encode(mongo.password()) + "@";
        return "mongodb://" + credentials + mongo.host() + ":" + mongo.port() + "/" + mongo.database()
                + "?authSource=" + mongo.authSource() + "&ssl=" + mongo.ssl();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}