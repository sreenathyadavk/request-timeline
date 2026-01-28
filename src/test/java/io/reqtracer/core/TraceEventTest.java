package io.reqtracer.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TraceEventTest {

    @Test
    void testBasicEvent() {
        TraceEvent event = new TraceEvent("AUTH_CHECK", 1000, 50, 10);

        assertEquals("AUTH_CHECK", event.getName());
        assertEquals(1000, event.getTimestamp());
        assertEquals(50, event.getElapsedSinceStart());
        assertEquals(10, event.getDelta());
        assertFalse(event.hasMetadata());
    }

    @Test
    void testEventWithMetadata() {
        Map<String, String> metadata = Map.of("user", "john", "role", "admin");
        TraceEvent event = new TraceEvent("LOGIN", 1000, 0, 0, metadata);

        assertTrue(event.hasMetadata());
        assertEquals("john", event.getMetadata().get("user"));
        assertEquals("admin", event.getMetadata().get("role"));
    }

    @Test
    void testMetadataLimit_MaxEntries() {
        Map<String, String> metadata = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            metadata.put("key" + i, "value" + i);
        }

        TraceEvent event = new TraceEvent("TEST", 1000, 0, 0, metadata);

        // Should only keep first 5 entries
        assertEquals(5, event.getMetadata().size());
    }

    @Test
    void testMetadataLimit_KeyTruncation() {
        String longKey = "a".repeat(100);
        Map<String, String> metadata = Map.of(longKey, "value");

        TraceEvent event = new TraceEvent("TEST", 1000, 0, 0, metadata);

        // Key should be truncated to 50 chars including "..."
        String truncatedKey = event.getMetadata().keySet().iterator().next();
        assertEquals(50, truncatedKey.length());
        assertTrue(truncatedKey.endsWith("..."));
    }

    @Test
    void testMetadataLimit_ValueTruncation() {
        String longValue = "b".repeat(300);
        Map<String, String> metadata = Map.of("key", longValue);

        TraceEvent event = new TraceEvent("TEST", 1000, 0, 0, metadata);

        // Value should be truncated to 200 chars including "..."
        String truncatedValue = event.getMetadata().get("key");
        assertEquals(200, truncatedValue.length());
        assertTrue(truncatedValue.endsWith("..."));
    }

    @Test
    void testMetadataImmutable() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("key", "value");

        TraceEvent event = new TraceEvent("TEST", 1000, 0, 0, metadata);

        // Modifying original map should not affect event
        metadata.put("key2", "value2");
        assertEquals(1, event.getMetadata().size());

        // Event metadata should be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> {
            event.getMetadata().put("new", "value");
        });
    }
}
