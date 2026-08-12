package com.ftxeven.airauctions.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public final class StorageConfig extends BaseConfig {

    private volatile String serverId;
    private volatile Database database;
    private volatile Redis redis;

    public StorageConfig(JavaPlugin plugin) {
        super(plugin, "storage.yml");
    }

    @Override
    protected void read(ConfigurationSection yaml) {
        String newServerId = getString(yaml, "server-id", "");
        Database newDatabase = readDatabase(yaml.getConfigurationSection("database"));
        Redis newRedis = readRedis(yaml.getConfigurationSection("redis"));

        serverId = newServerId;
        database = newDatabase;
        redis = newRedis;
    }

    public String serverId() {
        return serverId;
    }

    public Database database() {
        return database;
    }

    public Redis redis() {
        return redis;
    }

    // Section readers

    private Database readDatabase(ConfigurationSection sec) {
        sec = orEmpty(sec);

        int uuidLength = getInt(sec, "uuid-length", 8);
        if (uuidLength < 4 || uuidLength > 32) {
            plugin.getLogger().warning("uuid-length must be between 4 and 32 in " + fileName() + ", using 8");
            uuidLength = 8;
        }

        return new Database(
                enumOr(sec, "driver", Driver.class, Driver.SQLITE),
                enumOr(sec, "listing-id-format", ListingIdFormat.class, ListingIdFormat.AUTO_INCREMENT),
                uuidLength,
                getString(sec, "table-prefix", "aa_"),
                readSqlite(sec.getConfigurationSection("sqlite")),
                readMysql(sec.getConfigurationSection("mysql")),
                readMongodb(sec.getConfigurationSection("mongodb"))
        );
    }

    private Sqlite readSqlite(ConfigurationSection sec) {
        sec = orEmpty(sec);
        return new Sqlite(getString(sec, "file", "data/database.db"));
    }

    private Mysql readMysql(ConfigurationSection sec) {
        sec = orEmpty(sec);
        return new Mysql(
                getString(sec, "host", "localhost"),
                getInt(sec, "port", 3306),
                getString(sec, "database", "airauctions"),
                getString(sec, "user", "root"),
                getString(sec, "password", ""),
                getBoolean(sec, "ssl", false),
                getBoolean(sec, "auto-reconnect", true),
                readMysqlPool(sec.getConfigurationSection("pool"))
        );
    }

    private MysqlPool readMysqlPool(ConfigurationSection sec) {
        sec = orEmpty(sec);
        return new MysqlPool(
                getInt(sec, "size", 5),
                getString(sec, "name", "AirAuctions-Pool"),
                getInt(sec, "connection-timeout", 30000),
                getInt(sec, "idle-timeout", 600000),
                getInt(sec, "max-lifetime", 1800000)
        );
    }

    private Mongodb readMongodb(ConfigurationSection sec) {
        sec = orEmpty(sec);
        return new Mongodb(
                getString(sec, "uri", ""),
                getString(sec, "host", "localhost"),
                getInt(sec, "port", 27017),
                getString(sec, "database", "airauctions"),
                getString(sec, "user", ""),
                getString(sec, "password", ""),
                getString(sec, "auth-source", "admin"),
                getBoolean(sec, "ssl", false),
                readMongodbPool(sec.getConfigurationSection("pool"))
        );
    }

    private MongodbPool readMongodbPool(ConfigurationSection sec) {
        sec = orEmpty(sec);
        return new MongodbPool(
                getInt(sec, "min-size", 0),
                getInt(sec, "max-size", 5),
                getInt(sec, "connect-timeout", 10000),
                getInt(sec, "server-selection-timeout", 5000),
                getInt(sec, "idle-timeout", 600000),
                getInt(sec, "max-lifetime", 1800000)
        );
    }

    private Redis readRedis(ConfigurationSection sec) {
        sec = orEmpty(sec);
        return new Redis(
                getBoolean(sec, "enabled", false),
                getString(sec, "host", "localhost"),
                getInt(sec, "port", 6379),
                getString(sec, "password", ""),
                getInt(sec, "database", 0),
                getInt(sec, "timeout", 3000),
                getInt(sec, "pool-size", 4),
                getInt(sec, "resync-interval", 60)
        );
    }

    // Section types

    public enum Driver { SQLITE, MYSQL, MARIADB, MONGODB }

    public enum ListingIdFormat { AUTO_INCREMENT, UUID }

    public record Sqlite(String file) {}

    public record MysqlPool(int size, String name, int connectionTimeout, int idleTimeout, int maxLifetime) {}

    public record Mysql(
            String host,
            int port,
            String database,
            String user,
            String password,
            boolean ssl,
            boolean autoReconnect,
            MysqlPool pool
    ) {}

    public record MongodbPool(
            int minSize,
            int maxSize,
            int connectTimeout,
            int serverSelectionTimeout,
            int maxIdleTime,
            int maxLifeTime
    ) {}

    public record Mongodb(
            String uri,
            String host,
            int port,
            String database,
            String user,
            String password,
            String authSource,
            boolean ssl,
            MongodbPool pool
    ) {}

    public record Database(
            Driver driver,
            ListingIdFormat listingIdFormat,
            int uuidLength,
            String tablePrefix,
            Sqlite sqlite,
            Mysql mysql,
            Mongodb mongodb
    ) {}

    public record Redis(
            boolean enabled,
            String host,
            int port,
            String password,
            int database,
            int timeout,
            int poolSize,
            int resyncInterval
    ) {}
}