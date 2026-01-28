package io.reqtracer.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a single request trace.
 * <p>
 * A trace captures the complete lifecycle of one request through ordered
 * events.
 * <p>
 * Timing semantics:
 * - Each event's delta represents time since the PREVIOUS event
 * - Total duration = trace end time − trace start time
 * <p>
 * Thread-safe for recording events (uses synchronized list access).
 */
public class Trace {

    private final String traceId;
    private final long startTime;
    private Long endTime;
    private final List<TraceEvent> events;
    private final Clock clock;

    public Trace(String traceId, Clock clock) {
        this.traceId = Objects.requireNonNull(traceId, "Trace ID cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
        this.startTime = clock.nowMillis();
        this.events = Collections.synchronizedList(new ArrayList<>());
        this.endTime = null;
    }

    /**
     * Records a new event in this trace.
     * 
     * @param name event name (e.g., "AUTH_CHECK", "DB_QUERY")
     */
    public void mark(String name) {
        mark(name, Collections.emptyMap());
    }

    /**
     * Records a new event with metadata.
     * 
     * @param name     event name
     * @param metadata optional key-value metadata (subject to limits)
     */
    public void mark(String name, Map<String, String> metadata) {
        long now = clock.nowMillis();
        long elapsedSinceStart = now - startTime;

        // Calculate delta: time since previous event (or 0 for first event)
        long delta = events.isEmpty() ? 0 : (now - events.get(events.size() - 1).getTimestamp());

        TraceEvent event = new TraceEvent(name, now, elapsedSinceStart, delta, metadata);
        events.add(event);
    }

    /**
     * Ends the trace and captures end time.
     */
    public void end() {
        if (endTime == null) {
            endTime = clock.nowMillis();
        }
    }

    /**
     * Returns total duration from start to end.
     * Total duration = trace end time − trace start time.
     * 
     * @return duration in milliseconds, or 0 if not ended
     */
    public long getTotalDuration() {
        if (endTime == null) {
            return 0;
        }
        return endTime - startTime;
    }

    public String getTraceId() {
        return traceId;
    }

    public long getStartTime() {
        return startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public boolean isEnded() {
        return endTime != null;
    }

    /**
     * Returns an unmodifiable view of events in order.
     */
    public List<TraceEvent> getEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public int getEventCount() {
        return events.size();
    }

    @Override
    public String toString() {
        return String.format("Trace{id='%s', events=%d, duration=%dms}",
                traceId, events.size(), getTotalDuration());
    }
}
