package com.vebcoding.trade.inquiry.service;

import java.util.List;

interface OutboxStore {
    List<OutboxEvent> findDue(int limit);
    boolean claim(String eventId);
    void markPublished(String eventId);
    void scheduleRetry(String eventId);
}
