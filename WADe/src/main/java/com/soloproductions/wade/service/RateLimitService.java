package com.soloproductions.wade.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory, per-client rate limiter for AI prediction endpoints.
 * <p>
 * Configuration keys (application.properties):
 * <ul>
 *   <li>{@code wade.llm.rate-limit.max-guesses}  – maximum allowed calls per window (default 10)</li>
 *   <li>{@code wade.llm.rate-limit.window-seconds} – rolling window length in seconds (default 3600)</li>
 * </ul>
 * The client identifier is typically the remote IP address supplied by the controller.
 */
@Service
public class RateLimitService {

    @Value("${wade.llm.rate-limit.max-guesses:100}")
    private int maxGuesses;

    @Value("${wade.llm.rate-limit.window-seconds:3600}")
    private long windowSeconds;

    private static class Entry {
        int count;
        Instant windowStart;

        Entry(Instant windowStart) {
            this.count = 0;
            this.windowStart = windowStart;
        }
    }

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    /** Result returned to callers of {@link #checkAndDecrement(String)}. */
    public static class RateLimitResult {
        public final boolean allowed;
        public final int remaining;
        public final long resetInSeconds;

        RateLimitResult(boolean allowed, int remaining, long resetInSeconds) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.resetInSeconds = resetInSeconds;
        }
    }

    /**
     * Checks whether the given {@code clientId} is within quota.
     * If allowed, consumes one slot and returns the updated remaining count.
     * If not allowed, returns the current quota state without consuming a slot.
     */
    public synchronized RateLimitResult checkAndDecrement(String clientId) {
        Instant now = Instant.now();
        Entry entry = store.compute(clientId, (k, e) -> {
            if (e == null) return new Entry(now);
            // Reset window if it has expired
            if (now.getEpochSecond() - e.windowStart.getEpochSecond() >= windowSeconds) {
                e.count = 0;
                e.windowStart = now;
            }
            return e;
        });

        long elapsed = now.getEpochSecond() - entry.windowStart.getEpochSecond();
        long resetIn = Math.max(0, windowSeconds - elapsed);

        if (entry.count >= maxGuesses) {
            return new RateLimitResult(false, 0, resetIn);
        }

        entry.count++;
        int remaining = maxGuesses - entry.count;
        return new RateLimitResult(true, remaining, resetIn);
    }

    /**
     * Returns the current rate-limit status WITHOUT consuming a slot.
     * Used by the status endpoint so the UI can display remaining guesses on load.
     */
    public synchronized RateLimitResult getStatus(String clientId) {
        Instant now = Instant.now();
        Entry entry = store.get(clientId);

        if (entry == null) {
            return new RateLimitResult(true, maxGuesses, windowSeconds);
        }

        long elapsed = now.getEpochSecond() - entry.windowStart.getEpochSecond();

        // Window expired — treat as fresh
        if (elapsed >= windowSeconds) {
            return new RateLimitResult(true, maxGuesses, windowSeconds);
        }

        long resetIn = windowSeconds - elapsed;
        int remaining = Math.max(0, maxGuesses - entry.count);
        return new RateLimitResult(remaining > 0, remaining, resetIn);
    }
}
