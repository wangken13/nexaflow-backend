package com.vebcoding.trade.inquiry.service;

import jakarta.mail.Address;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.search.FlagTerm;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Properties;
import org.springframework.stereotype.Component;

@Component
class ImapMailboxClient implements MailboxClient {
    private static final int MAX_MESSAGES_PER_POLL = 50;
    private static final int MAX_CONTENT_LENGTH = 20_000;

    @Override
    public int consumeUnread(EmailMailbox mailbox, String password, MailboxMessageConsumer consumer) throws Exception {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", mailbox.host());
        properties.put("mail.imaps.port", String.valueOf(mailbox.port()));
        properties.put("mail.imaps.ssl.enable", "true");
        properties.put("mail.imaps.connectiontimeout", "10000");
        properties.put("mail.imaps.timeout", "20000");
        Store store = Session.getInstance(properties).getStore("imaps");
        Folder folder = null;
        try {
            store.connect(mailbox.host(), mailbox.port(), mailbox.username(), password);
            folder = store.getFolder(mailbox.folder());
            folder.open(Folder.READ_WRITE);
            Message[] unread = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            int consumed = 0;
            for (int index = Math.max(0, unread.length - MAX_MESSAGES_PER_POLL); index < unread.length; index++) {
                Message message = unread[index];
                MailboxMessage mailboxMessage = toMailboxMessage(message);
                consumer.accept(mailboxMessage);
                message.setFlag(Flags.Flag.SEEN, true);
                consumed++;
            }
            return consumed;
        } finally {
            if (folder != null && folder.isOpen()) folder.close(false);
            if (store.isConnected()) store.close();
        }
    }

    private MailboxMessage toMailboxMessage(Message message) throws Exception {
        Address[] from = message.getFrom();
        InternetAddress sender = from != null && from.length > 0 && from[0] instanceof InternetAddress address
                ? address : null;
        String senderEmail = sender == null ? "" : value(sender.getAddress());
        String senderName = sender == null ? "" : value(sender.getPersonal());
        String subject = value(message.getSubject());
        if (!subject.isBlank()) subject = MimeUtility.decodeText(subject);
        String externalId = header(message, "Message-ID");
        if (externalId.isBlank()) {
            externalId = "mail-" + sha256(senderEmail + "|" + subject + "|" + message.getSentDate());
        }
        return new MailboxMessage(limit(externalId, 128), limit(senderName, 128), limit(senderEmail, 255),
                limit(subject.isBlank() ? "邮件询盘" : subject, 255), limit(extractText(message, 0), MAX_CONTENT_LENGTH));
    }

    private String extractText(Part part, int depth) throws Exception {
        if (depth > 8 || Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) return "";
        if (part.isMimeType("text/plain")) return value(part.getContent());
        if (part.isMimeType("text/html")) return stripHtml(value(part.getContent()));
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            StringBuilder fallback = new StringBuilder();
            for (int index = 0; index < multipart.getCount(); index++) {
                Part child = multipart.getBodyPart(index);
                String text = extractText(child, depth + 1);
                if (child.isMimeType("text/plain") && !text.isBlank()) return text;
                if (!text.isBlank() && fallback.isEmpty()) fallback.append(text);
            }
            return fallback.toString();
        }
        return "";
    }

    private String stripHtml(String html) {
        return html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?s)<[^>]+>", " ")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replaceAll("\\s+", " ").trim();
    }

    private String header(Message message, String name) throws Exception {
        String[] values = message.getHeader(name);
        return values == null || values.length == 0 ? "" : value(values[0]);
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String limit(String value, int maxLength) {
        String normalized = value(value);
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
