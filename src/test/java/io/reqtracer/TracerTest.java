package io.reqtracer;

import io.reqtracer.core.FixedClock;
import io.reqtracer.core.Trace;
import io.reqtracer.storage.InMemoryTraceStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TracerTest {

    private FixedClock clock;
    private InMemoryTraceStore store;

    @BeforeEach
    void setUp() {
        clock = new FixedClock(1000);
        store = new InMemoryTraceStore();

        Tracer.setClock(clock);
        Tracer.setStore(store);
    }

    @AfterEach
    void tearDown() {
        Tracer.clear();
        store.clear();
    }

    @Test
    void testBasicLifecycle() {
        Tracer.start("req-123");

        clock.advance(10);
        Tracer.mark("EVENT_1");

        clock.advance(20);
        Tracer.mark("EVENT_2");

        Tracer.end();

        // Verify trace was stored
        Optional<Trace> trace = store.get("req-123");
        assertTrue(trace.isPresent());
        assertEquals(2, trace.get().getEventCount());
        assertEquals(30, trace.get().getTotalDuration());
    }

    @Test
    void testMarkWithMetadata() {
        Tracer.start("req-123");

        Map<String, String> metadata = Map.of("user", "john");
        Tracer.mark("AUTH", metadata);

        Tracer.end();

        Trace trace = store.get("req-123").get();
        assertEquals("john", trace.getEvents().get(0).getMetadata().get("user"));
    }

    @Test
    void testMarkWithoutStart() {
        assertThrows(IllegalStateException.class, () -> {
            Tracer.mark("EVENT");
        });
    }

    @Test
    void testEndWithoutStart() {
        assertThrows(IllegalStateException.class, () -> {
            Tracer.end();
        });
    }

    @Test
    void testThreadLocalIsolation() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // Each thread creates its own trace
                    Tracer.start("thread-" + threadId);

                    clock.advance(10);
                    Tracer.mark("EVENT_" + threadId);

                    clock.advance(5);
                    Tracer.end();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify all traces were stored separately
        assertEquals(threadCount, store.size());

        for (int i = 0; i < threadCount; i++) {
            Optional<Trace> trace = store.get("thread-" + i);
            assertTrue(trace.isPresent());
            assertEquals(1, trace.get().getEventCount());
            assertEquals("EVENT_" + i, trace.get().getEvents().get(0).getName());
        }
    }

    @Test
    void testGetCurrentTrace() {
        assertNull(Tracer.getCurrentTrace());

        Tracer.start("req-123");
        assertNotNull(Tracer.getCurrentTrace());
        assertEquals("req-123", Tracer.getCurrentTrace().getTraceId());

        Tracer.end();
        assertNull(Tracer.getCurrentTrace());
    }

    @Test
    void testClearActiveTrace() {
        Tracer.start("req-123");
        Tracer.mark("EVENT");

        assertNotNull(Tracer.getCurrentTrace());

        Tracer.clear();

        // Trace should be cleared and not stored
        assertNull(Tracer.getCurrentTrace());
        assertFalse(store.get("req-123").isPresent());
    }

    @Test
    void testDeterministicTiming() {
        Tracer.start("req-123");

        // Event at t=1000
        Tracer.mark("START");

        // Event at t=1012
        clock.advance(12);
        Tracer.mark("AUTH");

        // Event at t=1015
        clock.advance(3);
        Tracer.mark("RATE_LIMIT");

        // Event at t=1135
        clock.advance(120);
        Tracer.mark("DB_QUERY");

        // End at t=1143
        clock.advance(8);
        Tracer.end();

        Trace trace = store.get("req-123").get();

        // Verify deltas
        assertEquals(0, trace.getEvents().get(0).getDelta()); // START: 0ms
        assertEquals(12, trace.getEvents().get(1).getDelta()); // AUTH: +12ms
        assertEquals(3, trace.getEvents().get(2).getDelta()); // RATE_LIMIT: +3ms
        assertEquals(120, trace.getEvents().get(3).getDelta()); // DB_QUERY: +120ms

        // Verify total duration
        assertEquals(143, trace.getTotalDuration());
    }
}
