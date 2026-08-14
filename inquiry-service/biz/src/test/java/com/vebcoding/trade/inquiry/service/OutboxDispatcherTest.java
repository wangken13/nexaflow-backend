package com.vebcoding.trade.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vebcoding.trade.inquiry.api.InquiryCreatedEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class OutboxDispatcherTest {
    @Test
    void marksClaimedEventPublishedOnlyAfterRabbitAcceptsIt() throws Exception {
        TestOutboxStore store = new TestOutboxStore(event());
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        OutboxDispatcher dispatcher = new OutboxDispatcher(store, rabbit, new ObjectMapper());

        dispatcher.dispatch();

        verify(rabbit).convertAndSend(eq("trade.events"), eq("inquiry.created"), any(InquiryCreatedEvent.class));
        assertThat(store.published).containsExactly("evt-1");
        assertThat(store.retried).isEmpty();
    }

    @Test
    void schedulesRetryWhenRabbitPublishFails() throws Exception {
        TestOutboxStore store = new TestOutboxStore(event());
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        doThrow(new IllegalStateException("broker unavailable"))
                .when(rabbit).convertAndSend(eq("trade.events"), eq("inquiry.created"), any(InquiryCreatedEvent.class));
        OutboxDispatcher dispatcher = new OutboxDispatcher(store, rabbit, new ObjectMapper());

        dispatcher.dispatch();

        assertThat(store.published).isEmpty();
        assertThat(store.retried).containsExactly("evt-1");
    }

    private OutboxEvent event() throws Exception {
        String payload = new ObjectMapper().writeValueAsString(
                new InquiryCreatedEvent("evt-1", "inq-1", "tenant-1", "Need quote"));
        return new OutboxEvent("evt-1", payload);
    }

    private static final class TestOutboxStore implements OutboxStore {
        private final List<OutboxEvent> events;
        private final List<String> published = new ArrayList<>();
        private final List<String> retried = new ArrayList<>();

        private TestOutboxStore(OutboxEvent event) { this.events = List.of(event); }
        @Override public List<OutboxEvent> findDue(int limit) { return events; }
        @Override public boolean claim(String eventId) { return true; }
        @Override public void markPublished(String eventId) { published.add(eventId); }
        @Override public void scheduleRetry(String eventId) { retried.add(eventId); }
    }
}
