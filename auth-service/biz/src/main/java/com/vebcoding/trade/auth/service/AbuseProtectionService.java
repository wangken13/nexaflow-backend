package com.vebcoding.trade.auth.service;

import com.vebcoding.trade.common.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AbuseProtectionService {
    private static final Logger log = LoggerFactory.getLogger(AbuseProtectionService.class);
    private final RateLimitCounterStore redisCounterStore;
    private final RateLimitCounterStore localCounterStore;

    public AbuseProtectionService(RedisRateLimitCounterStore redisCounterStore,
                                  InMemoryRateLimitCounterStore localCounterStore) {
        this.redisCounterStore = redisCounterStore;
        this.localCounterStore = localCounterStore;
    }

    public void check(String scope, String identity, int maxRequests, Duration window) {
        String key = "nexaflow:abuse:" + scope + ":" + digest(identity);
        boolean allowed = redisCounterStore.tryAcquire(key, maxRequests, window)
                .orElseGet(() -> localCounterStore.tryAcquire(key, maxRequests, window).orElse(false));
        if (!allowed) {
            // The value is hashed before it reaches any store or log output.
            log.warn("security.rate_limit.rejected scope={}", scope);
            throw BusinessException.rateLimited("操作过于频繁，请稍后再试");
        }
    }

    private String digest(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

}
