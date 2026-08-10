package com.vebcoding.trade.auth.service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/** Local fallback strategy used only when Redis cannot be reached. */
@Component
public class InMemoryRateLimitCounterStore implements RateLimitCounterStore {
    private static final int MAX_WINDOWS = 20_000;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public Optional<Boolean> tryAcquire(String key, int maxRequests, Duration duration) {
        long now = System.nanoTime();
        Window window = windows.compute(key, (ignored, current) -> current == null || current.expiresAt < now
                ? new Window(now + duration.toNanos()) : current);
        if (windows.size() > MAX_WINDOWS) {
            windows.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
        }
        return Optional.of(window.count.incrementAndGet() <= maxRequests);
    }

    private static final class Window {
        private final long expiresAt;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}
