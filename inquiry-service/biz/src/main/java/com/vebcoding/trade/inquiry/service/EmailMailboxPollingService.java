package com.vebcoding.trade.inquiry.service;

import com.vebcoding.trade.inquiry.api.InboundInquiryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailMailboxPollingService {
    private static final Logger log = LoggerFactory.getLogger(EmailMailboxPollingService.class);
    private final EmailMailboxStore mailboxStore;
    private final SecretProtector secretProtector;
    private final MailboxClient mailboxClient;
    private final InboundInquiryService inboundInquiryService;

    public EmailMailboxPollingService(EmailMailboxStore mailboxStore, SecretProtector secretProtector,
                                      MailboxClient mailboxClient, InboundInquiryService inboundInquiryService) {
        this.mailboxStore = mailboxStore;
        this.secretProtector = secretProtector;
        this.mailboxClient = mailboxClient;
        this.inboundInquiryService = inboundInquiryService;
    }

    @Scheduled(fixedDelayString = "${app.integration.email-poll-delay-ms:60000}")
    public void poll() {
        if (!secretProtector.available()) return;
        mailboxStore.findActive().forEach(this::pollMailbox);
    }

    void pollMailbox(EmailMailbox mailbox) {
        try {
            int consumed = mailboxClient.consumeUnread(mailbox, secretProtector.reveal(mailbox.encryptedPassword()),
                    message -> inboundInquiryService.acceptTrusted(mailbox.id(), mailbox.tenantId(), "EMAIL",
                            new InboundInquiryRequest(message.externalId(), senderCompany(message),
                                    message.senderName(), message.senderEmail(), "", "", message.subject(),
                                    message.content())));
            mailboxStore.recordSuccess(mailbox.id());
            if (consumed > 0) log.info("mailbox.poll.completed mailboxId={} consumed={}", mailbox.id(), consumed);
        } catch (Exception exception) {
            mailboxStore.recordFailure(mailbox.id(), safeMessage(exception));
            log.warn("mailbox.poll.failed mailboxId={} reason={}", mailbox.id(), exception.getClass().getSimpleName());
        }
    }

    private String senderCompany(MailboxMessage message) {
        if (!message.senderName().isBlank()) return message.senderName();
        String email = message.senderEmail();
        int at = email.indexOf('@');
        return at > 0 && at < email.length() - 1 ? email.substring(at + 1) : "邮件客户";
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "邮箱连接或邮件解析失败" : message;
    }
}
