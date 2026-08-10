package com.vebcoding.trade.auth.mapper;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

public class InMemoryAuthMapper implements AuthMapper {
    private final Map<String, UserAccount> users;
    private final Map<String, SmsCode> smsCodes = new ConcurrentHashMap<>();
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();

    public InMemoryAuthMapper() {
        this("admin123");
    }

    public InMemoryAuthMapper(String adminPassword) {
        this.users = new ConcurrentHashMap<>(
                Map.of("admin", new UserAccount("demo-admin", "admin", adminPassword, "demo-tenant", "OWNER", "系统管理员", "+8613800000000")));
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    @Override
    public Optional<UserAccount> findById(String userId) {
        return users.values().stream().filter(user -> user.id().equals(userId)).findFirst();
    }

    @Override
    public UserAccount createUser(String tenantName, String username, String password, String displayName, String phone) {
        UserAccount user = new UserAccount("user-" + UUID.randomUUID(), username, password,
                "tenant-" + UUID.randomUUID(), "OWNER", displayName, phone);
        users.put(username, user);
        return user;
    }

    @Override
    public void updatePassword(String username, String password) {
        UserAccount existing = users.get(username);
        if (existing != null) {
            users.put(username, new UserAccount(existing.id(), username, password, existing.tenantId(), existing.role(),
                    existing.displayName(), existing.phone()));
        }
    }

    @Override
    public Optional<UserAccount> findByPhone(String phone) {
        return users.values().stream().filter(user -> phone.equals(user.phone())).findFirst();
    }

    @Override
    public void invalidateActiveSmsCodes(String phone, String purpose) {
        smsCodes.entrySet().removeIf(entry -> phone.equals(entry.getValue().phone()) && purpose.equals(entry.getValue().purpose()));
    }

    @Override
    public void saveSmsCode(SmsCode code) {
        smsCodes.put(code.id(), code);
    }

    @Override
    public Optional<SmsCode> findLatestActiveSmsCode(String phone, String purpose) {
        return smsCodes.values().stream()
                .filter(code -> phone.equals(code.phone()) && purpose.equals(code.purpose()))
                .findFirst();
    }

    @Override
    public boolean consumeSmsCode(String id) {
        return smsCodes.remove(id) != null;
    }

    @Override
    public void increaseSmsCodeAttempts(String id) {
        smsCodes.computeIfPresent(id, (key, code) -> new SmsCode(code.id(), code.phone(), code.purpose(),
                code.codeHash(), code.expiresAt(), code.attempts() + 1));
    }

    @Override public void createSession(UserSession session) { sessions.put(session.id(), session); }
    @Override public Optional<UserSession> findActiveSession(String sessionId, String userId) {
        return Optional.ofNullable(sessions.get(sessionId)).filter(item -> item.userId().equals(userId)
                && item.expiresAt().isAfter(Instant.now()));
    }
    @Override public void revokeSession(String sessionId, String userId) { sessions.remove(sessionId); }
}
