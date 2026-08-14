package com.vebcoding.trade.inquiry.service;

interface MailboxClient {
    int consumeUnread(EmailMailbox mailbox, String password, MailboxMessageConsumer consumer) throws Exception;

    @FunctionalInterface
    interface MailboxMessageConsumer {
        void accept(MailboxMessage message);
    }
}
