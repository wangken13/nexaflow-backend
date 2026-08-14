package com.vebcoding.trade.inquiry.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vebcoding.trade.inquiry.api.InboundInquiryRequest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmailMailboxPollingServiceTest {
    private final EmailMailbox mailbox = new EmailMailbox("mbx-1", "tenant-1", "销售邮箱",
            "sales@example.com", "imap.example.com", 993, "sales@example.com", "encrypted", "INBOX",
            true, "PENDING", "", "", Instant.now().toString());

    @Test
    void successfulPollConvertsUnreadEmailToTrustedInquiryAndRecordsHealth() throws Exception {
        EmailMailboxStore store = mock(EmailMailboxStore.class);
        SecretProtector protector = mock(SecretProtector.class);
        MailboxClient client = mock(MailboxClient.class);
        InboundInquiryService intake = mock(InboundInquiryService.class);
        when(protector.reveal("encrypted")).thenReturn("mail-password");
        when(client.consumeUnread(eq(mailbox), eq("mail-password"), any())).thenAnswer(invocation -> {
            MailboxClient.MailboxMessageConsumer consumer = invocation.getArgument(2);
            consumer.accept(new MailboxMessage("<message-1>", "Alice", "alice@acme.com",
                    "Need a quote", "Please quote 500 units"));
            return 1;
        });
        EmailMailboxPollingService service = new EmailMailboxPollingService(store, protector, client, intake);

        service.pollMailbox(mailbox);

        ArgumentCaptor<InboundInquiryRequest> request = ArgumentCaptor.forClass(InboundInquiryRequest.class);
        verify(intake).acceptTrusted(eq("mbx-1"), eq("tenant-1"), eq("EMAIL"), request.capture());
        verify(store).recordSuccess("mbx-1");
        org.assertj.core.api.Assertions.assertThat(request.getValue().email()).isEqualTo("alice@acme.com");
        org.assertj.core.api.Assertions.assertThat(request.getValue().customerName()).isEqualTo("Alice");
    }

    @Test
    void failedPollRecordsActionableStatusWithoutStoppingOtherSchedules() throws Exception {
        EmailMailboxStore store = mock(EmailMailboxStore.class);
        SecretProtector protector = mock(SecretProtector.class);
        MailboxClient client = mock(MailboxClient.class);
        when(protector.reveal("encrypted")).thenReturn("mail-password");
        when(client.consumeUnread(eq(mailbox), eq("mail-password"), any()))
                .thenThrow(new IllegalStateException("IMAP authentication failed"));
        EmailMailboxPollingService service = new EmailMailboxPollingService(store, protector, client,
                mock(InboundInquiryService.class));

        service.pollMailbox(mailbox);

        verify(store).recordFailure("mbx-1", "IMAP authentication failed");
    }
}
