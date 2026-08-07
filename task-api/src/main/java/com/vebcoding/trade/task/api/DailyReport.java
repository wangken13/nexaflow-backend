package com.vebcoding.trade.task.api;

public record DailyReport(int openTasks, int newInquiries, int riskyOrders, String summary) {
}