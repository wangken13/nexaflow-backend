package com.vebcoding.trade.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class InMemoryRateLimitCounterStoreTest {
    @Test
    void rejectsRequestsAfterTheConfiguredWindowLimit() {
        InMemoryRateLimitCounterStore store = new InMemoryRateLimitCounterStore();

        assertThat(store.tryAcquire("sms:hashed-client", 2, Duration.ofMinutes(1))).contains(true);
        assertThat(store.tryAcquire("sms:hashed-client", 2, Duration.ofMinutes(1))).contains(true);
        assertThat(store.tryAcquire("sms:hashed-client", 2, Duration.ofMinutes(1))).contains(false);
    }
}
