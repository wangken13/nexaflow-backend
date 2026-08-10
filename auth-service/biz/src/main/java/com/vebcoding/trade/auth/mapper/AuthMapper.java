package com.vebcoding.trade.auth.mapper;

import java.util.Optional;
import java.time.Instant;

public interface AuthMapper {
    Optional<UserAccount> findByUsername(String username);

    default Optional<UserAccount> findById(String userId) {
        return Optional.empty();
    }

    default Optional<UserAccount> findByPhone(String phone) {
        return Optional.empty();
    }

    UserAccount createUser(String tenantName, String username, String password, String displayName, String phone);

    void updatePassword(String username, String password);

    default int countFailedAttemptsSince(String username, Instant since) {
        return 0;
    }

    default void recordLoginAttempt(String username, boolean success) {
    }

    default int countSmsCodesSince(String phone, String purpose, Instant since) {
        return 0;
    }

    default void invalidateActiveSmsCodes(String phone, String purpose) {
    }

    default void saveSmsCode(SmsCode code) {
    }

    default Optional<SmsCode> findLatestActiveSmsCode(String phone, String purpose) {
        return Optional.empty();
    }

    default boolean consumeSmsCode(String id) {
        return false;
    }

    default void increaseSmsCodeAttempts(String id) {
    }

    default void createSession(UserSession session) {
    }

    default Optional<UserSession> findActiveSession(String sessionId, String userId) {
        return Optional.empty();
    }

    default void revokeSession(String sessionId, String userId) {
    }

    record UserAccount(String id, String username, String password, String tenantId, String role, String displayName,
                       String phone) {
    }

    record SmsCode(String id, String phone, String purpose, String codeHash, Instant expiresAt, int attempts) {
    }

    record UserSession(String id, String userId, Instant expiresAt) {
    }
}
