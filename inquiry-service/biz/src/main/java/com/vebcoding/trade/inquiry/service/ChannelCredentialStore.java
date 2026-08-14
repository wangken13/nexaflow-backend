package com.vebcoding.trade.inquiry.service;

import java.util.List;
import java.util.Optional;
import com.vebcoding.trade.inquiry.api.IntegrationInvocationView;

interface ChannelCredentialStore {
    ChannelCredential save(ChannelCredential credential);
    Optional<ChannelCredential> findActive(String id);
    List<ChannelCredential> findByTenant(String tenantId);
    boolean revoke(String tenantId, String id);
    void markUsed(String id);
    Optional<InboundReceipt> findReceipt(String credentialId, String externalId);
    void saveReceipt(InboundReceipt receipt);
    Optional<String> findTenantId(String credentialId);
    void saveInvocation(String tenantId, IntegrationInvocationView invocation);
    List<IntegrationInvocationView> findInvocations(String tenantId);
}
