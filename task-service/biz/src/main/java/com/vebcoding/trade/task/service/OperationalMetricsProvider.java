package com.vebcoding.trade.task.service;

public interface OperationalMetricsProvider {
    Metrics load(String tenantId);

    record Metrics(int newInquiries, int riskyOrders, int pendingApprovals, int overdueTasks) {
        public static Metrics empty() {
            return new Metrics(0, 0, 0, 0);
        }
    }
}
