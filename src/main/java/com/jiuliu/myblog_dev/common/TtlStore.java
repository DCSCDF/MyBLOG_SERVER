package com.jiuliu.myblog_dev.common;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory key/value store with per-key TTL, used as a generic scratch store (temp login tokens,
 * OAuth codes, pending registration / password-reset / email-change records).
 * Values are opaque strings; callers keep their existing serialization.
 */
@Component
public class TtlStore {

    private record Entry(String value, long expireAtMillis) {
    }

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler;

    public TtlStore() {
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ttl-store-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupScheduler.scheduleAtFixedRate(this::purgeExpired, 10, 10, TimeUnit.MINUTES);
    }

    public void set(String key, String value, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            store.remove(key);
            return;
        }
        store.put(key, new Entry(value, System.currentTimeMillis() + ttlSeconds * 1000L));
    }

    public String get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expireAtMillis()) {
            store.remove(key, entry);
            return null;
        }
        return entry.value();
    }

    public void delete(String key) {
        store.remove(key);
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> now > e.getValue().expireAtMillis());
    }

    @PreDestroy
    public void destroy() {
        cleanupScheduler.shutdownNow();
    }
}
