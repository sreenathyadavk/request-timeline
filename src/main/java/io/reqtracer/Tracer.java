package io.reqtracer;

import io.reqtracer.core.Clock;
import io.reqtracer.core.SystemClock;
import io.reqtracer.core.Trace;
import io.reqtracer.storage.InMemoryTraceStore;
import io.reqtracer.storage.TraceStore;

import java.util.Map;

/**
 * Main public API for request lifecycle tracing.
 * <p>
 * Usage:
 * 
 * <pre>
 * Tracer.start("req-123");
 * Tracer.mark("AUTH_CHECK");
 * Tracer.mark("DB_QUERY", Map.of("table", "users"));
 * Tracer.end();
 * </pre>
 * <p>
 * Uses ThreadLocal to manage trace context per thread.
 * Thread-safe for concurrent requests.
 */
public class Tracer {

    private static Clock clock = SystemClock.getInstance();
    private static TraceStore store = new InMemoryTraceStore();
    private static final ThreadLocal<Trace> currentTrace = new ThreadLocal<>();

    // Private constructor - static API only
    private Tracer() {
    }

    /**
     * Configures the clock implementation.
     * Primarily for testing with FixedClock.
     * 
     * @param newClock clock implementation
     */
    public static void setClock(Clock newClock) {
        clock = newClock;
    }

    /**
     * Configures the trace storage implementation.
     * 
     * @param newStore storage implementation
     */
    public static void setStore(TraceStore newStore) {
        store = newStore;
    }

    /**
     * Returns the configured trace store.
     * Useful for CLI and tests to access traces.
     */
    public static TraceStore getStore() {
        return store;
    }

    /**
     * Starts a new trace for the current thread.
     * 
     * @param traceId unique identifier for this request
     */
    public static void start(String traceId) {
        Trace trace = new Trace(traceId, clock);
        currentTrace.set(trace);
    }

    /**
     * Records an event in the current thread's trace.
     * 
     * @param eventName event name (e.g., "AUTH_CHECK", "DB_QUERY")
     * @throws IllegalStateException if no trace is active
     */
    public static void mark(String eventName) {
        Trace trace = currentTrace.get();
        if (trace == null) {
            throw new IllegalStateException("No active trace. Call Tracer.start() first.");
        }
        trace.mark(eventName);
    }

    /**
     * Records an event with metadata in the current thread's trace.
     * 
     * @param eventName event name
     * @param metadata  optional key-value metadata (subject to limits)
     * @throws IllegalStateException if no trace is active
     */
    public static void mark(String eventName, Map<String, String> metadata) {
        Trace trace = currentTrace.get();
        if (trace == null) {
            throw new IllegalStateException("No active trace. Call Tracer.start() first.");
        }
        trace.mark(eventName, metadata);
    }

    /**
     * Ends the current thread's trace and stores it.
     * 
     * @throws IllegalStateException if no trace is active
     */
    public static void end() {
        Trace trace = currentTrace.get();
        if (trace == null) {
            throw new IllegalStateException("No active trace. Call Tracer.start() first.");
        }
        trace.end();
        store.store(trace);
        currentTrace.remove();
    }

    /**
     * Returns the current thread's active trace, if any.
     * 
     * @return current trace or null if no trace is active
     */
    public static Trace getCurrentTrace() {
        return currentTrace.get();
    }

    /**
     * Clears the current thread's trace without storing it.
     * Useful for error handling and cleanup.
     */
    public static void clear() {
        currentTrace.remove();
    }
}
