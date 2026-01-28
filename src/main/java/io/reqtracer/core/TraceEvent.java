package io.reqtracer.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable event representing a step in request lifecycle.
 * <p>
 * Event timing semantics:
 * - timestamp: absolute time when event occurred (milliseconds since epoch)
 * - elapsedSinceStart: time since trace started (milliseconds)
 * - delta: time since the PREVIOUS event (milliseconds)
 * <p>
 * Metadata is strictly limited to prevent unbounded growth:
 * - Maximum 5 key-value pairs
 * - Maximum key length: 50 characters
 * - Maximum value length: 200 characters
 * - Excess data is truncated with "..." suffix
 */
public class TraceEvent {

    private static final int MAX_METADATA_ENTRIES = 5;
    private static final int MAX_KEY_LENGTH = 50;
    private static final int MAX_VALUE_LENGTH = 200;
    private static final String TRUNCATION_SUFFIX = "...";

    private final String name;
    private final long timestamp;
    private final long elapsedSinceStart;
    private final long delta;
    private final Map<String, String> metadata;

    public TraceEvent(String name, long timestamp, long elapsedSinceStart, long delta) {
        this(name, timestamp, elapsedSinceStart, delta, Collections.emptyMap());
    }

    public TraceEvent(String name, long timestamp, long elapsedSinceStart, long delta, Map<String, String> metadata) {
        this.name = Objects.requireNonNull(name, "Event name cannot be null");
        this.timestamp = timestamp;
        this.elapsedSinceStart = elapsedSinceStart;
        this.delta = delta;
        this.metadata = sanitizeMetadata(metadata);
    }

    /**
     * Sanitizes metadata to enforce limits.
     * Takes first 5 entries and truncates keys/values that exceed limits.
     */
    private Map<String, String> sanitizeMetadata(Map<String, String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> sanitized = new HashMap<>();
        int count = 0;

        for (Map.Entry<String, String> entry : raw.entrySet()) {
            if (count >= MAX_METADATA_ENTRIES) {
                break;
            }

            String key = truncate(entry.getKey(), MAX_KEY_LENGTH);
            String value = truncate(entry.getValue(), MAX_VALUE_LENGTH);

            sanitized.put(key, value);
            count++;
        }

        return Collections.unmodifiableMap(sanitized);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - TRUNCATION_SUFFIX.length()) + TRUNCATION_SUFFIX;
    }

    public String getName() {
        return name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns time elapsed since trace start in milliseconds.
     */
    public long getElapsedSinceStart() {
        return elapsedSinceStart;
    }

    /**
     * Returns time elapsed since the PREVIOUS event in milliseconds.
     * For the first event, this is 0.
     */
    public long getDelta() {
        return delta;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public boolean hasMetadata() {
        return !metadata.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("TraceEvent{name='%s', delta=%dms, metadata=%s}",
                name, delta, metadata);
    }
}
