package io.reqtracer.storage;

import io.reqtracer.core.Trace;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory trace storage using ConcurrentHashMap.
 * <p>
 * Suitable for development, testing, and single-instance production use.
 * Traces are lost on application restart.
 */
public class InMemoryTraceStore implements TraceStore {

    private final ConcurrentHashMap<String, Trace> traces = new ConcurrentHashMap<>();

    @Override
    public void store(Trace trace) {
        if (trace == null) {
            throw new IllegalArgumentException("Trace cannot be null");
        }
        traces.put(trace.getTraceId(), trace);
    }

    @Override
    public Optional<Trace> get(String traceId) {
        if (traceId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(traces.get(traceId));
    }

    @Override
    public void remove(String traceId) {
        if (traceId != null) {
            traces.remove(traceId);
        }
    }

    @Override
    public Collection<String> listTraceIds() {
        return traces.keySet();
    }

    /**
     * Clears all traces from storage.
     * Useful for testing.
     */
    public void clear() {
        traces.clear();
    }

    /**
     * Returns the number of traces currently stored.
     */
    public int size() {
        return traces.size();
    }
}
