package io.reqtracer.cli;

import io.reqtracer.Tracer;
import io.reqtracer.core.Trace;
import io.reqtracer.storage.TraceStore;

import java.util.Optional;

/**
 * Command-line interface for trace inspection.
 * <p>
 * Usage:
 * 
 * <pre>
 * java -jar req-tracer.jar inspect &lt;traceId&gt;
 * java -jar req-tracer.jar inspect &lt;traceId&gt; --compact
 * </pre>
 */
public class TraceInspector {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];

        if ("inspect".equals(command)) {
            handleInspect(args);
        } else {
            System.err.println("Unknown command: " + command);
            printUsage();
            System.exit(1);
        }
    }

    private static void handleInspect(String[] args) {
        if (args.length < 2) {
            System.err.println("Error: trace ID required");
            System.err.println("Usage: trace inspect <traceId> [--compact]");
            System.exit(1);
        }

        String traceId = args[1];
        boolean compact = args.length > 2 && "--compact".equals(args[2]);

        TraceStore store = Tracer.getStore();
        Optional<Trace> traceOpt = store.get(traceId);

        if (traceOpt.isEmpty()) {
            System.err.println("Trace not found: " + traceId);
            System.exit(1);
        }

        Trace trace = traceOpt.get();
        String output = compact ? TimelineFormatter.formatCompact(trace) : TimelineFormatter.formatNormal(trace);

        System.out.print(output);
    }

    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  trace inspect <traceId>           - Display trace timeline");
        System.err.println("  trace inspect <traceId> --compact - Display compact timeline");
    }
}
