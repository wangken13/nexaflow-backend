package com.vebcoding.trade.task.api;

public record DailyReport(int openTasks, int newInquiries, int riskyOrders, int pendingApprovals,
                          int overdueTasks, String summary) {
}
