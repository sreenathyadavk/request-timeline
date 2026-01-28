package io.reqtracer.core;

/**
 * Production implementation of Clock using System.currentTimeMillis().
 */
public class SystemClock implements Clock {

    private static final SystemClock INSTANCE = new SystemClock();

    private SystemClock() {
        // Singleton
    }

    public static SystemClock getInstance() {
        return INSTANCE;
    }

    @Override
    public long nowMillis() {
        return System.currentTimeMillis();
    }
}
