# req-tracer

A simple trace tool I built to answer one annoying question while debugging: **"What the hell happened to this request?"**

No dashboards. No fancy UI. Just a clean timeline showing what your code actually did.

## Why I built this

I got tired of scattered logs and complex observability tools when I just wanted to see what happened during a single request. This tool does one thing well: shows you the lifecycle of a request in a way you can actually read.

## What this is NOT

- Not a logging library
- Not an observability platform
- Not distributed tracing (like OpenTelemetry)

## What it actually does

Traces a single request through your code and spits out a timeline. That's it.

## Quick example

```java
import io.reqtracer.Tracer;
import java.util.Map;

// Start tracing
Tracer.start("req-123");

// Mark important points
Tracer.mark("REQUEST_RECEIVED");
Tracer.mark("AUTH_CHECK", Map.of("user", "john"));
Tracer.mark("DB_QUERY", Map.of("table", "users"));
Tracer.mark("RESPONSE_SENT");

// Done
Tracer.end();
```

Then check what happened:

```bash
java -jar req-tracer.jar inspect req-123
```

Output:
```
TRACE: req-123
─────────────────────────
REQUEST_RECEIVED     +0ms
AUTH_CHECK           +12ms
  user: john
DB_QUERY             +120ms
  table: users
RESPONSE_SENT        +8ms
─────────────────────────
TOTAL: 140ms
```

Want it in one line? Use `--compact`:

```bash
java -jar req-tracer.jar inspect req-123 --compact
```

```
[req-123] 140ms | REQUEST_RECEIVED → AUTH_CHECK(+12ms) → DB_QUERY(+120ms) → RESPONSE_SENT(+8ms)
```

## Setup

**Maven:**

```xml
<dependency>
    <groupId>io.reqtracer</groupId>
    <artifactId>req-tracer</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Or just build it:**

```bash
git clone https://github.com/sreenathyadavk/request-timeline.git
cd request-timeline
mvn clean install
```

## How it works

### The basics

Three methods, that's all you need:

```java
Tracer.start("some-id");     // Start a trace
Tracer.mark("EVENT_NAME");   // Record what happened
Tracer.end();                // Save it
```

### With metadata

Sometimes you want more context:

```java
Tracer.mark("DB_QUERY", Map.of(
    "table", "users",
    "operation", "SELECT",
    "rows", "1"
));
```

**Note:** Metadata is limited to 5 keys, and values are truncated if too long. This keeps things sane.

### Error handling

Don't forget to clean up if something breaks:

```java
try {
    Tracer.start("req-123");
    // your code here...
    Tracer.end();
} catch (Exception e) {
    Tracer.clear();  // cleanup
    throw e;
}
```

## Real-world usage

### HTTP request handler

```java
public Response handleRequest(Request req) {
    String traceId = req.getHeader("X-Request-ID");
    
    try {
        Tracer.start(traceId);
        Tracer.mark("REQUEST_RECEIVED");
        
        User user = authenticate(req);
        Tracer.mark("AUTH_CHECK", Map.of("user", user.getId()));
        
        Result result = processRequest(req);
        Tracer.mark("PROCESSING_COMPLETE");
        
        Tracer.mark("RESPONSE_SENT");
        Tracer.end();
        
        return Response.ok(result);
    } catch (Exception e) {
        Tracer.clear();
        throw e;
    }
}
```

### Background job

```java
public void processJob(Job job) {
    Tracer.start("job-" + job.getId());
    Tracer.mark("JOB_START");
    
    Data data = fetchData();
    Tracer.mark("DATA_FETCHED", Map.of("records", String.valueOf(data.size())));
    
    process(data);
    Tracer.mark("PROCESSING_DONE");
    
    save(data);
    Tracer.mark("DATA_SAVED");
    
    Tracer.end();
}
```

## CLI commands

```bash
# Normal output (multi-line with details)
java -jar req-tracer.jar inspect <traceId>

# Compact output (one line, good for screenshots)
java -jar req-tracer.jar inspect <traceId> --compact
```

## How timing works

Important to understand this:

- Each event shows **delta** = time since the *previous* event
- First event always has delta = 0
- Total duration = end time - start time

So if you see:
```
EVENT_1     +0ms
EVENT_2     +10ms
EVENT_3     +5ms
```

It means:
- EVENT_1 happened at the start
- EVENT_2 happened 10ms after EVENT_1
- EVENT_3 happened 5ms after EVENT_2
- Total time: 15ms

## Design decisions

**Framework-agnostic**: Pure Java. No Spring, no Micronaut, no weird annotations.

**Zero dependencies**: Seriously, check the POM. JUnit for tests, that's it.

**Thread-safe**: Uses ThreadLocal so concurrent requests don't mess with each other.

**Testable**: Clock is abstracted so you can test with deterministic timing.

## Building & Testing

```bash
# Run tests
mvn test

# Build JAR
mvn clean package

# Try the example
java -cp target/req-tracer-1.0.0.jar io.reqtracer.examples.ExampleUsage
java -jar target/req-tracer-1.0.0.jar inspect req-123
```

## Project structure

```
src/main/java/io/reqtracer/
├── Tracer.java                  # Main API
├── core/
│   ├── Clock.java               # Time abstraction
│   ├── TraceEvent.java          # Event model
│   └── Trace.java               # Trace model
├── storage/
│   ├── TraceStore.java          # Storage interface
│   └── InMemoryTraceStore.java  # Default in-memory store
└── cli/
    ├── TraceInspector.java      # CLI tool
    └── TimelineFormatter.java   # Output formatting
```

## Testing with mocked time

The Clock interface makes testing super easy:

```java
@Test
void testRequestFlow() {
    FixedClock clock = new FixedClock(1000);
    Tracer.setClock(clock);
    
    Tracer.start("test-req");
    
    clock.advance(10);
    Tracer.mark("EVENT_1");
    
    clock.advance(20);
    Tracer.mark("EVENT_2");
    
    Tracer.end();
    
    Trace trace = Tracer.getStore().get("test-req").get();
    assertEquals(30, trace.getTotalDuration());
}
```

## Philosophy

*"If you can't understand what happened to a request in 10 seconds, something's wrong."*

That's the whole point. Keep it simple. Keep it readable.

## Contributing

PRs welcome! Just make sure:
- Tests pass
- No new runtime dependencies
- Keep the API minimal

## License

MIT - do whatever you want with it.

---

Built this because I needed it. Hope it helps you too.
