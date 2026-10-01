package com.jiuliu.myblog_dev.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TtlStoreTest {

    @Test
    void setGetDeleteRoundTrip() {
        TtlStore store = new TtlStore();
        store.set("k", "v", 60);
        assertEquals("v", store.get("k"));

        store.delete("k");
        assertNull(store.get("k"));
        store.destroy();
    }

    @Test
    void expiredEntryIsNotReturned() throws InterruptedException {
        TtlStore store = new TtlStore();
        store.set("k", "v", 1);
        Thread.sleep(1100);
        assertNull(store.get("k"));
        store.destroy();
    }
}
