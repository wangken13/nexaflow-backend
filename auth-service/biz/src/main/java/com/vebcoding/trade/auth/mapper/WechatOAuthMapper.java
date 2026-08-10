package com.vebcoding.trade.auth.mapper;

import java.time.Instant;
import java.util.Optional;

public interface WechatOAuthMapper {
    void saveState(String state, Instant expiresAt);

    boolean consumeState(String state);

    Optional<AuthMapper.UserAccount> findActiveUserByIdentity(String provider, String providerSubject);

    void saveLoginTicket(String ticket, String userId, Instant expiresAt);

    Optional<AuthMapper.UserAccount> findActiveUserByTicket(String ticket, Instant now);

    boolean consumeLoginTicket(String ticket);

    void purgeExpired(Instant before);
}
