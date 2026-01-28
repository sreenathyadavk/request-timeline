package io.reqtracer.cli;

import io.reqtracer.core.FixedClock;
import io.reqtracer.core.Trace;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TimelineFormatterTest {

    @Test
    void testFormatNormal() {
        FixedClock clock = new FixedClock(1000);
        Trace trace = new Trace("req-123", clock);

        trace.mark("REQUEST_RECEIVED");
        clock.advance(12);
        trace.mark("AUTH_CHECK", Map.of("user", "john"));
        clock.advance(120);
        trace.mark("DB_QUERY");
        clock.advance(8);
        trace.end();

        String output = TimelineFormatter.formatNormal(trace);

        // Verify header
        assertTrue(output.contains("TRACE: req-123"));

        // Verify events
        assertTrue(output.contains("REQUEST_RECEIVED     +0ms"));
        assertTrue(output.contains("AUTH_CHECK           +12ms"));
        assertTrue(output.contains("DB_QUERY             +120ms"));

        // Verify metadata
        assertTrue(output.contains("user: john"));

        // Verify footer
        assertTrue(output.contains("TOTAL: 140ms"));
    }

    @Test
    void testFormatCompact() {
        FixedClock clock = new FixedClock(1000);
        Trace trace = new Trace("req-123", clock);

        trace.mark("REQUEST_RECEIVED");
        clock.advance(12);
        trace.mark("AUTH_CHECK");
        clock.advance(120);
        trace.mark("DB_QUERY");
        clock.advance(8);
        trace.end();

        String output = TimelineFormatter.formatCompact(trace);

        // Verify compact format
        assertTrue(output.contains("[req-123]"));
        assertTrue(output.contains("140ms"));
        assertTrue(output.contains("REQUEST_RECEIVED → AUTH_CHECK(+12ms) → DB_QUERY(+120ms)"));
    }

    @Test
    void testFormatNormal_EmptyTrace() {
        FixedClock clock = new FixedClock(1000);
        Trace trace = new Trace("req-empty", clock);
        trace.end();

        String output = TimelineFormatter.formatNormal(trace);

        assertTrue(output.contains("req-empty"));
        assertTrue(output.contains("(no events)"));
        assertTrue(output.contains("TOTAL: 0ms"));
    }

    @Test
    void testFormatCompact_EmptyTrace() {
        FixedClock clock = new FixedClock(1000);
        Trace trace = new Trace("req-empty", clock);
        trace.end();

        String output = TimelineFormatter.formatCompact(trace);

        assertTrue(output.contains("[req-empty]"));
        assertTrue(output.contains("(no events)"));
    }

    @Test
    void testFormatNull() {
        String normalOutput = TimelineFormatter.formatNormal(null);
        assertEquals("No trace found", normalOutput);

        String compactOutput = TimelineFormatter.formatCompact(null);
        assertEquals("No trace found", compactOutput);
    }
}
