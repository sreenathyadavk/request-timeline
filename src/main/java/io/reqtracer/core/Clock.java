package io.reqtracer.core;

/**
 * Time abstraction for testability and deterministic behavior.
 * <p>
 * Provides a single method for getting current time in milliseconds.
 * Implementations can use System.currentTimeMillis() for production
 * or fixed/mocked values for testing.
 */
public interface Clock {
    /**
     * Returns the current time in milliseconds.
     * 
     * @return current time in milliseconds since epoch
     */
    long nowMillis();
}
