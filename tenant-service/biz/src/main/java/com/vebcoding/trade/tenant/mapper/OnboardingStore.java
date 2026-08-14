package com.vebcoding.trade.tenant.mapper;

public interface OnboardingStore {
    SetupCounts counts(String tenantId);
    boolean hasDemoData(String tenantId);
    void createDemoData(String tenantId, String userId, DemoIds ids);
    int clearDemoData(String tenantId);

    record SetupCounts(int additionalMembers, int customers, int products, int activeMailboxes, int inquiries) {
    }

    record DemoIds(String batchId, String customerId, String productId, String inquiryId, String taskId) {
    }
}
