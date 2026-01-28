package io.reqtracer.examples;

import io.reqtracer.Tracer;

import java.util.Map;

/**
 * Example usage demonstrating typical HTTP request lifecycle tracing.
 */
public class ExampleUsage {

    public static void main(String[] args) throws InterruptedException {
        // Simulate HTTP request handling
        simulateRequest("req-123");

        System.out.println("\nTrace stored successfully!");
        System.out.println("To inspect: java -jar req-tracer.jar inspect req-123");
    }

    private static void simulateRequest(String requestId) throws InterruptedException {
        // Start tracing
        Tracer.start(requestId);

        // Request received
        Tracer.mark("REQUEST_RECEIVED");
        Thread.sleep(12);

        // Authentication check
        Tracer.mark("AUTH_CHECK", Map.of("user", "john_doe", "method", "JWT"));
        Thread.sleep(3);

        // Rate limiting
        Tracer.mark("RATE_LIMIT_CHECK", Map.of("limit", "100/min", "current", "45"));
        Thread.sleep(120);

        // Database query
        Tracer.mark("DB_QUERY", Map.of(
                "table", "users",
                "operation", "SELECT",
                "rows", "1"));
        Thread.sleep(8);

        // Response sent
        Tracer.mark("RESPONSE_SENT", Map.of("status", "200", "size", "1024 bytes"));

        // End trace
        Tracer.end();
    }
}
