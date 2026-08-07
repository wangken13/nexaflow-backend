package com.vebcoding.trade.auth.mapper;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAuthMapper implements AuthMapper {
    private final Map<String, UserAccount> users = new ConcurrentHashMap<>(
            Map.of("admin", new UserAccount("admin", "admin123", "demo-tenant", "OWNER")));

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    @Override
    public UserAccount createUser(String tenantName, String username, String password) {
        UserAccount user = new UserAccount(username, password, "tenant-" + UUID.randomUUID(), "OWNER");
        users.put(username, user);
        return user;
    }
}
