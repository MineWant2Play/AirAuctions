package com.ftxeven.airauctions.database.cache.sync;

import com.ftxeven.airauctions.config.StorageConfig;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class RedisCacheSync implements CacheSync {

    private final JavaPlugin plugin;
    private final JedisPool pool;
    private final HostAndPort address;
    private final JedisClientConfig clientConfig;
    private final String serverId;
    private final List<JedisPubSub> subscriptions = new CopyOnWriteArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public RedisCacheSync(JavaPlugin plugin, JedisPool pool, StorageConfig.Redis config, String serverId) {
        this.plugin = plugin;
        this.pool = pool;
        this.serverId = serverId;
        this.address = new HostAndPort(config.host(), config.port());
        this.clientConfig = DefaultJedisClientConfig.builder()
                .password(config.password().isBlank() ? null : config.password())
                .database(config.database())
                .timeoutMillis(config.timeout())
                .build();
    }

    @Override
    public void publish(String channel, String key) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish(topic(channel), serverId + ':' + key);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not publish cache invalidation on " + channel + ": " + e.getMessage());
        }
    }

    @Override
    public void subscribe(String channel, Consumer<String> onInvalidated) {
        JedisPubSub pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String subscribedChannel, String message) {
                int separator = message.lastIndexOf(':');
                if (separator < 0) {
                    return;
                }
                String sourceServer = message.substring(0, separator);
                if (!sourceServer.equals(serverId)) {
                    onInvalidated.accept(message.substring(separator + 1));
                }
            }
        };
        subscriptions.add(pubSub);

        Thread.ofVirtual().name("airauctions-redis-" + channel).start(() -> runWithReconnect(channel, pubSub));
    }

    private void runWithReconnect(String channel, JedisPubSub pubSub) {
        long backoff = 1000L;
        while (!closed.get()) {
            try (Jedis jedis = new Jedis(address, clientConfig)) {
                backoff = 1000L; // reset once a connection actually succeeds
                jedis.subscribe(pubSub, topic(channel));
            } catch (Exception e) {
                if (closed.get()) {
                    return;
                }
                plugin.getLogger().warning("Redis subscription on " + channel + " dropped, retrying in "
                        + (backoff / 1000) + "s: " + e.getMessage());
            }

            if (closed.get()) {
                return;
            }
            sleep(backoff);
            backoff = Math.min(backoff * 2, 30_000L);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        closed.set(true);
        for (JedisPubSub pubSub : subscriptions) {
            try {
                pubSub.unsubscribe();
            } catch (Exception ignored) {
                // already disconnected or mid-reconnect
            }
        }
        pool.close();
    }

    private static String topic(String channel) {
        return "airauctions:" + channel;
    }
}