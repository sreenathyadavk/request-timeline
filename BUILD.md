# Build Verification Guide

## Executable JAR Configuration

The project is already configured to build a single executable JAR using maven-shade-plugin.

### Configuration Summary

**File:** `pom.xml`

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.1</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <transformers>
                    <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                        <mainClass>io.reqtracer.cli.TraceInspector</mainClass>
                    </transformer>
                </transformers>
                <createDependencyReducedPom>false</createDependencyReducedPom>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### What This Does

✅ **Single JAR:** Produces one `request-timeline-1.0.0.jar` file  
✅ **Fat JAR:** Includes all classes (no external dependencies needed)  
✅ **Executable:** Sets `Main-Class: io.reqtracer.cli.TraceInspector` in manifest  
✅ **No Runtime Dependencies:** Pure Java, JUnit 5 is test-only  

### Build Commands

```bash
# Clean and build
mvn clean package

# Expected output:
# target/request-timeline-1.0.0.jar (executable)
# target/original-request-timeline-1.0.0.jar (before shading)
```

### Running the JAR

```bash
# Inspect a trace (normal mode)
java -jar target/request-timeline-1.0.0.jar inspect req-123

# Inspect a trace (compact mode)
java -jar target/request-timeline-1.0.0.jar inspect req-123 --compact

# Run example to create a trace first
java -cp target/request-timeline-1.0.0.jar io.reqtracer.examples.ExampleUsage

# Then inspect it
java -jar target/request-timeline-1.0.0.jar inspect req-123
```

### Verification Checklist

After running `mvn clean package`, verify:

- [ ] JAR exists at `target/request-timeline-1.0.0.jar`
- [ ] JAR is runnable: `java -jar target/request-timeline-1.0.0.jar` shows usage
- [ ] No "no main manifest attribute" error
- [ ] File size is reasonable (includes classes but no heavy dependencies)
- [ ] Can inspect traces created by example

### GitHub Release Ready

This JAR can be attached to GitHub releases as-is:

1. Build: `mvn clean package`
2. Upload: `target/request-timeline-1.0.0.jar`
3. Users can download and run immediately with `java -jar`

No additional assembly or configuration needed!
