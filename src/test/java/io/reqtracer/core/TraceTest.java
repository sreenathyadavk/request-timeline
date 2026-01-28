package io.reqtracer.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TraceTest {

    @Test
    void testTraceCreation() {
        FixedClock clock = new FixedClock(1000);
        Trace trace = new Trace("req-123", clock);

        assertEquals("req-123", trace.getTraceId());
        assertEquals(1000, trace.getStartTime());
        assertFalse(trace.isEnded());
        assertEquals(0, trace.getEventCount());
    }

    @Test
    void testMarkEvent() {
        FixedClock clock = new FixedClock(1000);
        Trace trace = new Trace("req-123", clock);

        // First event
        trace.mark("EVENT_1");

        assertEquals(1, trace.getEventCount());
        TraceEvent event = trace.getEvents().get(0);
        assertEquals("EVENT_1", event.getName());
        assertEquals(0, event.getDelta()); // First event has 0 delta
        assertEquals(0, event.getElapsedSinceStart());
    }

    @Test
    void testEventTiming_DeltaCalculation() {
        FixedClock clock = new FixedClock(1000);
        Trace trace = new Trace("req-123", clock);

        // Event 1 at t=1000
        trace.mark("EVENT_1");

        // Event 2 at t=1012 (12ms later)
        clock.advance(12);
        trace.mark("EVENT_2");

        // Event 3 at t=1025 (13ms later)
        clock.advance(13);
        trace.mark("EVENT_3");

        List<TraceEvent> events = trace.getEvents();

        // Event 1: delta = 0 (first event)
        assertEquals(0, events.get(0).getDelta());
        assertEquals(0, events.get(0).getElapsedSinceStart());

        // Event 2: delta = 12ms since EVENT_1
        assertEquals(12, events.get(1).getDelta());
        assertEquals(12, events.get(1).getElapsedSinceStart());

        // Event 3: delta = 13ms since EVENT_2
        assertEquals(13, events.get(2).getDelta());
        assertEquals(25, events.get(2).getElapsedSinceStart());
    }

    @Test
    void testTotalDuration() {
        FixedClock clock = new FixedClock(1000);
        Trace trace = new Trace("req-123", clock);

        trace.mark("START");
        clock.advance(50);
        trace.mark("MIDDLE");
        clock.advance(93);

        // End at t=1143
        trace.end();

        // Total duration = end - start = 1143 - 1000 = 143ms
        assertEquals(143, trace.getTotalDuration());
        assertTrue(trace.isEnded());
        assertEquals(1143, trace.getEndTime());
    }

    @Test
    void testMarkWithMetadata() {
        FixedClock clock = new FixedClock(1000);
        Trace trace = new Trace("req-123", clock);

        Map<String, String> metadata = Map.of("user", "john", "action", "login");
        trace.mark("AUTH", metadata);

        TraceEvent event = trace.getEvents().get(0);
        assertEquals("AUTH", event.getName());
        assertTrue(event.hasMetadata());
        assertEquals("john", event.getMetadata().get("user"));
    }

    @Test
    void testEventsListImmutable() {
        FixedClock clock = new FixedClock(1000);
        Trace trace = new Trace("req-123", clock);

        trace.mark("EVENT_1");
        List<TraceEvent> events = trace.getEvents();

        // Should not be able to modify returned list
        assertThrows(UnsupportedOperationException.class, () -> {
            events.add(new TraceEvent("FAKE", 0, 0, 0));
        });
    }

    @Test
    void testEndOnlyOnce() {
        FixedClock clock = new FixedClock(1000);
        Trace trace = new Trace("req-123", clock);

        clock.advance(50);
        trace.end();
        long firstEndTime = trace.getEndTime();

        // Calling end again should not change end time
        clock.advance(100);
        trace.end();

        assertEquals(firstEndTime, trace.getEndTime());
    }
}
