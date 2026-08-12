package com.ftxeven.airauctions.database.cache.sync;

import java.util.function.Consumer;

public interface CacheSync {

    default void publish(String channel, String key) {
    }

    default void subscribe(String channel, Consumer<String> onInvalidated) {
    }

    default void close() {
    }

    static CacheSync disabled() {
        return new CacheSync() {};
    }
}