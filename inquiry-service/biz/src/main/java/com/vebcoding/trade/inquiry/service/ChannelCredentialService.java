package com.vebcoding.trade.inquiry.service;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.RoleGuard;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.common.TextSanitizer;
import com.vebcoding.trade.inquiry.api.ChannelCredentialView;
import com.vebcoding.trade.inquiry.api.CreateChannelCredentialRequest;
import com.vebcoding.trade.inquiry.api.IntegrationInvocationView;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ChannelCredentialService {
    private static final Set<String> CHANNEL_TYPES = Set.of("EMAIL", "WEBSITE", "WHATSAPP", "WECHAT_WORK", "CUSTOM");
    private final ChannelCredentialStore credentialStore;
    private final SecretProtector secretProtector;
    private final SecureRandom secureRandom = new SecureRandom();

    public ChannelCredentialService(ChannelCredentialStore credentialStore, SecretProtector secretProtector) {
        this.credentialStore = credentialStore;
        this.secretProtector = secretProtector;
    }

    public List<ChannelCredentialView> list() {
        RoleGuard.requireAny("OWNER", "ADMIN");
        return credentialStore.findByTenant(TenantContext.tenantId()).stream()
                .map(item -> view(item, ""))
                .toList();
    }

    public ChannelCredentialView create(CreateChannelCredentialRequest request) {
        RoleGuard.requireAny("OWNER", "ADMIN");
        if (!secretProtector.available()) {
            throw BusinessException.unavailable("渠道接入尚未配置，请联系运维设置 INTEGRATION_MASTER_KEY");
        }
        String channelType = TextSanitizer.required(request.channelType(), "渠道类型").toUpperCase(Locale.ROOT);
        if (!CHANNEL_TYPES.contains(channelType)) throw new BusinessException("暂不支持该渠道类型");
        String secret = generateSecret();
        ChannelCredential credential = new ChannelCredential("icr-" + UUID.randomUUID(), TenantContext.tenantId(),
                TextSanitizer.required(request.displayName(), "接入名称"), channelType,
                secretProtector.protect(secret), true, "", Instant.now().toString());
        credentialStore.save(credential);
        return view(credential, secret);
    }

    public void revoke(String id) {
        RoleGuard.requireAny("OWNER", "ADMIN");
        if (!credentialStore.revoke(TenantContext.tenantId(), id)) {
            throw BusinessException.notFound("渠道接入凭据不存在或已停用");
        }
    }

    public List<IntegrationInvocationView> invocations() {
        RoleGuard.requireAny("OWNER", "ADMIN");
        return credentialStore.findInvocations(TenantContext.tenantId());
    }

    private ChannelCredentialView view(ChannelCredential credential, String secret) {
        return new ChannelCredentialView(credential.id(), credential.displayName(), credential.channelType(),
                "/api/inquiry/inbound/" + credential.id(), secret, credential.active(), credential.lastUsedAt(),
                credential.createdAt());
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
