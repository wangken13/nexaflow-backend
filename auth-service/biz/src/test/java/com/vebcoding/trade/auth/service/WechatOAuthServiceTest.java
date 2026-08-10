package com.vebcoding.trade.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.auth.mapper.AuthMapper;
import com.vebcoding.trade.auth.mapper.WechatOAuthMapper;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class WechatOAuthServiceTest {
    @Test
    void authorizationCreatesSingleUseState() {
        TestWechatOAuthMapper mapper = new TestWechatOAuthMapper();
        WechatOAuthService service = configured(mapper);

        String url = service.authorization().authorizationUrl();
        String state = url.substring(url.indexOf("state=") + 6, url.indexOf("#wechat_redirect"));

        assertThat(state).hasSize(48);
        assertThat(mapper.consumeState(state)).isTrue();
        assertThat(mapper.consumeState(state)).isFalse();
    }

    @Test
    void loginTicketCanOnlyBeConsumedOnce() {
        TestWechatOAuthMapper mapper = new TestWechatOAuthMapper();
        AuthMapper.UserAccount user = new AuthMapper.UserAccount("user-1", "owner", "hash", "tenant-1", "OWNER", "Owner", "+8613800000000");
        mapper.saveLoginTicket("wechat-ticket-1", user.id(), Instant.now().plusSeconds(60));
        mapper.users.put(user.id(), user);
        WechatOAuthService service = configured(mapper);

        assertThat(service.consumeLoginTicket("wechat-ticket-1")).contains(user);
        assertThat(service.consumeLoginTicket("wechat-ticket-1")).isEmpty();
    }

    private WechatOAuthService configured(TestWechatOAuthMapper mapper) {
        WechatOAuthService service = new WechatOAuthService(mapper, org.springframework.web.client.RestClient.create());
        ReflectionTestUtils.setField(service, "appId", "wx-test-app");
        ReflectionTestUtils.setField(service, "appSecret", "test-secret");
        ReflectionTestUtils.setField(service, "redirectUri", "https://example.com/api/auth/wechat/callback");
        ReflectionTestUtils.setField(service, "frontendLoginUri", "https://example.com/login");
        return service;
    }

    private static final class TestWechatOAuthMapper implements WechatOAuthMapper {
        private final Map<String, Instant> states = new ConcurrentHashMap<>();
        private final Map<String, String> tickets = new ConcurrentHashMap<>();
        private final Map<String, AuthMapper.UserAccount> users = new ConcurrentHashMap<>();

        @Override public void saveState(String state, Instant expiresAt) { states.put(state, expiresAt); }
        @Override public boolean consumeState(String state) { return states.remove(state) != null; }
        @Override public Optional<AuthMapper.UserAccount> findActiveUserByIdentity(String provider, String subject) { return Optional.empty(); }
        @Override public void saveLoginTicket(String ticket, String userId, Instant expiresAt) { tickets.put(ticket, userId); }
        @Override public Optional<AuthMapper.UserAccount> findActiveUserByTicket(String ticket, Instant now) {
            return Optional.ofNullable(tickets.get(ticket)).flatMap(userId -> Optional.ofNullable(users.get(userId)));
        }
        @Override public boolean consumeLoginTicket(String ticket) { return tickets.remove(ticket) != null; }
        @Override public void purgeExpired(Instant before) { }
    }
}
