package com.vebcoding.trade.auth.service;

import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Redis implementation of the rate-limit counter strategy. */
@Component
public class RedisRateLimitCounterStore implements RateLimitCounterStore {
    private final ObjectProvider<StringRedisTemplate> redisTemplate;

    public RedisRateLimitCounterStore(ObjectProvider<StringRedisTemplate> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<Boolean> tryAcquire(String key, int maxRequests, Duration window) {
        StringRedisTemplate redis = redisTemplate.getIfAvailable();
        if (redis == null) {
            return Optional.empty();
        }
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, window);
            }
            return count == null ? Optional.empty() : Optional.of(count <= maxRequests);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }
}
