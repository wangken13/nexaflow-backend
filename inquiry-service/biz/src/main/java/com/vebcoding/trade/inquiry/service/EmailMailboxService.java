package com.vebcoding.trade.inquiry.service;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.RoleGuard;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.common.TextSanitizer;
import com.vebcoding.trade.inquiry.api.CreateEmailMailboxRequest;
import com.vebcoding.trade.inquiry.api.EmailMailboxView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EmailMailboxService {
    private final EmailMailboxStore mailboxStore;
    private final SecretProtector secretProtector;

    public EmailMailboxService(EmailMailboxStore mailboxStore, SecretProtector secretProtector) {
        this.mailboxStore = mailboxStore;
        this.secretProtector = secretProtector;
    }

    public List<EmailMailboxView> list() {
        RoleGuard.requireAny("OWNER", "ADMIN");
        return mailboxStore.findByTenant(TenantContext.tenantId()).stream().map(this::view).toList();
    }

    public EmailMailboxView create(CreateEmailMailboxRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN");
        if (!secretProtector.available()) {
            throw BusinessException.unavailable("企业邮箱接入尚未配置，请联系运维设置 INTEGRATION_MASTER_KEY");
        }
        String folder = TextSanitizer.optional(request.folder());
        EmailMailbox mailbox = new EmailMailbox("mbx-" + UUID.randomUUID(), TenantContext.tenantId(),
                limited(request.displayName(), "邮箱名称", 128), limited(request.emailAddress(), "邮箱地址", 255),
                limited(request.host(), "IMAP服务器", 255), request.port(), limited(request.username(), "登录账号", 255),
                secretProtector.protect(TextSanitizer.required(request.password(), "邮箱应用密码")),
                folder.isBlank() ? "INBOX" : limited(folder, "邮箱目录", 128), true, "PENDING", "", "",
                Instant.now().toString());
        return view(mailboxStore.save(mailbox));
    }

    public void disable(String id) {
        RoleGuard.requireAny("OWNER", "ADMIN");
        if (!mailboxStore.disable(TenantContext.tenantId(), id)) {
            throw BusinessException.notFound("邮箱接入不存在或已停用");
        }
    }

    private EmailMailboxView view(EmailMailbox mailbox) {
        return new EmailMailboxView(mailbox.id(), mailbox.displayName(), mailbox.emailAddress(), mailbox.host(),
                mailbox.port(), mailbox.username(), mailbox.folder(), mailbox.active(), mailbox.connectionStatus(),
                mailbox.lastSyncAt(), mailbox.lastError(), mailbox.createdAt());
    }

    private String limited(String value, String field, int maxLength) {
        String normalized = TextSanitizer.required(value, field);
        if (normalized.length() > maxLength) throw new BusinessException(field + "不能超过" + maxLength + "个字符");
        return normalized;
    }
}
