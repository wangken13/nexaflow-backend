package com.vebcoding.trade.auth.mapper;

import java.util.Optional;

public interface AuthMapper {
    Optional<UserAccount> findByUsername(String username);

    UserAccount createUser(String tenantName, String username, String password);

    record UserAccount(String username, String password, String tenantId, String role) {
    }
}