package io.reqtracer.storage;

import io.reqtracer.core.Trace;

import java.util.Collection;
import java.util.Optional;

/**
 * Storage abstraction for traces.
 * <p>
 * Implementations must be thread-safe for concurrent access.
 */
public interface TraceStore {

    /**
     * Stores a trace.
     * If a trace with the same ID exists, it is replaced.
     * 
     * @param trace trace to store
     */
    void store(Trace trace);

    /**
     * Retrieves a trace by ID.
     * 
     * @param traceId trace identifier
     * @return optional containing trace if found, empty otherwise
     */
    Optional<Trace> get(String traceId);

    /**
     * Removes a trace from storage.
     * 
     * @param traceId trace identifier
     */
    void remove(String traceId);

    /**
     * Returns all trace IDs currently in storage.
     * 
     * @return collection of trace IDs
     */
    Collection<String> listTraceIds();
}
