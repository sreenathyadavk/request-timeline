package io.reqtracer.core;

/**
 * Fixed clock implementation for testing.
 * <p>
 * Allows manual control of time progression for deterministic tests.
 */
public class FixedClock implements Clock {

    private long currentTime;

    public FixedClock(long initialTime) {
        this.currentTime = initialTime;
    }

    @Override
    public long nowMillis() {
        return currentTime;
    }

    /**
     * Advances the clock by the specified number of milliseconds.
     * 
     * @param millis milliseconds to advance
     */
    public void advance(long millis) {
        this.currentTime += millis;
    }

    /**
     * Sets the clock to a specific time.
     * 
     * @param time time in milliseconds
     */
    public void setTime(long time) {
        this.currentTime = time;
    }
}
