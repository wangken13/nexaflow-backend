package com.vebcoding.trade.auth.service;

import java.time.Duration;
import java.util.Optional;

/**
 * Strategy port for a rate-limit counter. An empty result means the backing
 * store is temporarily unavailable and the caller may use a fallback.
 */
public interface RateLimitCounterStore {
    Optional<Boolean> tryAcquire(String key, int maxRequests, Duration window);
}
