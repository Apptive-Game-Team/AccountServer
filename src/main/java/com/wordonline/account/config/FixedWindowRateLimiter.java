package com.wordonline.account.config;

import java.time.Clock;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fixed-window counter per key, held in memory.
 *
 * <p>Memory is bounded two ways. Entries are kept in the order their windows opened, so expired
 * ones are dropped from the front on every call. When the map still holds maxEntries keys, the
 * key whose window opened first is dropped to make room; that key's count starts over, which
 * only matters under an attack from more than maxEntries addresses.
 */
public class FixedWindowRateLimiter {

    /**
     * @param allowed           whether the request may go ahead
     * @param retryAfterSeconds seconds until the window resets; 0 when allowed
     */
    public record Decision(boolean allowed, long retryAfterSeconds) {

    }

    private static final class Window {

        private final long startMillis;
        private int count;

        private Window(long startMillis) {
            this.startMillis = startMillis;
        }
    }

    private final int limit;
    private final long windowMillis;
    private final int maxEntries;
    private final Clock clock;
    // Insertion order equals window start order: a reset removes the key and puts it back.
    private final Map<String, Window> windows = new LinkedHashMap<>();

    public FixedWindowRateLimiter(int limit, Duration window, int maxEntries, Clock clock) {
        this.limit = limit;
        this.windowMillis = window.toMillis();
        this.maxEntries = Math.max(1, maxEntries);
        this.clock = clock;
    }

    public synchronized Decision tryAcquire(String key) {
        long now = clock.millis();
        evictExpired(now);

        Window window = windows.get(key);
        if (window == null) {
            if (windows.size() >= maxEntries) {
                Iterator<String> oldest = windows.keySet().iterator();
                oldest.next();
                oldest.remove();
            }
            window = new Window(now);
            windows.put(key, window);
        }
        if (window.count >= limit) {
            long remainingMillis = window.startMillis + windowMillis - now;
            return new Decision(false, Math.max(1, (remainingMillis + 999) / 1000));
        }
        window.count++;
        return new Decision(true, 0);
    }

    synchronized int size() {
        return windows.size();
    }

    private void evictExpired(long now) {
        Iterator<Window> iterator = windows.values().iterator();
        while (iterator.hasNext() && now - iterator.next().startMillis >= windowMillis) {
            iterator.remove();
        }
    }
}
