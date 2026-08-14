package com.vebcoding.trade.inquiry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vebcoding.trade.inquiry.api.InquiryCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
    private final OutboxStore outboxStore;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public OutboxDispatcher(OutboxStore outboxStore, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.outboxStore = outboxStore;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.dispatch-delay-ms:5000}")
    public void dispatch() {
        var events = outboxStore.findDue(50);
        for (OutboxEvent event : events) dispatch(event);
    }

    private void dispatch(OutboxEvent event) {
        // Conditional state transition prevents competing instances from publishing the same row concurrently.
        if (!claim(event.id())) {
            return;
        }
        try {
            InquiryCreatedEvent payload = objectMapper.readValue(event.payload(), InquiryCreatedEvent.class);
            rabbitTemplate.convertAndSend("trade.events", "inquiry.created", payload);
            outboxStore.markPublished(event.id());
            log.info("outbox.event.published eventId={}", event.id());
        } catch (Exception exception) {
            outboxStore.scheduleRetry(event.id());
            log.warn("outbox.event.retry_scheduled eventId={}", event.id(), exception);
        }
    }

    private boolean claim(String eventId) {
        return outboxStore.claim(eventId);
    }
}
