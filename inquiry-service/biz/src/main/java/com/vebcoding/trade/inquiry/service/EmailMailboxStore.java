package com.vebcoding.trade.inquiry.service;

import java.util.List;
import java.util.Optional;

interface EmailMailboxStore {
    EmailMailbox save(EmailMailbox mailbox);
    List<EmailMailbox> findByTenant(String tenantId);
    List<EmailMailbox> findActive();
    Optional<EmailMailbox> findByTenantAndId(String tenantId, String id);
    boolean disable(String tenantId, String id);
    void recordSuccess(String id);
    void recordFailure(String id, String message);
}
