package io.github.yuanbug.drawer.utils;

/**
 * @author yuanbug
 */
public class StopwatchTimer {

    private long last = System.currentTimeMillis();

    public static StopwatchTimer start() {
        return new StopwatchTimer();
    }

    public long next() {
        long now = System.currentTimeMillis();
        long cost = now - last;
        last = now;
        return cost;
    }

}
