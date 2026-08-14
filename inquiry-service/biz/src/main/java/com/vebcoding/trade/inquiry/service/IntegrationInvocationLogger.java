package com.vebcoding.trade.inquiry.service;

import com.vebcoding.trade.inquiry.api.IntegrationInvocationView;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntegrationInvocationLogger {
    private final ChannelCredentialStore store;

    public IntegrationInvocationLogger(ChannelCredentialStore store) {
        this.store = store;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String credentialId, String requestId, String method, String path, String clientIp,
                       String outcome, long durationMs) {
        store.findTenantId(credentialId).ifPresent(tenantId -> store.saveInvocation(tenantId,
                new IntegrationInvocationView("inv-" + UUID.randomUUID(), credentialId, requestId, method, path,
                        clientIp, outcome, durationMs, Instant.now().toString())));
    }
}
