package io.reqtracer.storage;

import io.reqtracer.core.FixedClock;
import io.reqtracer.core.Trace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTraceStoreTest {

    private InMemoryTraceStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryTraceStore();
    }

    @Test
    void testStoreAndRetrieve() {
        FixedClock clock = new FixedClock(1000);
        Trace trace = new Trace("req-123", clock);

        store.store(trace);

        Optional<Trace> retrieved = store.get("req-123");
        assertTrue(retrieved.isPresent());
        assertEquals("req-123", retrieved.get().getTraceId());
    }

    @Test
    void testGetNonExistent() {
        Optional<Trace> result = store.get("nonexistent");
        assertFalse(result.isPresent());
    }

    @Test
    void testRemove() {
        FixedClock clock = new FixedClock(1000);
        Trace trace = new Trace("req-123", clock);

        store.store(trace);
        assertTrue(store.get("req-123").isPresent());

        store.remove("req-123");
        assertFalse(store.get("req-123").isPresent());
    }

    @Test
    void testListTraceIds() {
        FixedClock clock = new FixedClock(1000);

        store.store(new Trace("req-1", clock));
        store.store(new Trace("req-2", clock));
        store.store(new Trace("req-3", clock));

        Collection<String> ids = store.listTraceIds();
        assertEquals(3, ids.size());
        assertTrue(ids.contains("req-1"));
        assertTrue(ids.contains("req-2"));
        assertTrue(ids.contains("req-3"));
    }

    @Test
    void testClear() {
        FixedClock clock = new FixedClock(1000);

        store.store(new Trace("req-1", clock));
        store.store(new Trace("req-2", clock));

        assertEquals(2, store.size());

        store.clear();
        assertEquals(0, store.size());
    }

    @Test
    void testStoreReplaces() {
        FixedClock clock = new FixedClock(1000);
        Trace trace1 = new Trace("req-123", clock);
        trace1.mark("EVENT_1");

        Trace trace2 = new Trace("req-123", clock);
        trace2.mark("EVENT_2");

        store.store(trace1);
        store.store(trace2);

        // Should have only one trace with req-123
        assertEquals(1, store.size());

        Trace retrieved = store.get("req-123").get();
        assertEquals(1, retrieved.getEventCount());
        assertEquals("EVENT_2", retrieved.getEvents().get(0).getName());
    }

    @Test
    void testThreadSafety_ConcurrentWrites() throws InterruptedException {
        int threadCount = 10;
        int tracesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    FixedClock clock = new FixedClock(1000);
                    for (int i = 0; i < tracesPerThread; i++) {
                        String traceId = "thread-" + threadId + "-trace-" + i;
                        Trace trace = new Trace(traceId, clock);
                        trace.mark("EVENT");
                        store.store(trace);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // All traces should be stored
        assertEquals(threadCount * tracesPerThread, store.size());
    }

    @Test
    void testThreadSafety_ConcurrentReads() throws InterruptedException {
        // Populate store
        FixedClock clock = new FixedClock(1000);
        for (int i = 0; i < 100; i++) {
            store.store(new Trace("trace-" + i, clock));
        }

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 100; i++) {
                        Optional<Trace> trace = store.get("trace-" + i);
                        if (trace.isPresent()) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Each thread should successfully read all 100 traces
        assertEquals(threadCount * 100, successCount.get());
    }
}
