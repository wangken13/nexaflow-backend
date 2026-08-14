package com.vebcoding.trade.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.inquiry.api.CreateChannelCredentialRequest;
import com.vebcoding.trade.inquiry.mapper.InMemoryInquiryMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChannelIntakeServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-14T05:00:00Z");
    private final TestCredentialStore store = new TestCredentialStore();
    private final SecretProtector protector = new SecretProtector() {
        @Override public String protect(String secret) { return "protected:" + secret; }
        @Override public String reveal(String secret) { return secret.substring("protected:".length()); }
        @Override public boolean available() { return true; }
    };

    @BeforeEach
    void authenticate() {
        TenantContext.setTenantId("tenant-1");
        TenantContext.setUserId("owner-1");
        TenantContext.setRole("OWNER");
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void credentialSecretIsReturnedOnlyWhenCreatedAndCanBeRevoked() {
        ChannelCredentialService service = new ChannelCredentialService(store, protector);

        var created = service.create(new CreateChannelCredentialRequest("官网表单", "website"));

        assertThat(created.signingSecret()).isNotBlank();
        assertThat(created.endpointPath()).endsWith(created.id());
        assertThat(service.list()).singleElement().satisfies(item -> assertThat(item.signingSecret()).isBlank());
        service.revoke(created.id());
        assertThat(store.findActive(created.id())).isEmpty();
    }

    @Test
    void signedRequestCreatesCustomerAndInquiryOnlyOnce() {
        String secret = "channel-secret";
        store.save(new ChannelCredential("key-1", "tenant-1", "官网", "WEBSITE",
                protector.protect(secret), true, "", NOW.toString()));
        AtomicInteger published = new AtomicInteger();
        InquiryService inquiryService = new InquiryService(new InMemoryInquiryMapper(), inquiry -> published.incrementAndGet());
        InboundCustomerResolver resolver = (tenantId, request) -> new InboundCustomerResolver.ResolvedCustomer("cus-1", true);
        InboundInquiryService service = new InboundInquiryService(store, protector,
                new HmacSha256ChannelSignatureVerifier(Clock.fixed(NOW, ZoneOffset.UTC)), resolver, inquiryService,
                new ObjectMapper());
        String body = "{\"externalId\":\"lead-1001\",\"customerName\":\"Acme Inc\","
                + "\"contactName\":\"Alice\",\"email\":\"alice@example.com\","
                + "\"subject\":\"Need 500 units\",\"content\":\"Please quote FOB Shanghai\"}";
        String timestamp = String.valueOf(NOW.getEpochSecond());
        String signature = HmacSha256ChannelSignatureVerifier.sign(secret, timestamp + "." + body);

        var first = service.accept("key-1", timestamp, signature, body);
        var repeated = service.accept("key-1", timestamp, signature, body);

        assertThat(first.duplicate()).isFalse();
        assertThat(repeated.duplicate()).isTrue();
        assertThat(repeated.inquiryId()).isEqualTo(first.inquiryId());
        assertThat(published).hasValue(1);
        assertThat(store.receipts).hasSize(1);
    }

    @Test
    void rejectsExpiredOrTamperedRequests() {
        HmacSha256ChannelSignatureVerifier verifier = new HmacSha256ChannelSignatureVerifier(
                Clock.fixed(NOW, ZoneOffset.UTC));
        String oldTimestamp = String.valueOf(NOW.minusSeconds(301).getEpochSecond());

        assertThatThrownBy(() -> verifier.verify("secret", oldTimestamp, "{}",
                HmacSha256ChannelSignatureVerifier.sign("secret", oldTimestamp + ".{}")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已过期");
        String timestamp = String.valueOf(NOW.getEpochSecond());
        assertThatThrownBy(() -> verifier.verify("secret", timestamp, "{\"changed\":true}",
                HmacSha256ChannelSignatureVerifier.sign("secret", timestamp + ".{}")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("签名校验失败");
    }

    @Test
    void aesGcmProtectorDoesNotPersistPlaintext() {
        AesGcmSecretProtector aes = new AesGcmSecretProtector("a-random-master-key-that-is-longer-than-32-characters");

        String encrypted = aes.protect("sensitive-channel-secret");

        assertThat(encrypted).doesNotContain("sensitive-channel-secret");
        assertThat(aes.reveal(encrypted)).isEqualTo("sensitive-channel-secret");
    }

    private static final class TestCredentialStore implements ChannelCredentialStore {
        private final List<ChannelCredential> credentials = new CopyOnWriteArrayList<>();
        private final List<InboundReceipt> receipts = new CopyOnWriteArrayList<>();

        @Override public ChannelCredential save(ChannelCredential value) { credentials.add(value); return value; }
        @Override public Optional<ChannelCredential> findActive(String id) {
            return credentials.stream().filter(item -> item.id().equals(id) && item.active()).findFirst();
        }
        @Override public List<ChannelCredential> findByTenant(String tenantId) {
            return credentials.stream().filter(item -> item.tenantId().equals(tenantId)).toList();
        }
        @Override public boolean revoke(String tenantId, String id) {
            Optional<ChannelCredential> current = credentials.stream()
                    .filter(item -> item.tenantId().equals(tenantId) && item.id().equals(id) && item.active()).findFirst();
            current.ifPresent(item -> {
                credentials.remove(item);
                credentials.add(new ChannelCredential(item.id(), item.tenantId(), item.displayName(), item.channelType(),
                        item.encryptedSecret(), false, item.lastUsedAt(), item.createdAt()));
            });
            return current.isPresent();
        }
        @Override public void markUsed(String id) { }
        @Override public Optional<InboundReceipt> findReceipt(String credentialId, String externalId) {
            return receipts.stream().filter(item -> item.credentialId().equals(credentialId)
                    && item.externalId().equals(externalId)).findFirst();
        }
        @Override public void saveReceipt(InboundReceipt receipt) { receipts.add(receipt); }
        @Override public Optional<String> findTenantId(String credentialId) {
            return credentials.stream().filter(item -> item.id().equals(credentialId)).map(ChannelCredential::tenantId).findFirst();
        }
        @Override public void saveInvocation(String tenantId, com.vebcoding.trade.inquiry.api.IntegrationInvocationView invocation) { }
        @Override public List<com.vebcoding.trade.inquiry.api.IntegrationInvocationView> findInvocations(String tenantId) { return List.of(); }
    }
}
