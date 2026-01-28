package io.reqtracer.cli;

import io.reqtracer.core.Trace;
import io.reqtracer.core.TraceEvent;

import java.util.List;

/**
 * Formats trace timelines for human-readable output.
 * <p>
 * Supports two modes:
 * - Normal: Multi-line format with event details and metadata
 * - Compact: Single-line arrow-separated format
 */
public class TimelineFormatter {

    private static final String SEPARATOR = "─────────────────────────";
    private static final String ARROW = " → ";

    /**
     * Formats trace in normal multi-line mode.
     * <p>
     * Example:
     * 
     * <pre>
     * TRACE: req-123
     * ─────────────────────────
     * REQUEST_RECEIVED     +0ms
     * AUTH_CHECK           +12ms
     *   user: john
     * DB_QUERY             +120ms
     * ─────────────────────────
     * TOTAL: 143ms
     * </pre>
     */
    public static String formatNormal(Trace trace) {
        if (trace == null) {
            return "No trace found";
        }

        StringBuilder sb = new StringBuilder();
        List<TraceEvent> events = trace.getEvents();

        // Header
        sb.append("TRACE: ").append(trace.getTraceId()).append("\n");
        sb.append(SEPARATOR).append("\n");

        // Events
        if (events.isEmpty()) {
            sb.append("(no events)\n");
        } else {
            for (TraceEvent event : events) {
                // Event name and delta
                String line = String.format("%-20s +%dms", event.getName(), event.getDelta());
                sb.append(line).append("\n");

                // Metadata (indented)
                if (event.hasMetadata()) {
                    event.getMetadata().forEach((key, value) -> {
                        sb.append("  ").append(key).append(": ").append(value).append("\n");
                    });
                }
            }
        }

        // Footer
        sb.append(SEPARATOR).append("\n");
        sb.append("TOTAL: ").append(trace.getTotalDuration()).append("ms\n");

        return sb.toString();
    }

    /**
     * Formats trace in compact single-line mode.
     * <p>
     * Example:
     * 
     * <pre>
     * [req-123] 143ms | REQUEST_RECEIVED → AUTH_CHECK(+12ms) → DB_QUERY(+120ms) → RESPONSE_SENT(+8ms)
     * </pre>
     */
    public static String formatCompact(Trace trace) {
        if (trace == null) {
            return "No trace found";
        }

        StringBuilder sb = new StringBuilder();
        List<TraceEvent> events = trace.getEvents();

        // Header: [traceId] duration |
        sb.append("[").append(trace.getTraceId()).append("] ");
        sb.append(trace.getTotalDuration()).append("ms | ");

        // Events: EVENT_NAME → EVENT_NAME(+Xms) → ...
        if (events.isEmpty()) {
            sb.append("(no events)");
        } else {
            for (int i = 0; i < events.size(); i++) {
                TraceEvent event = events.get(i);

                if (i > 0) {
                    sb.append(ARROW);
                }

                sb.append(event.getName());
                if (event.getDelta() > 0) {
                    sb.append("(+").append(event.getDelta()).append("ms)");
                }
            }
        }

        sb.append("\n");
        return sb.toString();
    }
}
